package es.empleofacil.pictogramas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import es.empleofacil.pictogramas.ProfessionPictogramService.Professions;

// Test unitario: la IA y ARASAAC son falsos (mocks)
class ProfessionPictogramServiceTest {

    private static final String OFFER = "Se precisa auxiliar de limpieza para oficinas.";

    private final Resource systemPrompt = new ClassPathResource("prompts/profession-system.st");
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final ArasaacClient arasaacClient = mock(ArasaacClient.class);
    private final ProfessionPictogramService service =
            new ProfessionPictogramService(chatClient, systemPrompt, arasaacClient);

    private final Pictogram cleaner =
            new Pictogram(4871, "limpiador", "https://static.arasaac.org/pictograms/4871/4871_300.png");

    @Test
    void returnsProfessionsAndPictogramOfTheFirstOneFound() {
        aiAnswers(new Professions(List.of("limpiador", "auxiliar de limpieza")));
        when(arasaacClient.findFirstProfession(List.of("limpiador", "auxiliar de limpieza"))).thenReturn(Optional.of(cleaner));

        ProfessionPictogram result = service.find(OFFER);

        assertThat(result.professions()).containsExactly("limpiador", "auxiliar de limpieza");
        assertThat(result.pictogram()).isEqualTo(cleaner);
    }

    @Test
    void professionWithoutPictogramKeepsTheProfessions() {
        aiAnswers(new Professions(List.of("carretillero")));
        when(arasaacClient.findFirstProfession(List.of("carretillero"))).thenReturn(Optional.empty());

        ProfessionPictogram result = service.find(OFFER);

        assertThat(result.professions()).containsExactly("carretillero");
        assertThat(result.pictogram()).isNull();
    }

    @Test
    void notAJobOfferDoesNotCallArasaac() {
        aiAnswers(new Professions(List.of()));

        assertThat(service.find("Gracias por venir a la entrevista.")).isEqualTo(ProfessionPictogram.NONE);
        verifyNoInteractions(arasaacClient);
    }

    @Test
    void cleansTheAiAnswer() {
        // Quita vacíos, espacios, mayúsculas y repetidos, y se queda con 3 como máximo
        aiAnswers(new Professions(Arrays.asList(" Limpiador ", "", null, "limpiador", "limpiadora", "conserje", "portero")));

        assertThat(service.professions(OFFER)).containsExactly("limpiador", "limpiadora", "conserje");
    }

    // Caso real: con "auxiliar", ARASAAC devuelve el pictograma de una monitora de educación especial
    @Test
    void discardsGenericWordsButKeepsCompoundNames() {
        aiAnswers(new Professions(List.of("Auxiliar", "personal", "mozo de almacén")));

        assertThat(service.professions(OFFER)).containsExactly("mozo de almacén");
    }

    @Test
    void onlyGenericWordsMeansNoPictogram() {
        aiAnswers(new Professions(List.of("trabajador", "empleado", "personal")));

        assertThat(service.find(OFFER)).isEqualTo(ProfessionPictogram.NONE);
        verifyNoInteractions(arasaacClient);
    }

    @Test
    void aiErrorMeansNoPictogramInsteadOfFailing() {
        when(chatClient.prompt().system(systemPrompt).user(OFFER).advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT).call())
                .thenThrow(new RuntimeException("Connection refused"));

        assertThat(service.find(OFFER)).isEqualTo(ProfessionPictogram.NONE);
    }

    @Test
    void systemPromptFileExists() {
        assertThat(systemPrompt.exists()).isTrue();
    }

    private void aiAnswers(Professions professions) {
        when(chatClient.prompt().system(systemPrompt).user(OFFER).advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT).call().entity(Professions.class))
                .thenReturn(professions);
        when(chatClient.prompt().system(systemPrompt).user("Gracias por venir a la entrevista.").advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT).call()
                .entity(Professions.class)).thenReturn(professions);
    }
}
