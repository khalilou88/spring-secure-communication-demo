# Secure Spring Boot Project with Certificate Authority Chain

## Project Overview

This guide demonstrates how to create a secure Maven multi-module Spring Boot application using a proper certificate authority (CA) chain with root and intermediate certificates.

## Project Structure

```
secure-communication-demo/
├── pom.xml                         # Parent POM
├── certificates/                   # Certificate management
│   ├── ca/                         # Root CA
│   ├── intermediate/               # Intermediate CA
│   └── server/                     # Server certificates
├── common/                         # Shared components
│   ├── pom.xml
│   └── src/main/java/com/example/common/
│       └── dto/
│           └── Message.java
├── server/                         # Secure REST API
│   ├── pom.xml
│   ├── src/main/java/com/example/server/
│   │   ├── ServerApplication.java
│   │   └── controller/
│   │       └── SecureController.java
│   └── src/main/resources/
│       ├── application.properties
│       └── server-keystore.p12
└── client/                         # API Consumer
    ├── pom.xml
    ├── src/main/java/com/example/client/
    │   ├── ClientApplication.java
    │   └── service/
    │       └── SecureApiClient.java
    └── src/main/resources/
        ├── application.properties
        └── client-truststore.p12
```

## Step 1: Create Certificate Authority Chain

### 1.1 Create Root CA

```bash
# Create certificates directory structure
mkdir -p certificates/{ca,intermediate,server,client}
cd certificates

# Generate Root CA private key
keytool -genkeypair \
    -alias rootca \
    -keyalg RSA \
    -keysize 4096 \
    -validity 3650 \
    -keystore ca/rootca-keystore.p12 \
    -storetype PKCS12 \
    -storepass rootcapass \
    -keypass rootcapass \
    -dname "CN=Demo Root CA, OU=Security, O=Demo Corp, L=New York, ST=NY, C=US" \
    -ext KeyUsage=digitalSignature,keyCertSign \
    -ext BasicConstraints=ca:true,pathlen:1

# Export Root CA certificate
keytool -exportcert \
    -alias rootca \
    -keystore ca/rootca-keystore.p12 \
    -storetype PKCS12 \
    -storepass rootcapass \
    -file ca/rootca.crt \
    -rfc
```

### 1.2 Create Intermediate CA

```bash
# Generate Intermediate CA private key
keytool -genkeypair \
    -alias intermediateca \
    -keyalg RSA \
    -keysize 2048 \
    -validity 1825 \
    -keystore intermediate/intermediate-keystore.p12 \
    -storetype PKCS12 \
    -storepass intermediatecapass \
    -keypass intermediatecapass \
    -dname "CN=Demo Intermediate CA, OU=Security, O=Demo Corp, L=New York, ST=NY, C=US" \
    -ext KeyUsage=digitalSignature,keyCertSign \
    -ext BasicConstraints=ca:true,pathlen:0

# Generate Certificate Signing Request (CSR) for Intermediate CA
keytool -certreq \
    -alias intermediateca \
    -keystore intermediate/intermediate-keystore.p12 \
    -storetype PKCS12 \
    -storepass intermediatecapass \
    -file intermediate/intermediate.csr

# Sign Intermediate CA certificate with Root CA
keytool -gencert \
    -alias rootca \
    -keystore ca/rootca-keystore.p12 \
    -storetype PKCS12 \
    -storepass rootcapass \
    -infile intermediate/intermediate.csr \
    -outfile intermediate/intermediate.crt \
    -validity 1825 \
    -ext KeyUsage=digitalSignature,keyCertSign \
    -ext BasicConstraints=ca:true,pathlen:0 \
    -rfc

# Import Root CA certificate into Intermediate CA keystore
keytool -importcert \
    -alias rootca \
    -keystore intermediate/intermediate-keystore.p12 \
    -storetype PKCS12 \
    -storepass intermediatecapass \
    -file ca/rootca.crt \
    -noprompt

# Import signed Intermediate CA certificate
keytool -importcert \
    -alias intermediateca \
    -keystore intermediate/intermediate-keystore.p12 \
    -storetype PKCS12 \
    -storepass intermediatecapass \
    -file intermediate/intermediate.crt \
    -noprompt
```

