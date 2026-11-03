package es.empleofacil.lecturafacil;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

// Adapta textos a lectura fácil con el LLM
@Service
public class EasyReadService {

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
        return chatClient.prompt()
                .system(systemPrompt)
                .messages(examples.messages())
                .user(text)
                .call()
                .entity(EasyReadResult.class);
    }
}
