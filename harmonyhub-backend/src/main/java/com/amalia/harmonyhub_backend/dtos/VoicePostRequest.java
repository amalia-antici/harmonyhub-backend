package com.amalia.harmonyhub_backend.dtos;

public class VoicePostRequest {
    private String audio;       // base64
    private String description;

    public String getAudio() { return audio; }
    public String getDescription() { return description; }
}