### 1.3 Create Server Certificate

```bash
# Generate Server private key and certificate directly in the final keystore
keytool -genkeypair \
    -alias server \
    -keyalg RSA \
    -keysize 2048 \
    -validity 365 \
    -keystore server/server-keystore.p12 \
    -storetype PKCS12 \
    -storepass serverpass \
    -keypass serverpass \
    -dname "CN=localhost, OU=IT, O=Demo Corp, L=New York, ST=NY, C=US" \
    -ext SAN=dns:localhost,ip:127.0.0.1 \
    -ext KeyUsage=digitalSignature,keyEncipherment \
    -ext EKU=serverAuth

# Verify the keystore contains the server key entry
keytool -list \
    -keystore server/server-keystore.p12 \
    -storetype PKCS12 \
    -storepass serverpass

# You should see: server, ..., PrivateKeyEntry
```

### 1.4 Create Client Trust Store

```bash
# Create client truststore with root CA
keytool -importcert \
    -alias rootca \
    -keystore client/client-truststore.p12 \
    -storetype PKCS12 \
    -storepass truststorepass \
    -file ca/rootca.crt \
    -noprompt

# Verify client truststore
keytool -list \
    -keystore client/client-truststore.p12 \
    -storetype PKCS12 \
    -storepass truststorepass
```

## Step 2: Maven Project Setup

