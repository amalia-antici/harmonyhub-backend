package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.VoicePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoicePostRepository extends JpaRepository<VoicePost, Long> {
    Page<VoicePost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
