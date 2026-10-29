package es.empleofacil;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

// Simula que existen las variables de entorno OLLAMA_MODEL y OLLAMA_TEMPERATURE
@SpringBootTest(properties = {
        "OLLAMA_MODEL=modelo-de-prueba",
        "OLLAMA_TEMPERATURE=0.9"
})
class OllamaConfigurationTest {

    @Value("${spring.ai.ollama.chat.model}")
    private String model;

    @Value("${spring.ai.ollama.chat.temperature}")
    private Double temperature;

    @Test
    void environmentVariablesOverrideDefaults() {
        assertThat(model).isEqualTo("modelo-de-prueba");
        assertThat(temperature).isEqualTo(0.9);
    }
}
