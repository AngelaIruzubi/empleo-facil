package es.empleofacil.lecturafacil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.empleofacil.ApiExceptionHandler;
import es.empleofacil.pictogramas.ProfessionPictogram;
import es.empleofacil.pictogramas.ProfessionPictogramService;

@RestController
@RequestMapping("/api")
public class EasyReadController {

    private static final Logger log = LoggerFactory.getLogger(EasyReadController.class);

    // La IA tarda 1 o 2 minutos en este ordenador. Damos margen hasta 10.
    private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000;

    private final EasyReadService easyReadService;
    private final ProfessionPictogramService professionPictogramService;
    // Hilos virtuales de Java 21: cada stream espera a la IA en su propio hilo, que es muy barato
    private final ExecutorService streams = Executors.newVirtualThreadPerTaskExecutor();

    public EasyReadController(EasyReadService easyReadService,
                              ProfessionPictogramService professionPictogramService) {
        this.easyReadService = easyReadService;
        this.professionPictogramService = professionPictogramService;
    }

    // Dos llamadas a la IA: primero adapta el texto y, si va bien, busca la profesión.
    // La profesión sale del texto original, que dice el nombre real del puesto.
    @PostMapping("/easy-read")
    public EasyReadReport adapt(@Valid @RequestBody EasyReadRequest request) {
        EasyReadResult result = easyReadService.adapt(request.text());
        ProfessionPictogram profession = professionPictogramService.find(request.text());
        return EasyReadReport.from(request.text(), result, profession);
    }

    // Lo mismo, pero con Server-Sent Events: la respuesta llega en varios mensajes.
    //   event: text    -> {"text": "..."}  un trozo nuevo del texto adaptado (muchos)
    //   event: report  -> el EasyReadReport completo, igual que en /easy-read (uno, al final)
    //   event: error   -> un ProblemDetail si la IA falla (uno, al final)
    // Si el texto no es válido, responde 400 normal, sin stream.
    @PostMapping(path = "/easy-read/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter adaptStreaming(@Valid @RequestBody EasyReadRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        // El trabajo va en otro hilo para que Spring pueda abrir el stream ya
        streams.execute(() -> {
            String text = request.text();
            try {
                EasyReadResult result = easyReadService.adaptStreaming(text,
                        newText -> send(emitter, "text", Map.of("text", newText)));
                ProfessionPictogram profession = professionPictogramService.find(text);
                send(emitter, "report", EasyReadReport.from(text, result, profession));
                emitter.complete();
            } catch (EasyReadException e) {
                sendErrorAndComplete(emitter);
            } catch (RuntimeException e) {
                log.error("Error inesperado en el stream de lectura fácil", e);
                sendErrorAndComplete(emitter);
            }
        });
        return emitter;
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            // La persona ha cerrado la página. Al lanzar la excepción, la IA deja de escribir.
            throw new UncheckedIOException(e);
        }
    }

    private static void sendErrorAndComplete(SseEmitter emitter) {
        try {
            send(emitter, "error", ApiExceptionHandler.aiUnavailable());
            emitter.complete();
        } catch (RuntimeException e) {
            // La conexión ya está cerrada: no hay a quién avisar
            emitter.completeWithError(e);
        }
    }
}
