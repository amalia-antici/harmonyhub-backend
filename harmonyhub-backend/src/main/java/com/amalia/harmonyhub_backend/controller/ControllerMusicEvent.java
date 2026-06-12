package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.model.*;
import com.amalia.harmonyhub_backend.repository.MusicEventRepository;
import com.amalia.harmonyhub_backend.repository.TagRepository;
import com.amalia.harmonyhub_backend.services.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import com.amalia.harmonyhub_backend.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class ControllerMusicEvent {

    private final ServiceMusicEvents service;
    private final UserRepository userRepository;
    private final LoggingService loggingService;
    private final BehaviorMonitorService behaviorMonitorService;
    private final MusicEventRepository musicEventRepository;
    private final TagRepository tagRepository;

    @Autowired
    private ServiceFakerGenerator generateService;

    @Autowired
    private AiMonitorService aiMonitorService;

    @Autowired private CloudinaryService cloudinaryService;


    public ControllerMusicEvent(ServiceMusicEvents service,
                                UserRepository userRepository,
                                LoggingService loggingService,
                                BehaviorMonitorService behaviorMonitorService,
                                MusicEventRepository musicEventRepository,
                                TagRepository tagRepository
                                ) {
        this.service = service;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.behaviorMonitorService = behaviorMonitorService;
        this.musicEventRepository=musicEventRepository;
        this.tagRepository=tagRepository;
    }

    @GetMapping
    public ResponseEntity<List<MusicEvent>> getAllEvents(@RequestParam(defaultValue = "-1") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getPaginatedEvents(page, size));
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody MusicEvent event) {
        User creator = getCurrentAuthenticatedUser();
        if (creator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Sign in first!");
        }

        if (event.getPhotoUrl() != null && event.getPhotoUrl().startsWith("data:image")) {
            String cloudUrl = cloudinaryService.uploadBase64Image(event.getPhotoUrl());
            if (cloudUrl != null) {
                event.setPhotoUrl(cloudUrl);
            }
        }

        event.setCreatedBy(creator);
        MusicEvent savedEvent = service.addEvent(event);
        loggingService.record(creator, "CREATED EVENT: " + savedEvent.getTitle());
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @Valid @RequestBody MusicEvent event) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Sign in first!");
        }

        MusicEvent existingEvent = service.getEventById(id);
        if (existingEvent == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        boolean isOwner = existingEvent.getCreatedBy() != null &&
                existingEvent.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to edit this event.");
        }

        if (event.getPhotoUrl() != null && event.getPhotoUrl().startsWith("data:image")) {
            String cloudUrl = cloudinaryService.uploadBase64Image(event.getPhotoUrl());
            if (cloudUrl != null) {
                event.setPhotoUrl(cloudUrl);
            }
        }

        event.setCreatedBy(existingEvent.getCreatedBy());

        MusicEvent updatedEv = service.updateEvent(id, event);
        if (updatedEv != null) {
            loggingService.record(currentUser, "UPDATE EVENT ID [" + id + "]: " + updatedEv.getTitle());
            return ResponseEntity.ok(updatedEv);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Sign in first!");
        }

        MusicEvent existingEvent = service.getEventById(id);
        if (existingEvent == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        boolean isOwner = existingEvent.getCreatedBy() != null &&
                existingEvent.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to delete this event.");
        }

        boolean deleted = service.deleteEvent(id);
        if (deleted) {
            AuditLog log = loggingService.record(currentUser, "DELETED EVENT ID: " + id);
            behaviorMonitorService.analyzeLog(log);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats/days")
    public ResponseEntity<Map<DayOfWeek, Long>> getDayStats() {
        return ResponseEntity.ok(service.getEventsCountByWeekDay());
    }

    @GetMapping("/stats/genres")
    public ResponseEntity<Map<MusicEvent.Genre, Long>> getGenreStats() {
        return ResponseEntity.ok(service.getEventsCountByGenre());
    }

    @GetMapping("/generator-status")
    public ResponseEntity<Boolean> getGeneratorStatus() {
        return ResponseEntity.ok(generateService.isRunning());
    }

    @PostMapping("/start-generator")
    public ResponseEntity<String> startGenerator() {
        if (!generateService.isRunning()) {
            generateService.start();
            return ResponseEntity.ok("Generator started running");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Generator is already running!");
    }

    @PostMapping("/stop-generator")
    public ResponseEntity<String> stopGenerator() {
        generateService.stop();
        return ResponseEntity.ok("Generator stopped");
    }

    @GetMapping("/stats/tags")
    public ResponseEntity<Map<String, Long>> getTagStats() {
        return ResponseEntity.ok(service.getTagUsageStats());
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getAuditLogs() {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin only");
        }

        return ResponseEntity.ok(loggingService.getAllLogs());
    }

    @GetMapping("/observations")
    public ResponseEntity<?> getObservations() {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin only");
        }

        return ResponseEntity.ok(behaviorMonitorService.getObservationList());
    }
    @GetMapping("/stats/heavy")
    public ResponseEntity<?> getHeavyStats() {
        return ResponseEntity.ok(service.getHeavyTagStats());
    }
    @PostMapping("/seed")
    public ResponseEntity<?> seedDatabase(@RequestParam(defaultValue = "1000") int count) {
        System.out.println(">>> SEED CALLED with count: " + count);
        return ResponseEntity.ok(service.seedWithFaker(count));
    }

    @PostMapping("/stats/heavy/evict")
    public ResponseEntity<?> evictHeavyStatsCache() {
        service.evictHeavyStatsCache();
        return ResponseEntity.ok("Cache evicted");
    }

    @GetMapping("/observations/ai-analysis")
    public ResponseEntity<?> getAiThreatAnalysis() {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin only");
        }

        List<SuspiciousUser> observations = behaviorMonitorService.getObservationList();
        String analysis = aiMonitorService.analyzeThreats(observations);
        return ResponseEntity.ok(Map.of(
                "analysis", analysis,
                "totalFlags", observations.size()
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MusicEvent>> searchEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(service.searchEvents(page, size, genre, city, date));
    }

    @PatchMapping("/{id}/attend")
    public ResponseEntity<?> toggleEventAttendance(@PathVariable Long id, @RequestParam boolean isAttending) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Sign in first!");
        }

        try {
            MusicEvent updatedEvent = service.toggleAttendance(id, isAttending);
            loggingService.record(currentUser, (isAttending ? "UNATTENDED" : "ATTENDED") + " EVENT: " + updatedEvent.getTitle());
            return ResponseEntity.ok(updatedEvent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


}