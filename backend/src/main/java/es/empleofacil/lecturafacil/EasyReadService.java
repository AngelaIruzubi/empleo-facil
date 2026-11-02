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

    public EasyReadService(ChatClient chatClient,
                           @Value("classpath:prompts/easy-read-system.st") Resource systemPrompt) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
    }

    public String adapt(String text) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(text)
                .call()
                .content();
    }
}
