
package com.verify_x.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ZeroBounceEmailVerifier {

    private static final String API_URL =
            "https://api.zerobounce.net/v2/validate";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zerobounce.api-key:}")
    private String apiKey;

    public ZeroBounceEmailVerifier(
            ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public boolean isValidMailbox(String email) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Email verification service is not configured."
            );
        }

        String url = UriComponentsBuilder
                .fromUriString(API_URL)
                .queryParam("api_key", apiKey)
                .queryParam("email", email)
                .queryParam("timeout", 10)
                .encode()
                .toUriString();

        final String response;

        try {
            response = restTemplate.getForObject(
                    url, String.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Email verification service is temporarily unavailable.",
                    ex
            );
        }

        try {
            JsonNode json = objectMapper.readTree(response);

            if (json.has("error")) {
                throw new IllegalStateException(
                        "Email verification service returned an error."
                );
            }

            String status = json.path("status")
                    .asText("unknown");

            if ("valid".equalsIgnoreCase(status)) {
                return true;
            }

            if ("invalid".equalsIgnoreCase(status)
                    || "spamtrap".equalsIgnoreCase(status)
                    || "abuse".equalsIgnoreCase(status)
                    || "do_not_mail".equalsIgnoreCase(status)) {
                return false;
            }

            // Includes unknown and catch-all:
            // do not send an OTP when verification is inconclusive.
            throw new IllegalStateException(
                    "Email address could not be verified. "
                            + "Please try again with another address."
            );

        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Invalid response from email verification service.",
                    ex
            );
        }
    }
}
