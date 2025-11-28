package com.ausganslage.ausgangslageBackend.dto;

public class ChatMessageRequest {
    private String content;

    public ChatMessageRequest() {
    }

    public ChatMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

