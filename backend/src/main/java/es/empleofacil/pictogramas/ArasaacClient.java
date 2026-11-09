package es.empleofacil.pictogramas;

import java.util.List;
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

    // Devuelve el primer resultado de ARASAAC, que es el que mejor encaja con la palabra.
    // Si no hay pictograma o ARASAAC falla, devuelve un Optional vacío: el texto se puede leer igual.
    public Optional<Pictogram> find(String word) {
        try {
            List<ArasaacPictogram> results = restClient.get()
                    .uri("/pictograms/es/search/{word}", word)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ArasaacPictogram>>() {
                    });
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }
            int id = results.getFirst().id();
            return Optional.of(new Pictogram(id, word, IMAGE_URL.formatted(id, id)));
        } catch (HttpClientErrorException.NotFound e) {
            // ARASAAC responde 404 cuando no tiene ningún pictograma para la palabra
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("No se ha podido consultar ARASAAC para la palabra '{}'", word, e);
            return Optional.empty();
        }
    }

    // Solo leemos el número del pictograma. ARASAAC envía muchos más datos y los ignoramos.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArasaacPictogram(@JsonProperty("_id") int id) {
    }
}
