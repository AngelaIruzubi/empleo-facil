package es.empleofacil.pictogramas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// Test sin internet: MockRestServiceServer hace de ARASAAC y responde lo que le decimos
class ArasaacClientTest {

    private static final String BASE_URL = "https://api.arasaac.org/v1";
    private static final String SEARCH = BASE_URL + "/pictograms/es/search/";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer arasaac = MockRestServiceServer.bindTo(builder).build();
    private final ArasaacClient client = new ArasaacClient(builder.build());

    @Test
    void returnsPictogramWhoseKeywordIsTheWord() {
        // Respuesta real de ARASAAC recortada: trae muchos más datos que no usamos
        arasaac.expect(requestTo(SEARCH + "limpiador"))
                .andRespond(withSuccess("""
                        [
                          {"_id": 4871, "schematic": false, "keywords": [{"keyword": "limpiador", "type": 2}]},
                          {"_id": 11231, "schematic": false, "keywords": [{"keyword": "limpiador", "type": 2}]}
                        ]""", MediaType.APPLICATION_JSON));

        assertThat(client.find("limpiador")).contains(new Pictogram(4871, "limpiador",
                "https://static.arasaac.org/pictograms/4871/4871_300.png"));
        arasaac.verify();
    }

    // Caso real: ARASAAC pone primero "llevar en silla de ruedas" al buscar "dependiente"
    @Test
    void skipsResultsWhoseKeywordIsDifferent() {
        arasaac.expect(requestTo(SEARCH + "dependiente"))
                .andRespond(withSuccess("""
                        [
                          {"_id": 11467, "keywords": [{"keyword": "llevar en silla de ruedas"}]},
                          {"_id": 20123, "keywords": [{"keyword": "vendedor"}]},
                          {"_id": 34878, "keywords": [{"keyword": "Dependiente"}, {"keyword": "tendero"}]}
                        ]""", MediaType.APPLICATION_JSON));

        assertThat(client.find("dependiente")).map(Pictogram::id).contains(34878);
    }

    @Test
    void noExactKeywordReturnsEmpty() {
        arasaac.expect(requestTo(SEARCH + "auxiliar%20de%20limpieza"))
                .andRespond(withSuccess("""
                        [{"_id": 16117, "keywords": [{"keyword": "cuarto de la limpieza"}]}]""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.find("auxiliar de limpieza")).isEmpty();
    }

    @Test
    void encodesWordsWithAccents() {
        arasaac.expect(requestTo(SEARCH + "almac%C3%A9n"))
                .andRespond(withSuccess("[{\"_id\": 6896, \"keywords\": [{\"keyword\": \"almacén\"}]}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.find("almacén")).map(Pictogram::id).contains(6896);
    }

    @Test
    void wordWithoutPictogramReturnsEmpty() {
        arasaac.expect(requestTo(SEARCH + "operario")).andRespond(withResourceNotFound());

        assertThat(client.find("operario")).isEmpty();
    }

    @Test
    void arasaacErrorReturnsEmptyInsteadOfFailing() {
        arasaac.expect(requestTo(SEARCH + "limpiador")).andRespond(withServerError());

        assertThat(client.find("limpiador")).isEmpty();
    }

    @Test
    void findFirstTriesWordsInOrderUntilOneHasPictogram() {
        arasaac.expect(requestTo(SEARCH + "operario")).andRespond(withResourceNotFound());
        arasaac.expect(requestTo(SEARCH + "reponedor"))
                .andRespond(withSuccess("[{\"_id\": 39973, \"keywords\": [{\"keyword\": \"reponedor\"}]}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.findFirst(List.of("operario", "reponedor", "mozo de almacén")))
                .map(Pictogram::keyword).contains("reponedor");
        // No llega a buscar "mozo de almacén": ya tiene pictograma
        arasaac.verify();
    }

    @Test
    void findFirstWithNoWordsReturnsEmptyWithoutCallingArasaac() {
        assertThat(client.findFirst(List.of())).isEmpty();
        assertThat(client.findFirst(null)).isEmpty();
        arasaac.verify();
    }
}
