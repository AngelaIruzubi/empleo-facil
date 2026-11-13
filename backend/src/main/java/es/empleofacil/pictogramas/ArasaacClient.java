package es.empleofacil.pictogramas;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Busca pictogramas en la API de ARASAAC (https://arasaac.org).
// Los pictogramas son de Sergio Palao para el Gobierno de Aragón, con licencia CC BY-NC-SA.
@Component
public class ArasaacClient {

    private static final Logger log = LoggerFactory.getLogger(ArasaacClient.class);

    private static final String IMAGE_URL = "https://static.arasaac.org/pictograms/%d/%d_300.png";

    private final RestClient restClient;

    public ArasaacClient(RestClient arasaacRestClient) {
        this.restClient = arasaacRestClient;
    }

    // Categoría de ARASAAC para los pictogramas de profesiones
    static final String PROFESSIONAL = "professional";

    // Prueba las profesiones en orden y devuelve el pictograma de la primera que tenga uno
    public Optional<Pictogram> findFirstProfession(List<String> professions) {
        if (professions == null) {
            return Optional.empty();
        }
        for (String profession : professions) {
            Optional<Pictogram> pictogram = findProfession(profession);
            if (pictogram.isPresent()) {
                return pictogram;
            }
        }
        return Optional.empty();
    }

    // Solo acepta un pictograma si cumple dos cosas, porque la búsqueda de ARASAAC es aproximada:
    // - Su palabra es exactamente la buscada. Con "dependiente", el primer resultado es "llevar en silla de ruedas".
    // - Es de la categoría de profesiones. "Limpiador" también es una botella de producto de limpieza.
    // Es mejor no poner pictograma que poner uno equivocado.
    // Si no hay pictograma o ARASAAC falla, devuelve un Optional vacío: el texto se puede leer igual.
    public Optional<Pictogram> findProfession(String word) {
        if (word == null || word.isBlank()) {
            return Optional.empty();
        }
        String wanted = normalize(word);
        try {
            List<ArasaacPictogram> results = restClient.get()
                    .uri("/pictograms/es/search/{word}", word.strip())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ArasaacPictogram>>() {
                    });
            if (results == null) {
                return Optional.empty();
            }
            return results.stream()
                    .filter(result -> result.hasKeyword(wanted) && result.isProfession())
                    .findFirst()
                    .map(result -> new Pictogram(result.id(), word.strip(), IMAGE_URL.formatted(result.id(), result.id())));
        } catch (HttpClientErrorException.NotFound e) {
            // ARASAAC responde 404 cuando no tiene ningún pictograma para la palabra
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("No se ha podido consultar ARASAAC para la palabra '{}'", word, e);
            return Optional.empty();
        }
    }

    private static String normalize(String word) {
        return word.strip().toLowerCase(Locale.ROOT);
    }

    // Solo leemos el número, las palabras y las categorías del pictograma.
    // ARASAAC envía muchos más datos y los ignoramos.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArasaacPictogram(@JsonProperty("_id") int id, List<Keyword> keywords, List<String> categories) {

        boolean hasKeyword(String wanted) {
            return keywords != null && keywords.stream()
                    .anyMatch(keyword -> keyword.keyword() != null && normalize(keyword.keyword()).equals(wanted));
        }

        boolean isProfession() {
            return categories != null && categories.contains(PROFESSIONAL);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Keyword(String keyword) {
    }
}
