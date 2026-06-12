package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.VoicePostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoicePostCommentRepository extends JpaRepository<VoicePostComment, Long> {
    List<VoicePostComment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
