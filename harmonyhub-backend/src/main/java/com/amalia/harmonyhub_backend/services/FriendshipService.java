package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.*;
import com.amalia.harmonyhub_backend.repository.FriendshipRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Friendship sendRequest(String senderUsername, Long receiverId, MusicalNote note) {
        // 🛠️ FIXED: Standard null checks instead of Optional wrapper handling
        User sender = userRepository.findByUsername(senderUsername);
        if (sender == null) {
            throw new RuntimeException("Sender not found");
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found")); // findById natively returns Optional, so this stays

        if (sender.getId().equals(receiverId)) {
            throw new RuntimeException("You cannot harmonize with yourself!");
        }

        // Look up using your custom repository query
        Optional<Friendship> existingOpt = friendshipRepository.findBetween(sender.getId(), receiver.getId());

        if (existingOpt.isPresent()) {
            Friendship existing = existingOpt.get();

            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new RuntimeException("You are already harmonized friends!");
            }

            if (existing.getStatus() == FriendshipStatus.PENDING) {
                throw new RuntimeException("A harmonize request is already pending!");
            }

            // If it was DECLINED (wrong note played before), reuse and reset it!
            if (existing.getStatus() == FriendshipStatus.DECLINED) {
                existing.setStatus(FriendshipStatus.PENDING);
                existing.setSender(sender);
                existing.setReceiver(receiver);
                existing.setSenderNote(note);
                existing.setReceiverNote(null);
                existing.setHarmonized(false);
                return friendshipRepository.save(existing);
            }
        }

        // If no request exists at all, make a new one
        Friendship newFriendship = new Friendship();
        newFriendship.setSender(sender);
        newFriendship.setReceiver(receiver);
        newFriendship.setStatus(FriendshipStatus.PENDING);
        newFriendship.setSenderNote(note);
        newFriendship.setHarmonized(false);

        return friendshipRepository.save(newFriendship);
    }

    @Transactional
    public Friendship respond(String receiverUsername, Long friendshipId, MusicalNote receivedNote) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        boolean correct = HarmonyRules.isHarmony(friendship.getSenderNote(), receivedNote);
        friendship.setReceiverNote(receivedNote);

        if (correct) {
            friendship.setStatus(FriendshipStatus.ACCEPTED);
            friendship.setHarmonized(true);
        } else {
            friendship.setStatus(FriendshipStatus.DECLINED);
            friendship.setHarmonized(false);
        }

        return friendshipRepository.save(friendship);
    }

    public List<Map<String, Object>> getPendingRequests(String username) {
        // 🛠️ FIXED: Standard null check to match your repo definition
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return friendshipRepository
                .findByReceiverIdAndStatus(user.getId(), FriendshipStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    public List<Map<String, Object>> getFriends(String username) {
        // 🛠️ FIXED: Standard null check to match your repo definition
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return friendshipRepository.findAcceptedFriendships(user.getId())
                .stream().map(f -> {
                    User friend = f.getSender().getId().equals(user.getId())
                            ? f.getReceiver() : f.getSender();
                    return toUserDto(friend, "ACCEPTED");
                }).toList();
    }

    public List<Map<String, Object>> getAllUsers(String currentUsername) {
        // 🛠️ FIXED: Standard null check to match your repo definition
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            throw new RuntimeException("Current user not found");
        }

        return userRepository.findAllByOrderByUsernameAsc()
                .stream()
                .filter(u -> !u.getUsername().equals(currentUsername))
                .map(u -> {
                    Optional<Friendship> fOpt = friendshipRepository.findBetween(currentUser.getId(), u.getId());
                    String status = fOpt.isPresent() ? fOpt.get().getStatus().toString() : "NONE";
                    return toUserDto(u, status);
                })
                .toList();
    }

    private Map<String, Object> toDto(Friendship f) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", f.getId());
        dto.put("status", f.getStatus());
        dto.put("createdAt", f.getCreatedAt());
        dto.put("sender", toUserDto(f.getSender(), f.getStatus().toString()));
        dto.put("receiver", toUserDto(f.getReceiver(), f.getStatus().toString()));
        dto.put("senderNote", f.getSenderNote());
        dto.put("harmonized", f.isHarmonized());
        return dto;
    }

    private Map<String, Object> toUserDto(User u) {
        return toUserDto(u, "NONE");
    }

    private Map<String, Object> toUserDto(User u, String friendshipStatus) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", u.getId());
        dto.put("username", u.getUsername());
        dto.put("firstName", u.getFirstName() != null ? u.getFirstName() : "");
        dto.put("lastName", u.getLastName() != null ? u.getLastName() : "");
        dto.put("photo", u.getPhoto() != null ? u.getPhoto() : "");
        dto.put("bio", u.getBio() != null ? u.getBio() : "");
        dto.put("skills", u.getSkills() != null ? u.getSkills() : "");
        dto.put("city", u.getCity() != null ? u.getCity() : "");
        dto.put("country", u.getCountry() != null ? u.getCountry() : "");
        dto.put("friendshipStatus", friendshipStatus);
        return dto;
    }
}