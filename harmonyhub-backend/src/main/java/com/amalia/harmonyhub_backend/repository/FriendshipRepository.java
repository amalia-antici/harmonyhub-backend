package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.Friendship;
import com.amalia.harmonyhub_backend.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // All accepted friends of a user
    @Query("SELECT f FROM Friendship f WHERE (f.sender.id = :userId OR f.receiver.id = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("userId") Long userId);

    // Pending requests received
    List<Friendship> findByReceiverIdAndStatus(Long receiverId, FriendshipStatus status);

    // Check if friendship already exists between two users
    @Query("SELECT f FROM Friendship f WHERE (f.sender.id = :a AND f.receiver.id = :b) OR (f.sender.id = :b AND f.receiver.id = :a)")
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);
}
