package com.example.client.service;

import com.example.common.dto.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.Map;

@Service
public class SecureApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SecureApiClient(RestTemplateBuilder builder,
                           @Value("${client.api.base-url:https://localhost:8443}") String baseUrl,
                           @Value("${client.ssl.trust-store:classpath:client-truststore.p12}") Resource trustStore,
                           @Value("${client.ssl.trust-store-password:truststorepass}") String trustStorePassword) {
        this.baseUrl = baseUrl;


// Debug: Test trust store loading
        try {
            System.out.println("=== DEBUG: Testing trust store loading ===");
            System.out.println("Trust store resource: " + trustStore);
            System.out.println("Trust store exists: " + trustStore.exists());

            KeyStore testKeyStore = KeyStore.getInstance("PKCS12");
            try (var inputStream = trustStore.getInputStream()) {
                testKeyStore.load(inputStream, trustStorePassword.toCharArray());
                System.out.println("Trust store loaded successfully!");
                System.out.println("Trust store size: " + testKeyStore.size());
            }
        } catch (Exception e) {
            System.err.println("Error loading trust store: " + e.getMessage());
            e.printStackTrace();
        }




        this.restTemplate = builder
                .requestFactory(() -> {
                    try {
                        return createSecureRequestFactory(trustStore, trustStorePassword);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create secure request factory", e);
                    }
                })
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory createSecureRequestFactory(
            Resource trustStore, String trustStorePassword) throws Exception {

        // Load trust store
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var inputStream = trustStore.getInputStream()) {
            keyStore.load(inputStream, trustStorePassword.toCharArray());
        }

        // Create trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        // Create HTTP client with custom SSL context using Apache HttpClient 5
        var httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                .setConnectionManager(org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(sslContext)
                                .build())
                        .build())
                .build();

        return new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);
    }

    public Message getSecureMessage() {
        return restTemplate.getForObject(baseUrl + "/api/secure/message", Message.class);
    }

    public Message postSecureMessage(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Message message = new Message(content, "Client");
        HttpEntity<Message> request = new HttpEntity<>(message, headers);

        return restTemplate.exchange(
                baseUrl + "/api/secure/message",
                HttpMethod.POST,
                request,
                Message.class
        ).getBody();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getHealth() {
        return restTemplate.getForObject(baseUrl + "/api/secure/health", Map.class);
    }
}