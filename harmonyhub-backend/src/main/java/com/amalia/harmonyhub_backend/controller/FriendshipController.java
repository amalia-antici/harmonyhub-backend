package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.dtos.HarmonizeRequest;
import com.amalia.harmonyhub_backend.services.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    private String me() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(friendshipService.getAllUsers(me()));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(friendshipService.getPendingRequests(me()));
    }

    @GetMapping
    public ResponseEntity<?> getFriends() {
        return ResponseEntity.ok(friendshipService.getFriends(me()));
    }

    @PostMapping("/request/{receiverId}")
    public ResponseEntity<?> sendRequest(@PathVariable Long receiverId,
                                         @RequestBody HarmonizeRequest body) {
        try {
            return ResponseEntity.ok(friendshipService.sendRequest(me(), receiverId, body.getNote()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/respond/{friendshipId}")
    public ResponseEntity<?> respond(@PathVariable Long friendshipId,
                                     @RequestBody HarmonizeRequest body) {
        try {
            return ResponseEntity.ok(friendshipService.respond(me(), friendshipId, body.getNote()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
