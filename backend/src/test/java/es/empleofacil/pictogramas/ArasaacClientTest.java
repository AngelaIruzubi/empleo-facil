package es.empleofacil.pictogramas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// Test sin internet: MockRestServiceServer hace de ARASAAC y responde lo que le decimos
class ArasaacClientTest {

    private static final String BASE_URL = "https://api.arasaac.org/v1";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer arasaac = MockRestServiceServer.bindTo(builder).build();
    private final ArasaacClient client = new ArasaacClient(builder.build());

    @Test
    void returnsFirstPictogramFound() {
        // Respuesta real de ARASAAC recortada: trae muchos más datos que no usamos
        arasaac.expect(requestTo(BASE_URL + "/pictograms/es/search/limpiar"))
                .andRespond(withSuccess("""
                        [
                          {"_id": 3351, "schematic": false, "keywords": [{"keyword": "limpiar", "type": 3}]},
                          {"_id": 16803, "schematic": false, "keywords": [{"keyword": "limpiar", "type": 3}]}
                        ]""", MediaType.APPLICATION_JSON));

        assertThat(client.find("limpiar")).contains(new Pictogram(3351, "limpiar",
                "https://static.arasaac.org/pictograms/3351/3351_300.png"));
        arasaac.verify();
    }

    @Test
    void encodesWordsWithAccents() {
        arasaac.expect(requestTo(BASE_URL + "/pictograms/es/search/almac%C3%A9n"))
                .andRespond(withSuccess("[{\"_id\": 6896}]", MediaType.APPLICATION_JSON));

        assertThat(client.find("almacén")).map(Pictogram::id).contains(6896);
    }

    @Test
    void wordWithoutPictogramReturnsEmpty() {
        arasaac.expect(requestTo(BASE_URL + "/pictograms/es/search/reponer"))
                .andRespond(withResourceNotFound());

        assertThat(client.find("reponer")).isEmpty();
    }

    @Test
    void arasaacErrorReturnsEmptyInsteadOfFailing() {
        arasaac.expect(requestTo(BASE_URL + "/pictograms/es/search/limpiar"))
                .andRespond(withServerError());

        assertThat(client.find("limpiar")).isEmpty();
    }
}
