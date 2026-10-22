
package com.verify_x.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class ZeroBounceEmailVerifier {

    private static final String API_URL =
            "https://api.zerobounce.net/v2/validate";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zerobounce.api-key:}")
    private String apiKey;

    public ZeroBounceEmailVerifier(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public boolean isValidMailbox(String email) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailVerificationServiceException(
                    "Email verification service is not configured."
            );
        }

        URI uri = UriComponentsBuilder
                .fromUriString(API_URL)
                .queryParam("api_key", apiKey)
                .queryParam("email", email)
                .queryParam("timeout", 10)
                .build()
                .encode()
                .toUri();

        final String response;

        try {
            response = restTemplate.getForObject(uri, String.class);
        } catch (RestClientException ex) {
            throw new EmailVerificationServiceException(
                    "Email verification service is temporarily unavailable.",
                    ex
            );
        }

        if (response == null || response.isBlank()) {
            throw new EmailVerificationServiceException(
                    "Email verification service returned an empty response."
            );
        }

        final JsonNode json;

        try {
            json = objectMapper.readTree(response);
        } catch (JsonProcessingException ex) {
            throw new EmailVerificationServiceException(
                    "Invalid response from email verification service.",
                    ex
            );
        }

        if (json.has("error") && !json.get("error").isNull()) {
            // Do not log or return the raw response: it may contain
            // sensitive request details.
            throw new EmailVerificationServiceException(
                    "Email verification provider rejected the request. "
                            + "Check the API key, account access, and request parameters."
            );
        }

        String status = json.path("status").asText("unknown");

        switch (status.toLowerCase()) {
            case "valid":
                return true;

            case "invalid":
            case "spamtrap":
            case "abuse":
            case "do_not_mail":
                return false;

            default:
                throw new EmailVerificationServiceException(
                        "The email address could not be verified conclusively."
                );
        }
    }
}
