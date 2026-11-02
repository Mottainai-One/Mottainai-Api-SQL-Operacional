package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.CepResponse;
import com.institutojf.mottainai.exception.CepNotFoundException;
import com.institutojf.mottainai.exception.CepLookupUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/** HTTP client for BrasilAPI CEP lookups. */
@Slf4j
@Component
public class BrasilApiClient {

    private final RestClient restClient;

    public BrasilApiClient(@Value("${brasilapi.base-url:https://brasilapi.com.br/api}") String baseUrl,
            @Value("${brasilapi.connect-timeout:3s}") Duration connectTimeout,
            @Value("${brasilapi.read-timeout:3s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Cacheable(cacheNames = "cep", key = "#cep")
    public CepResponse getCep(String cep) {
        try {
            CepApiResult result = restClient.get().uri("/cep/v2/{cep}", cep).retrieve().body(CepApiResult.class);
            if (result == null) {
                log.error("BrasilAPI returned an empty response for a CEP lookup");
                throw new CepNotFoundException(cep);
            }
            return new CepResponse(result.cep(), result.street(), result.neighborhood(), result.city(), result.state(),
                    result.service());
        }
        catch (RestClientResponseException exception) {
            int statusCode = exception.getStatusCode().value();
            if (statusCode == 404) {
                log.error("BrasilAPI did not find the requested CEP (404)");
                throw new CepNotFoundException(cep);
            }
            log.error("BrasilAPI returned an error for a CEP lookup: status {}", statusCode);
            throw new CepLookupUnavailableException("CEP lookup service is temporarily unavailable");
        }
        catch (CepNotFoundException exception) {
            throw exception;
        }
        catch (Exception exception) {
            log.error("Unexpected error during CEP lookup: {}", exception.getClass().getSimpleName());
            throw new CepLookupUnavailableException("CEP lookup service is temporarily unavailable");
        }
    }

    private record CepApiResult(String cep, String street, String neighborhood, String city, String state, String service) {
    }
}
