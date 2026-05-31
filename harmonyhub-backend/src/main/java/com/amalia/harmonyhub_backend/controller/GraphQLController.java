package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.model.*;
import com.amalia.harmonyhub_backend.repository.AuditLogRepository;
import com.amalia.harmonyhub_backend.repository.SuspiciousUserRepository;
import com.amalia.harmonyhub_backend.repository.TagRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository; // Import your repository
import com.amalia.harmonyhub_backend.services.BehaviorMonitorService;
import com.amalia.harmonyhub_backend.services.LoggingService;
import com.amalia.harmonyhub_backend.services.ServiceMusicEvents;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
public class GraphQLController {
    private final ServiceMusicEvents service;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final LoggingService loggingService;
    private final AuditLogRepository auditLogRepository;
    private final BehaviorMonitorService behaviorMonitorService;
    private final SuspiciousUserRepository suspiciousUserRepository;

    public GraphQLController(ServiceMusicEvents service, UserRepository userRepository, TagRepository tagRepository,
                             LoggingService loggingService, AuditLogRepository auditLogRepository,
                             BehaviorMonitorService behaviorMonitorService, SuspiciousUserRepository suspiciousUserRepository) {
        this.service = service;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.loggingService = loggingService;
        this.auditLogRepository = auditLogRepository;
        this.behaviorMonitorService = behaviorMonitorService;
        this.suspiciousUserRepository = suspiciousUserRepository;
    }

    @QueryMapping
    public List<AuditLog> allLogs() {
        return auditLogRepository.findAll();
    }

    @QueryMapping
    public List<MusicEvent> allEvents() {
        return service.getAllEvents();
    }

    @QueryMapping
    public MusicEvent eventById(@Argument Long id) {
        return service.getAllEvents().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @MutationMapping
    public MusicEvent createEvent(@Argument EventInput input) {
        // Get authenticated user from token
        User creator = getCurrentAuthenticatedUser();

        MusicEvent newEvent = new MusicEvent();
        newEvent.setCreatedBy(creator);

        mapInputToEvent(input, newEvent);
        MusicEvent saved = service.addEvent(newEvent);

        loggingService.record(creator, "CREATED EVENT: " + input.Title());
        return saved;
    }

    @MutationMapping
    public MusicEvent updateEvent(@Argument Long id, @Argument EventInput input) {
        User currentUser = getCurrentAuthenticatedUser();

        MusicEvent existingEvent = service.getEventById(id);
        if (existingEvent == null) {
            throw new RuntimeException("Event not found with id: " + id);
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        boolean isOwner = existingEvent.getCreatedBy() != null &&
                existingEvent.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("You do not have permission to edit this event.");
        }

        User originalCreator = existingEvent.getCreatedBy();
        mapInputToEvent(input, existingEvent);
        existingEvent.setCreatedBy(originalCreator);

        MusicEvent updated = service.updateEvent(id, existingEvent);
        loggingService.record(currentUser, "UPDATE EVENT ID [" + id + "]: " + input.Title());

        return updated;
    }

    @MutationMapping
    public Boolean deleteEvent(@Argument Long id) {
        // Get authenticated user from token
        User currentUser = getCurrentAuthenticatedUser();

        MusicEvent existingEvent = service.getEventById(id);
        if (existingEvent == null) {
            throw new RuntimeException("Event not found with id: " + id);
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        boolean isOwner = existingEvent.getCreatedBy() != null &&
                existingEvent.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("You do not have permission to delete this event.");
        }

        boolean deleted = service.deleteEvent(id);
        if (deleted) {
            AuditLog log = loggingService.record(currentUser, "DELETED EVENT ID: " + id);
            behaviorMonitorService.analyzeLog(log);
        }

        return deleted;
    }

    @QueryMapping
    public List<SuspiciousUser> observationList() {
        return suspiciousUserRepository.findAll();
    }

    private void mapInputToEvent(EventInput input, MusicEvent event) {
        event.setTitle(input.Title());
        event.setLocation(input.Location());
        event.setCity(input.City());
        event.setCountry(input.Country());
        event.setDescription(input.Description());
        event.setPhotoUrl(input.PhotoUrl());
        event.setFormLink(input.FormLink());
        event.setCapacity(input.Capacity());

        if (input.EventType() != null) {
            event.setEventType(MusicEvent.EventType.valueOf(input.EventType()));
        }
        if (input.Genre() != null) {
            event.setGenre(MusicEvent.Genre.valueOf(input.Genre()));
        }
        if (input.DateTime() != null) {
            event.setDateTime(LocalDateTime.parse(input.DateTime()));
        }
        if (input.Tags() != null) {
            List<EventTag> tags = input.Tags().stream()
                    .map(tagInput -> {
                        return tagRepository.findByName(tagInput.Name())
                                .orElseGet(() -> {
                                    EventTag tag = new EventTag();
                                    tag.setName(tagInput.Name());
                                    return tag;
                                });
                    }).collect(Collectors.toList());
            event.setTags(tags);
        }
    }

    public record TagInput(String Name) {}
    public record EventInput(
            String Title, String Location, String City, String Country,
            String EventType, String Genre, Integer Capacity,
            String Description, String DateTime, String PhotoUrl,
            String FormLink, List<TagInput> Tags
    ) {}

    private User getCurrentAuthenticatedUser(){
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())){
            throw new RuntimeException("Unauthorized: Sign in first!");
        }
        String username=authentication.getName();
        return userRepository.findByUsername(username);
    }

}