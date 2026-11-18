package es.empleofacil.lecturafacil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

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

    // Devuelve las frases corregidas en el mismo orden que request.corrections()
    public List<String> revise(ReviseRequest request) {
        RevisionAnswer answer;
        try {
            ResponseEntity<ChatResponse, RevisionAnswer> response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage(request))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(RevisionAnswer.class);
            log.info("Tiempos de la IA en la corrección: {}", AiTimings.describe(response.getResponse()));
            answer = response.getEntity();
        } catch (RuntimeException e) {
            log.error("Error al llamar a la IA para corregir frases", e);
            throw new RevisionException("La IA no ha podido corregir las frases", e);
        }
        return byNumber(answer, request.corrections().size());
    }

    // Las frases van numeradas para que la IA conteste en el mismo orden
    static String userMessage(ReviseRequest request) {
        List<ReviseRequest.Correction> corrections = request.corrections();
        String numbered = String.join("\n\n", IntStream.range(0, corrections.size())
                .mapToObj(index -> """
                        %d. Frase: %s
                        Qué está mal: %s""".formatted(index + 1,
                        corrections.get(index).sentence().strip(), corrections.get(index).comment().strip()))
                .toList());
        return """
                OFERTA ORIGINAL:
                %s

                TEXTO ADAPTADO COMPLETO:
                %s

                FRASES QUE HAY QUE CORREGIR (%d):
                %s""".formatted(request.originalText().strip(), request.adaptedText().strip(),
                corrections.size(), numbered);
    }

    // Junta las frases de cada número en orden: la frase 1, la 2... Si la IA ha dividido una corrección
    // en varias frases, van juntas, cada una en su línea.
    // Si falta algún número o hay un número que no existe, no sabemos qué va en cada sitio:
    // mejor avisar que estropear el texto.
    static List<String> byNumber(RevisionAnswer answer, int expected) {
        Map<Integer, List<String>> sentences = new TreeMap<>();
        boolean valid = answer != null && answer.sentences() != null;
        if (valid) {
            for (RevisionAnswer.Item item : answer.sentences()) {
                if (item == null || item.number() < 1 || item.number() > expected
                        || item.text() == null || TextSplitter.words(item.text()).isEmpty()) {
                    valid = false;
                    break;
                }
                // En lectura fácil cada frase va en su línea. A veces la IA junta varias frases en una:
                // TextSplitter las separa (respeta "7.5", "Sra." y las fechas).
                sentences.computeIfAbsent(item.number(), number -> new ArrayList<>())
                        .addAll(TextSplitter.sentences(item.text()));
            }
        }
        if (!valid || sentences.size() != expected) {
            log.error("La IA ha devuelto una corrección que no sirve. Esperábamos {} frases: {}", expected, answer);
            throw new RevisionException("La IA ha devuelto una corrección que no sirve");
        }
        return sentences.values().stream().map(lines -> String.join("\n", lines)).toList();
    }
}
