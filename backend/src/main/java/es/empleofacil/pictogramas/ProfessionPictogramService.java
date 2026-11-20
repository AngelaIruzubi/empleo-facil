package es.empleofacil.pictogramas;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import es.empleofacil.llm.AiTimings;

// Busca el pictograma de la profesión de una oferta de trabajo.
// Es una llamada a la IA aparte de la adaptación: con una sola tarea, un modelo pequeño acierta más.
@Service
public class ProfessionPictogramService {

    private static final Logger log = LoggerFactory.getLogger(ProfessionPictogramService.class);

    static final int MAX_PROFESSIONS = 3;

    // Palabras que solas no dicen qué trabajo se hace. En ARASAAC, "auxiliar" es una monitora
    // de educación especial: ese pictograma en una oferta de limpieza confundiría a la persona.
    // El prompt ya las prohíbe, pero un modelo pequeño no siempre hace caso.
    static final Set<String> GENERIC_WORDS = Set.of(
            "auxiliar", "ayudante", "operario", "operaria", "empleado", "empleada",
            "personal", "trabajador", "trabajadora", "mozo", "moza", "peón");

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final ArasaacClient arasaacClient;

    public ProfessionPictogramService(ChatClient chatClient,
                                      @Value("classpath:prompts/profession-system.st") Resource systemPrompt,
                                      ArasaacClient arasaacClient) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.arasaacClient = arasaacClient;
    }

    // Pregunta la profesión a la IA en una llamada aparte y busca su pictograma.
    // Ahora es la red de seguridad: la adaptación ya trae la profesión (ver fromProfessions).
    public ProfessionPictogram find(String text) {
        return pictogramFor(professions(text));
    }

    // Busca el pictograma con las profesiones que ya ha escrito la IA al adaptar el texto. Sin otra llamada a la IA.
    public ProfessionPictogram fromProfessions(List<String> professions) {
        return pictogramFor(clean(professions));
    }

    private ProfessionPictogram pictogramFor(List<String> professions) {
        if (professions.isEmpty()) {
            return ProfessionPictogram.NONE;
        }
        return new ProfessionPictogram(professions, arasaacClient.findFirstProfession(professions).orElse(null));
    }

    // Minúsculas, sin palabras generales, sin repetir y como mucho 3.
    // Además arregla lo que la IA copia de la oferta y no está en ARASAAC (pasó el 20/11/2026):
    // "limpiador/a de oficinas" pasa a "limpiador de oficinas" y después se prueba también "limpiador",
    // la palabra que va antes de "de".
    static List<String> clean(List<String> professions) {
        if (professions == null) {
            return List.of();
        }
        List<String> names = professions.stream()
                .filter(profession -> profession != null && !profession.isBlank())
                .map(profession -> withoutGenderSlash(profession.strip().toLowerCase(Locale.ROOT)))
                .toList();
        // Primero los nombres completos y al final las palabras principales, por si los completos no tienen dibujo
        return Stream.concat(names.stream(), names.stream().map(ProfessionPictogramService::mainWord))
                .filter(profession -> !profession.isBlank() && !GENERIC_WORDS.contains(profession))
                .distinct()
                .limit(MAX_PROFESSIONS)
                .toList();
    }

    // "limpiador/a" o "camarero/as" pasan a "limpiador" y "camarero"
    private static String withoutGenderSlash(String profession) {
        return GENDER_SLASH.matcher(profession).replaceAll("").replaceAll("\\s+", " ").strip();
    }

    // "limpiador de oficinas" pasa a "limpiador". Un nombre sin "de" se queda igual.
    private static String mainWord(String profession) {
        int of = profession.indexOf(" de ");
        return of < 0 ? profession : profession.substring(0, of).strip();
    }

    private static final Pattern GENDER_SLASH = Pattern.compile("/(?:as?|os?)(?![\\p{L}])");

    // Si la IA falla, seguimos sin pictograma: el pictograma ayuda, pero el texto adaptado ya está hecho
    List<String> professions(String text) {
        try {
            ResponseEntity<ChatResponse, Professions> response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(text)
                    // JSON nativo: Ollama obliga al modelo a escribir un JSON válido con el molde del record.
                    // Sin esto, el molde va como texto en el mensaje y a veces el JSON sale roto.
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(Professions.class);
            log.info("Tiempos de la IA en la profesión: {}", AiTimings.describe(response.getResponse()));
            Professions result = response.getEntity();
            return clean(result == null ? null : result.professions());
        } catch (RuntimeException e) {
            log.warn("La IA no ha podido decir la profesión del texto", e);
            return List.of();
        }
    }

    record Professions(
            @JsonPropertyDescription("De 1 a 3 nombres de la profesión, en singular. Primero el más general y corto. "
                    + "Lista vacía si el texto no es una oferta de trabajo.")
            List<String> professions) {
    }
}
