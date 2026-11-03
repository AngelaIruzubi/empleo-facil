package es.empleofacil.lecturafacil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

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
            result = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(examples.messages())
                    .user(text)
                    .call()
                    .entity(EasyReadResult.class);
        } catch (RuntimeException e) {
            // El detalle técnico va al log; la persona usuaria verá un mensaje en lectura fácil
            log.error("Error al llamar a la IA para adaptar un texto", e);
            throw new EasyReadException("La IA no ha podido adaptar el texto", e);
        }
        if (result == null || result.adaptedText() == null || result.adaptedText().isBlank()) {
            log.error("La IA ha devuelto una respuesta vacía: {}", result);
            throw new EasyReadException("La IA ha devuelto una respuesta vacía");
        }
        return result;
    }
}
