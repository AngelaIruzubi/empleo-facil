package es.empleofacil.lecturafacil;

import java.util.List;
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
        RevisedSentences result;
        try {
            ResponseEntity<ChatResponse, RevisedSentences> response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage(request))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(RevisedSentences.class);
            log.info("Tiempos de la IA en la corrección: {}", AiTimings.describe(response.getResponse()));
            result = response.getEntity();
        } catch (RuntimeException e) {
            log.error("Error al llamar a la IA para corregir frases", e);
            throw new EasyReadException("La IA no ha podido corregir las frases", e);
        }
        return checked(result, request.corrections().size());
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

    // La IA tiene que devolver una frase con palabras por cada frase marcada.
    // Si no, no sabemos qué frase va en qué sitio: mejor avisar que estropear el texto.
    private static List<String> checked(RevisedSentences result, int expected) {
        if (result == null || result.sentences() == null || result.sentences().size() != expected
                || result.sentences().stream().anyMatch(sentence -> sentence == null || TextSplitter.words(sentence).isEmpty())) {
            log.error("La IA ha devuelto una corrección que no sirve. Esperábamos {} frases: {}", expected, result);
            throw new EasyReadException("La IA ha devuelto una corrección que no sirve");
        }
        return result.sentences().stream().map(String::strip).toList();
    }
}
