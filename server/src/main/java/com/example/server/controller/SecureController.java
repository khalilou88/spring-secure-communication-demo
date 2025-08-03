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
                "certificateChain", "root -> intermediate -> server"
        ));
    }
}