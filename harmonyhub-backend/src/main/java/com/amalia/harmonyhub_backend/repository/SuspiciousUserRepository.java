package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.SuspiciousUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuspiciousUserRepository extends JpaRepository<SuspiciousUser, Long> {
    List<SuspiciousUser> findByUserIdOrderByTimestampDesc(String userId);

    void deleteByUserId(String userId);
}
