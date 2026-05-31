package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.MusicEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface MusicEventRepository extends JpaRepository<MusicEvent, Long> {

    Page<MusicEvent> findAll(Pageable pageable);

    @Query("SELECT m FROM MusicEvent m WHERE m.city = :city")
    List<MusicEvent> findByCity(@Param("city") String city);

    @Query("SELECT e.genre, COUNT(e) FROM MusicEvent e GROUP BY e.genre")
    List<Object[]> countEventsByGenre();


    @Query("SELECT FUNCTION('DAYNAME', e.dateTime), COUNT(e) FROM MusicEvent e GROUP BY FUNCTION('DAYNAME', e.dateTime)")
    List<Object[]> countEventsByDayOfWeek();

    @Query(value = """
    SELECT 
        t.name AS tagName,
        COUNT(DISTINCT e.id) AS eventCount,
        COUNT(DISTINCT e.created_by_user_id) AS uniqueCreators,
        AVG(e.capacity) AS avgCapacity,
        MAX(e.capacity) AS maxCapacity,
        MIN(e.date_time) AS earliestEvent
    FROM event_tags_mapping et
    JOIN music_events e ON e.id = et.event_id
    JOIN tags t ON t.id = et.tag_id
    GROUP BY t.name
    ORDER BY eventCount DESC
    """, nativeQuery = true)
    List<Map<String, Object>> getHeavyTagStats();
}