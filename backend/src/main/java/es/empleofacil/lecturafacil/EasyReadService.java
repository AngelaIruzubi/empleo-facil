package es.empleofacil.lecturafacil;

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

// Adapta textos a lectura fácil con el LLM
@Service
public class EasyReadService {

    private static final Logger log = LoggerFactory.getLogger(EasyReadService.class);

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final EasyReadExamples examples;

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
        // Un texto sin palabras, como "...", tampoco sirve: no se puede leer ni medir
        if (result == null || result.adaptedText() == null || TextSplitter.words(result.adaptedText()).isEmpty()) {
            log.error("La IA ha devuelto una respuesta vacía: {}", result);
            throw new EasyReadException("La IA ha devuelto una respuesta vacía");
        }
        return result;
    }
}
