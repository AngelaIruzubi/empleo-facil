package es.empleofacil.pictogramas;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ArasaacConfig {

    // Si ARASAAC tarda más de 5 segundos, seguimos sin pictograma
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient arasaacRestClient(RestClient.Builder builder,
                                        @Value("${arasaac.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
