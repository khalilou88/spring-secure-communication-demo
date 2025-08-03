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