package es.empleofacil.lecturafacil;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import es.empleofacil.llm.AiTimings;
import es.empleofacil.lecturafacil.analysis.TextSplitter;

// Corrige las frases que ha marcado el preparador laboral, siguiendo sus comentarios.
// Una llamada a la IA por cada frase: con todas a la vez, numeradas, el modelo pequeño se liaba
// con los números y la corrección fallaba.
// No es mucho más lento: todas las llamadas empiezan igual (instrucciones, oferta y texto adaptado)
// y Ollama no vuelve a leer el principio si es igual que en la llamada anterior.
// No usa los ejemplos largos de la adaptación: así la IA lee menos y tarda menos.
@Service
public class RevisionService {

    private static final Logger log = LoggerFactory.getLogger(RevisionService.class);

    private final ChatClient chatClient;
    private final Resource systemPrompt;

    public RevisionService(ChatClient chatClient,
                           @Value("classpath:prompts/revise-system.st") Resource systemPrompt) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
    }

    // Devuelve las frases corregidas en el mismo orden que request.corrections().
    // Una corrección puede tener varias líneas si la IA ha dividido la frase.
    public List<String> revise(ReviseRequest request) {
        return request.corrections().stream()
                .map(correction -> reviseOne(request, correction))
                .toList();
    }

    private String reviseOne(ReviseRequest request, ReviseRequest.Correction correction) {
        RevisionAnswer answer;
        try {
            ResponseEntity<ChatResponse, RevisionAnswer> response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage(request, correction))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(RevisionAnswer.class);
            log.info("Tiempos de la IA en la corrección: {}", AiTimings.describe(response.getResponse()));
            answer = response.getEntity();
        } catch (RuntimeException e) {
            log.error("Error al llamar a la IA para corregir una frase", e);
            throw new RevisionException("La IA no ha podido corregir la frase", e);
        }
        String corrected = checked(answer);
        // Para poder revisar después qué ha contestado la IA. Los textos son ofertas de trabajo, sin datos personales.
        log.info("Corrección: «{}» con el comentario «{}» -> «{}»",
                correction.sentence(), correction.comment(), corrected.replace("\n", " / "));
        return corrected;
    }

    // Lo que cambia de una llamada a otra (la frase y el comentario) va al final:
    // así el principio es igual y Ollama aprovecha lo que ya ha leído.
    static String userMessage(ReviseRequest request, ReviseRequest.Correction correction) {
        return """
                OFERTA ORIGINAL:
                %s

                TEXTO ADAPTADO COMPLETO:
                %s

                FRASE QUE HAY QUE CORREGIR:
                %s

                QUÉ ESTÁ MAL, SEGÚN EL PREPARADOR LABORAL:
                %s""".formatted(request.originalText().strip(), request.adaptedText().strip(),
                correction.sentence().strip(), correction.comment().strip());
    }

    // En lectura fácil cada frase va en su línea. A veces la IA junta varias frases en una:
    // TextSplitter las separa (respeta "7.5", "Sra." y las fechas).
    static String checked(RevisionAnswer answer) {
        if (answer == null || answer.correctedText() == null || TextSplitter.words(answer.correctedText()).isEmpty()) {
            log.error("La IA ha devuelto una corrección vacía: {}", answer);
            throw new RevisionException("La IA ha devuelto una corrección vacía");
        }
        return String.join("\n", TextSplitter.sentences(answer.correctedText()));
    }
}
