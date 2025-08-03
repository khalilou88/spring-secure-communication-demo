package com.example.client;

import com.example.client.service.SecureApiClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(SecureApiClient apiClient) {
        return args -> {
            System.out.println("=== Secure API Client Demo ===");

            try {
                // Test GET request
                var message = apiClient.getSecureMessage();
                System.out.println("✅ GET Request successful:");
                System.out.println("   " + message);

                // Test POST request
                var response = apiClient.postSecureMessage("Hello from secure client!");
                System.out.println("✅ POST Request successful:");
                System.out.println("   " + response);

                // Test health endpoint
                var health = apiClient.getHealth();
                System.out.println("✅ Health check successful:");
                System.out.println("   " + health);

            } catch (Exception e) {
                System.err.println("❌ Error communicating with secure server:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}