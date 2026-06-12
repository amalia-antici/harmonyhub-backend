package com.amalia.harmonyhub_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    @Enumerated(EnumType.STRING)
    private MusicalNote senderNote;   // note the sender picked

    @Enumerated(EnumType.STRING)
    private MusicalNote receiverNote; // note the receiver must match with

    private boolean harmonized; // true when the correct note is played

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public MusicalNote getSenderNote() {
        return senderNote;
    }

    public void setSenderNote(MusicalNote senderNote) {
        this.senderNote = senderNote;
    }

    public MusicalNote getReceiverNote() {
        return receiverNote;
    }

    public void setReceiverNote(MusicalNote receiverNote) {
        this.receiverNote = receiverNote;
    }

    public boolean isHarmonized() {
        return harmonized;
    }

    public void setHarmonized(boolean harmonized) {
        this.harmonized = harmonized;
    }
}