### 2.1 Parent POM (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>secure-communication-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <name>Secure Communication Demo</name>
    <description>Multi-module Spring Boot application with SSL/TLS security</description>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <modules>
        <module>common</module>
        <module>server</module>
        <module>client</module>
    </modules>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 2.2 Common Module (common/pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.example</groupId>
        <artifactId>secure-communication-demo</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>common</artifactId>
    <packaging>jar</packaging>
    
    <name>Common</name>
    <description>Shared components and DTOs</description>
    
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.3 Common Message DTO (common/src/main/java/com/example/common/dto/Message.java)

```java
package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

public class Message {
    private final String content;
    private final String sender;
    private final LocalDateTime timestamp;
    
    @JsonCreator
    public Message(@JsonProperty("content") String content,
                   @JsonProperty("sender") String sender,
                   @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.content = content;
        this.sender = sender;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }
    
    public Message(String content, String sender) {
        this(content, sender, LocalDateTime.now());
    }
    
    public String getContent() { return content; }
    public String getSender() { return sender; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(content, message.content) &&
               Objects.equals(sender, message.sender) &&
               Objects.equals(timestamp, message.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(content, sender, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("Message{content='%s', sender='%s', timestamp=%s}",
                content, sender, timestamp);
    }
}
```

## Step 3: Server Module

### 3.1 Server POM (server/pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.example</groupId>
        <artifactId>secure-communication-demo</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>server</artifactId>
    <packaging>jar</packaging>
    
    <name>Server</name>
    <description>Secure REST API Server</description>
    
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.2 Server Application (server/src/main/java/com/example/server/ServerApplication.java)

```java
package com.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
```

### 3.3 Secure Controller (server/src/main/java/com/example/server/controller/SecureController.java)

```java
package com.example.server.controller;

import com.example.common.dto.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/secure")
public class SecureController {
    
    @GetMapping("/message")
    public ResponseEntity<Message> getSecureMessage(Principal principal) {
        String sender = principal != null ? principal.getName() : "Server";
        Message message = new Message(
            "This is a secure message transmitted over HTTPS with proper certificate chain validation",
            sender
        );
        return ResponseEntity.ok(message);
    }
    
    @PostMapping("/message")
    public ResponseEntity<Message> postSecureMessage(@RequestBody Message message, Principal principal) {
        String sender = principal != null ? principal.getName() : "Anonymous";
        Message response = new Message(
            "Received: " + message.getContent(),
            sender,
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "ssl", "enabled",
            "certificateChain", "self-signed for demo"
        ));
    }
}
```

### 3.4 Server Configuration (server/src/main/resources/application.properties)

```properties
# Server Configuration
server.port=8443
server.ssl.enabled=true

# SSL Configuration - NOTE: No keystore subdirectory
server.ssl.key-store=classpath:server-keystore.p12
server.ssl.key-store-password=serverpass
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=server

# Application Configuration
spring.application.name=secure-server
logging.level.org.springframework.security=INFO
logging.level.org.springframework.web=INFO

# Actuator Configuration
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

## Step 4: Client Module

### 4.1 Client POM (client/pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.example</groupId>
        <artifactId>secure-communication-demo</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>client</artifactId>
    <packaging>jar</packaging>
    
    <name>Client</name>
    <description>Secure API Client</description>
    
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4.2 Client Application (client/src/main/java/com/example/client/ClientApplication.java)

```java
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
```

### 4.3 Secure API Client Service (client/src/main/java/com/example/client/service/SecureApiClient.java)

```java
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
        
        // Debug logging
        System.out.println("=== SSL Configuration Debug ===");
        System.out.println("Trust store resource: " + trustStore);
        System.out.println("Trust store password: " + (trustStorePassword != null ? "[SET]" : "[NOT SET]"));
        
        this.restTemplate = builder
                .requestFactory(() -> {
                    try {
                        return createSecureRequestFactory(trustStore, trustStorePassword);
                    } catch (Exception e) {
                        System.err.println("Failed to create secure request factory: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Failed to create secure request factory", e);
                    }
                })
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory createSecureRequestFactory(
            Resource trustStore, String trustStorePassword) throws Exception {
        
        System.out.println("=== Creating Secure Request Factory ===");
        
        // Load trust store
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var inputStream = trustStore.getInputStream()) {
            keyStore.load(inputStream, trustStorePassword.toCharArray());
            System.out.println("Trust store loaded successfully! Size: " + keyStore.size());
        }

        // Create trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        System.out.println("Trust manager factory initialized");

        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        System.out.println("SSL context created");

        // Create HTTP client with custom SSL context using Apache HttpClient 5
        var httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                .setConnectionManager(org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(sslContext)
                                .build())
                        .build())
                .build();

        System.out.println("HTTP client created with custom SSL context");
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
```

### 4.4 Client Configuration (client/src/main/resources/application.properties)

```properties
# Client Configuration
spring.application.name=secure-client
server.port=8080

# API Configuration
client.api.base-url=https://localhost:8443

# SSL Configuration - NOTE: No keystore subdirectory
client.ssl.trust-store=classpath:client-truststore.p12
client.ssl.trust-store-password=truststorepass

# Logging Configuration
logging.level.com.example.client=DEBUG
logging.level.org.springframework.web.client=DEBUG
logging.level.javax.net.ssl=DEBUG
```

## Step 5: Copy Certificates to Resources

```bash
# Copy server keystore to server module resources (root level)
cp certificates/server/server-keystore.p12 server/src/main/resources/

# Copy client truststore to client module resources (root level)
cp certificates/client/client-truststore.p12 client/src/main/resources/
```

## Step 6: Build and Run

### 6.1 Build the Project

```bash
# From the root directory
./mvnw clean install
```

### 6.2 Run the Server

```bash
# Terminal 1 - Start the server
./mvnw spring-boot:run -pl server
```

### 6.3 Run the Client

```bash
# Terminal 2 - Start the client with SSL debugging
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Djavax.net.debug=ssl:handshake:verbose" -pl client
```

## Step 7: Troubleshooting Common Issues

### Issue 1: "Alias name [server] does not identify a key entry"

**Solution**: The keystore doesn't contain a private key entry.

```bash
# Check keystore contents
keytool -list -keystore server/src/main/resources/server-keystore.p12 -storetype PKCS12 -storepass serverpass

