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

    // Caso real: los dos primeros "limpiador" de ARASAAC son botellas de producto de limpieza
    @Test
    void returnsTheProfessionAndSkipsObjectsWithTheSameWord() {
        // Respuesta real de ARASAAC recortada: trae muchos más datos que no usamos
        arasaac.expect(requestTo(SEARCH + "limpiador"))
                .andRespond(withSuccess("""
                        [
                          {"_id": 4871, "keywords": [{"keyword": "limpiador"}], "categories": ["cleaning product"]},
                          {"_id": 11231, "keywords": [{"keyword": "limpiador"}], "categories": ["cleaning product"]},
                          {"_id": 15744, "keywords": [{"keyword": "limpiador"}, {"keyword": "ordenanza"}],
                           "categories": ["professional", "waste disposal"]}
                        ]""", MediaType.APPLICATION_JSON));

        assertThat(client.findProfession("limpiador")).contains(new Pictogram(15744, "limpiador",
                "https://static.arasaac.org/pictograms/15744/15744_300.png"));
        arasaac.verify();
    }

    // Caso real: ARASAAC pone primero "llevar en silla de ruedas" al buscar "dependiente"
    @Test
    void skipsResultsWhoseKeywordIsDifferent() {
        arasaac.expect(requestTo(SEARCH + "dependiente"))
                .andRespond(withSuccess("""
                        [
                          {"_id": 11467, "keywords": [{"keyword": "llevar en silla de ruedas"}], "categories": ["movement"]},
                          {"_id": 20123, "keywords": [{"keyword": "vendedor"}], "categories": ["professional"]},
                          {"_id": 34878, "keywords": [{"keyword": "Dependiente"}], "categories": ["professional"]}
                        ]""", MediaType.APPLICATION_JSON));

        assertThat(client.findProfession("dependiente")).map(Pictogram::id).contains(34878);
    }

    @Test
    void noExactKeywordReturnsEmpty() {
        arasaac.expect(requestTo(SEARCH + "auxiliar%20de%20limpieza"))
                .andRespond(withSuccess("""
                        [{"_id": 16117, "keywords": [{"keyword": "cuarto de la limpieza"}], "categories": ["room"]}]""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.findProfession("auxiliar de limpieza")).isEmpty();
    }

    @Test
    void encodesWordsWithAccents() {
        arasaac.expect(requestTo(SEARCH + "peluquer%C3%ADa"))
                .andRespond(withSuccess("""
                        [{"_id": 1, "keywords": [{"keyword": "peluquería"}], "categories": ["professional"]}]""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.findProfession("peluquería")).map(Pictogram::id).contains(1);
    }

    @Test
    void wordWithoutPictogramReturnsEmpty() {
        arasaac.expect(requestTo(SEARCH + "carretillero")).andRespond(withResourceNotFound());

        assertThat(client.findProfession("carretillero")).isEmpty();
    }

    @Test
    void arasaacErrorReturnsEmptyInsteadOfFailing() {
        arasaac.expect(requestTo(SEARCH + "limpiador")).andRespond(withServerError());

        assertThat(client.findProfession("limpiador")).isEmpty();
    }

    @Test
    void findFirstProfessionTriesWordsInOrderUntilOneHasPictogram() {
        arasaac.expect(requestTo(SEARCH + "carretillero")).andRespond(withResourceNotFound());
        arasaac.expect(requestTo(SEARCH + "reponedor"))
                .andRespond(withSuccess("""
                        [{"_id": 39973, "keywords": [{"keyword": "reponedor"}], "categories": ["professional", "trade"]}]""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.findFirstProfession(List.of("carretillero", "reponedor", "mozo de almacén")))
                .map(Pictogram::keyword).contains("reponedor");
        // No llega a buscar "mozo de almacén": ya tiene pictograma
        arasaac.verify();
    }

    @Test
    void findFirstProfessionWithNoWordsReturnsEmptyWithoutCallingArasaac() {
        assertThat(client.findFirstProfession(List.of())).isEmpty();
        assertThat(client.findFirstProfession(null)).isEmpty();
        arasaac.verify();
    }
}
