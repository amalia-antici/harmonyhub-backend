package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.dtos.VoicePostCommentRequest;
import com.amalia.harmonyhub_backend.dtos.VoicePostRequest;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.model.VoicePost;
import com.amalia.harmonyhub_backend.model.VoicePostComment;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import com.amalia.harmonyhub_backend.repository.VoicePostCommentRepository;
import com.amalia.harmonyhub_backend.repository.VoicePostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoicePostService {

    @Autowired
    private VoicePostRepository voicePostRepository;
    @Autowired private VoicePostCommentRepository commentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CloudinaryService cloudinaryService;

    public Map<String, Object> createPost(String username, VoicePostRequest request) {
        User author = userRepository.findByUsername(username);
        if (author == null) throw new RuntimeException("User not found");

        String audioUrl = cloudinaryService.uploadAudio(request.getAudio());
        if (audioUrl == null) throw new RuntimeException("Audio upload failed");

        VoicePost post = new VoicePost();
        post.setAuthor(author);
        post.setAudioUrl(audioUrl);
        post.setDescription(request.getDescription());

        return toDto(voicePostRepository.save(post), username);
    }

    public Page<Map<String, Object>> getFeed(Pageable pageable, String currentUsername) {
        return voicePostRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> toDto(post, currentUsername));
    }

    public Map<String, Object> getPost(Long id, String currentUsername) {
        VoicePost post = voicePostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return toDto(post, currentUsername);
    }

    public Map<String, Object> addComment(Long postId, String username, VoicePostCommentRequest request) {
        VoicePost post = voicePostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User author = userRepository.findByUsername(username);
        if (author == null) throw new RuntimeException("User not found");

        VoicePostComment comment = new VoicePostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.getContent());

        return commentToDto(commentRepository.save(comment));
    }

    public List<Map<String, Object>> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::commentToDto)
                .toList();
    }

    private Map<String, Object> toDto(VoicePost post, String currentUsername) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", post.getId());
        dto.put("audioUrl", post.getAudioUrl());
        dto.put("description", post.getDescription());
        dto.put("createdAt", post.getCreatedAt());
        dto.put("revealAt", post.getRevealAt());

        boolean revealed = LocalDateTime.now().isAfter(post.getRevealAt());
        boolean isOwnPost = post.getAuthor().getUsername().equals(currentUsername);

        if (revealed || isOwnPost) {
            Map<String, Object> authorDto = new HashMap<>();
            authorDto.put("username", post.getAuthor().getUsername());
            authorDto.put("photo", post.getAuthor().getPhoto());
            dto.put("author", authorDto);
            dto.put("revealed", true);
        } else {
            dto.put("author", null);
            dto.put("revealed", false);
        }

        return dto;
    }

    private Map<String, Object> commentToDto(VoicePostComment comment) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", comment.getId());
        dto.put("content", comment.getContent());
        dto.put("createdAt", comment.getCreatedAt());
        dto.put("author", Map.of(
                "username", comment.getAuthor().getUsername(),
                "photo", comment.getAuthor().getPhoto() != null
                        ? comment.getAuthor().getPhoto() : ""
        ));
        return dto;
    }
}
