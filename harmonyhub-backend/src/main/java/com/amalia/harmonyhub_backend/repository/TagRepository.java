package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.EventTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<EventTag, Long> {
    Optional<EventTag> findByName(String name);
}