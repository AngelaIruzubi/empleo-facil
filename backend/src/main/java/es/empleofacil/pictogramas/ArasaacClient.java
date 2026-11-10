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

    // Prueba las palabras en orden y devuelve el pictograma de la primera que tenga uno
    public Optional<Pictogram> findFirst(List<String> words) {
        if (words == null) {
            return Optional.empty();
        }
        for (String word : words) {
            Optional<Pictogram> pictogram = find(word);
            if (pictogram.isPresent()) {
                return pictogram;
            }
        }
        return Optional.empty();
    }

    // Solo acepta un pictograma cuya palabra sea exactamente la buscada.
    // La búsqueda de ARASAAC es aproximada: con "dependiente", el primer resultado es
    // "llevar en silla de ruedas". Es mejor no poner pictograma que poner uno equivocado.
    // Si no hay pictograma o ARASAAC falla, devuelve un Optional vacío: el texto se puede leer igual.
    public Optional<Pictogram> find(String word) {
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
                    .filter(result -> result.hasKeyword(wanted))
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

    // Solo leemos el número y las palabras del pictograma. ARASAAC envía muchos más datos y los ignoramos.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArasaacPictogram(@JsonProperty("_id") int id, List<Keyword> keywords) {

        boolean hasKeyword(String wanted) {
            return keywords != null && keywords.stream()
                    .anyMatch(keyword -> keyword.keyword() != null && normalize(keyword.keyword()).equals(wanted));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Keyword(String keyword) {
    }
}
