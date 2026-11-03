package es.empleofacil.lecturafacil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

// Ejemplos few-shot: una conversación inventada en la que el modelo ya adaptó bien otros textos.
// Cada ejemplo son dos archivos en prompts/examples/: <nombre>-original.txt y <nombre>-adapted.txt
@Component
public class EasyReadExamples {

    static final List<String> NAMES = List.of(
            "kitchen-job-offer",
            "interview-invitation");

    private final ResourceLoader resourceLoader;
    private final List<Message> messages = new ArrayList<>();

    public EasyReadExamples(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        for (String name : NAMES) {
            messages.add(new UserMessage(original(name)));
            messages.add(new AssistantMessage(adapted(name)));
        }
    }

    public List<Message> messages() {
        return messages;
    }

    String original(String name) {
        return read("classpath:prompts/examples/" + name + "-original.txt");
    }

    String adapted(String name) {
        return read("classpath:prompts/examples/" + name + "-adapted.txt");
    }

    private String read(String location) {
        try {
            return resourceLoader.getResource(location).getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede leer el ejemplo " + location, e);
        }
    }
}