# Look for "PrivateKeyEntry" for the server alias
# If missing, recreate with:
cd certificates
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -validity 365 \
    -keystore server/server-keystore.p12 -storetype PKCS12 \
    -storepass serverpass -keypass serverpass \
    -dname "CN=localhost, OU=IT, O=Demo Corp, L=New York, ST=NY, C=US" \
    -ext SAN=dns:localhost,ip:127.0.0.1

cp server/server-keystore.p12 ../server/src/main/resources/
```

### Issue 2: "PKIX path building failed"

**Solution**: Client doesn't trust the server certificate.

```bash
# Verify client truststore exists and contains root CA
keytool -list -keystore client/src/main/resources/client-truststore.p12 -storetype PKCS12 -storepass truststorepass

# If missing, create it:
keytool -importcert -alias rootca \
    -keystore client/src/main/resources/client-truststore.p12 \
    -storetype PKCS12 -storepass truststorepass \
    -file certificates/ca/rootca.crt -noprompt
```

try this solution:
```bash
# Add our root CA to the system trust store (requires sudo)
sudo keytool -importcert \
-alias demo-root-ca \
-keystore $JAVA_HOME/lib/security/cacerts \
-storepass changeit \
-file certificates/ca/rootca.crt \
-noprompt
```


### Issue 3: SSL Debug Mode

Run client with detailed SSL debugging:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Djavax.net.debug=ssl:handshake:verbose:trustmanager"
```

### Issue 4: Force Custom Trust Store

If the application ignores the custom trust store:

```bash
# Get absolute path to trust store
TRUSTSTORE_PATH=$(pwd)/client/src/main/resources/client-truststore.p12

# Run with JVM system properties
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Djavax.net.ssl.trustStore=$TRUSTSTORE_PATH -Djavax.net.ssl.trustStorePassword=truststorepass -Djavax.net.ssl.trustStoreType=PKCS12"
```

## Step 8: Verification and Testing

### 8.1 Test with curl

```bash
# Test with root CA (should work)
curl --cacert certificates/ca/rootca.crt -v https://localhost:8443/api/secure/health

# Test without CA (should fail)
curl -v https://localhost:8443/api/secure/health
```

### 8.2 Verify Certificate Chain

```bash
# Check server certificate chain
openssl s_client -connect localhost:8443 -servername localhost -showcerts

# Verify certificate locally (if using CA-signed certificates)
openssl verify -CAfile certificates/ca/rootca.crt certificates/server/server.crt
```

### 8.3 Expected Output

When everything works correctly, you should see:

**Server startup:**
```
INFO  - Tomcat started on port(s): 8443 (https)
INFO  - Started ServerApplication
```

**Client output:**
```
=== Secure API Client Demo ===
✅ GET Request successful:
   Message{content='This is a secure message...', sender='Server', timestamp=...}
✅ POST Request successful:
   Message{content='Received: Hello from secure client!', sender='Anonymous', timestamp=...}
✅ Health check successful:
   {status=UP, timestamp=..., ssl=enabled, certificateChain=self-signed for demo}
```

## Key Learning Points

1. **Keystore vs Truststore**: Server uses keystore (private key + certificate), client uses truststore (trusted CA certificates)
2. **Certificate Validation**: Client validates server certificate against trusted CAs
3. **SSL Context Configuration**: Custom SSL configuration in Apache HttpClient 5
4. **Debugging**: JVM SSL debugging flags help diagnose certificate issues
5. **File Locations**: Certificates must be in classpath resources for Spring Boot to find them

## Security Considerations

- **Self-signed certificates**: Only for development/testing
- **Certificate expiration**: Monitor and renew certificates
- **Private key security**: Protect keystore passwords
- **Trust management**: Carefully manage trusted CAs
- **Production deployment**: Use proper CA-issued certificates

This setup demonstrates the complete SSL/TLS certificate chain validation process in a Spring Boot application with proper separation of concerns between server authentication (keystore) and client validation (truststore).