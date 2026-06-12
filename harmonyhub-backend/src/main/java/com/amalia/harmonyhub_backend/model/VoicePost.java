package com.amalia.harmonyhub_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class VoicePost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    private String audioUrl;

    @Column(length = 500)
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime revealAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<VoicePostComment> comments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        revealAt = createdAt.plusHours(24);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRevealAt() {
        return revealAt;
    }

    public void setRevealAt(LocalDateTime revealAt) {
        this.revealAt = revealAt;
    }

    public List<VoicePostComment> getComments() {
        return comments;
    }

    public void setComments(List<VoicePostComment> comments) {
        this.comments = comments;
    }
}

