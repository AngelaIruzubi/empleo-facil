package es.empleofacil.lecturafacil;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import es.empleofacil.llm.AiTimings;
import es.empleofacil.llm.StreamingJsonField;
import es.empleofacil.lecturafacil.analysis.TextSplitter;

// Adapta textos a lectura fácil con el LLM
@Service
public class EasyReadService {

    private static final Logger log = LoggerFactory.getLogger(EasyReadService.class);

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final EasyReadExamples examples;
    // Convierte el JSON de la IA en un EasyReadResult y genera el molde (JSON Schema) para Ollama
    private final BeanOutputConverter<EasyReadResult> converter = new BeanOutputConverter<>(EasyReadResult.class);

    public EasyReadService(ChatClient chatClient,
                           @Value("classpath:prompts/easy-read-system.st") Resource systemPrompt,
                           EasyReadExamples examples) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.examples = examples;
    }

    // Devuelve el texto adaptado y la lista de cambios para que una persona los revise
    public EasyReadResult adapt(String text) {
        EasyReadResult result;
        try {
            ResponseEntity<ChatResponse, EasyReadResult> response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(examples.messages())
                    .user(text)
                    // JSON nativo: Ollama obliga al modelo a escribir un JSON válido con el molde del record.
                    // Sin esto, el molde va como texto en el mensaje y a veces el JSON sale roto.
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(EasyReadResult.class);
            log.info("Tiempos de la IA en la adaptación: {}", AiTimings.describe(response.getResponse()));
            result = response.getEntity();
        } catch (RuntimeException e) {
            // El detalle técnico va al log; la persona usuaria verá un mensaje en lectura fácil
            log.error("Error al llamar a la IA para adaptar un texto", e);
            throw new EasyReadException("La IA no ha podido adaptar el texto", e);
        }
        return checked(result);
    }

    // Igual que adapt(), pero la IA manda la respuesta a trozos según la escribe.
    // Cada trozo nuevo del texto adaptado va a onText, para enseñarlo en pantalla sin esperar al final.
    // onTextEnd avisa una vez cuando el texto está completo. Después la IA aún escribe la lista de cambios.
    public EasyReadResult adaptStreaming(String text, Consumer<String> onText, Runnable onTextEnd) {
        StreamingJsonField adaptedText = new StreamingJsonField("adaptedText");
        StringBuilder json = new StringBuilder();
        AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
        EasyReadResult result;
        try {
            chatClient.prompt()
                    .system(systemPrompt)
                    .messages(examples.messages())
                    .user(text)
                    // El mismo molde de JSON que en adapt(). Aquí hay que pasarlo a mano:
                    // el atajo ENABLE_NATIVE_STRUCTURED_OUTPUT solo funciona con call().
                    .options(OllamaChatOptions.builder().outputSchema(converter.getJsonSchema()))
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        lastResponse.set(response);
                        String chunk = chunkText(response);
                        json.append(chunk);
                        boolean wasFinished = adaptedText.isFinished();
                        String newText = adaptedText.accept(chunk);
                        if (!newText.isEmpty()) {
                            onText.accept(newText);
                        }
                        if (!wasFinished && adaptedText.isFinished()) {
                            onTextEnd.run();
                        }
                    })
                    // Espera aquí hasta el último trozo
                    .blockLast();
            // Ollama manda los tiempos en el último trozo
            log.info("Tiempos de la IA en la adaptación: {}", AiTimings.describe(lastResponse.get()));
            result = converter.convert(json.toString());
        } catch (RuntimeException e) {
            log.error("Error al llamar a la IA para adaptar un texto a trozos", e);
            throw new EasyReadException("La IA no ha podido adaptar el texto", e);
        }
        return checked(result);
    }

    private static String chunkText(ChatResponse response) {
        if (response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return Objects.requireNonNullElse(response.getResult().getOutput().getText(), "");
    }

    private static EasyReadResult checked(EasyReadResult result) {
        // Un texto sin palabras, como "...", tampoco sirve: no se puede leer ni medir
        if (result == null || result.adaptedText() == null || TextSplitter.words(result.adaptedText()).isEmpty()) {
            log.error("La IA ha devuelto una respuesta vacía: {}", result);
            throw new EasyReadException("La IA ha devuelto una respuesta vacía");
        }
        return result;
    }
}
