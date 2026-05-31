package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.EventTag;
import com.amalia.harmonyhub_backend.model.MusicEvent;
import com.amalia.harmonyhub_backend.repository.MusicEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceMusicEventsTest {

    @Mock
    private MusicEventRepository repository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ServiceMusicEvents service;

    private MusicEvent mondayJazzEvent;
    private MusicEvent tuesdayRockEvent;
    private MusicEvent tuesdayPopEvent;

    @BeforeEach
    void setUp() {
        // Monday, May 4 2026
        mondayJazzEvent = new MusicEvent();
        mondayJazzEvent.setId(1L);
        mondayJazzEvent.setTitle("Jazz Night");
        mondayJazzEvent.setCity("Cluj-Napoca");
        mondayJazzEvent.setGenre(MusicEvent.Genre.JAZZ);
        mondayJazzEvent.setDateTime(LocalDateTime.of(2026, 5, 4, 20, 0));

        // Tuesday, May 5 2026
        tuesdayRockEvent = new MusicEvent();
        tuesdayRockEvent.setId(2L);
        tuesdayRockEvent.setTitle("Rock Blast");
        tuesdayRockEvent.setCity("Bucharest");
        tuesdayRockEvent.setGenre(MusicEvent.Genre.ROCK);
        tuesdayRockEvent.setDateTime(LocalDateTime.of(2026, 5, 5, 20, 0));

        // Tuesday, May 12 2026
        tuesdayPopEvent = new MusicEvent();
        tuesdayPopEvent.setId(3L);
        tuesdayPopEvent.setTitle("Pop Party");
        tuesdayPopEvent.setCity("Cluj-Napoca");
        tuesdayPopEvent.setGenre(MusicEvent.Genre.POP);
        tuesdayPopEvent.setDateTime(LocalDateTime.of(2026, 5, 12, 20, 0));
    }


    @Test
    @DisplayName("getAllEvents - should return all events from repository")
    void testGetAllEvents() {
        when(repository.findAll()).thenReturn(List.of(mondayJazzEvent, tuesdayRockEvent, tuesdayPopEvent));

        List<MusicEvent> result = service.getAllEvents();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(MusicEvent::getTitle)
                .containsExactlyInAnyOrder("Jazz Night", "Rock Blast", "Pop Party");
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllEvents - should return empty list when no events exist")
    void testGetAllEventsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        List<MusicEvent> result = service.getAllEvents();

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("getPaginatedEvents - page -1 should return all events unpaginated")
    void testGetPaginatedEventsAllWhenPageMinusOne() {
        when(repository.findAll()).thenReturn(List.of(mondayJazzEvent, tuesdayRockEvent, tuesdayPopEvent));

        List<MusicEvent> result = service.getPaginatedEvents(-1, 10);

        assertThat(result).hasSize(3);
        verify(repository, times(1)).findAll();
        verify(repository, never()).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("getPaginatedEvents - valid page should return paginated content")
    void testGetPaginatedEventsWithPage() {
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<MusicEvent> mockPage = new PageImpl<>(
                List.of(mondayJazzEvent, tuesdayRockEvent), pageRequest, 3
        );
        when(repository.findAll(PageRequest.of(0, 2))).thenReturn(mockPage);

        List<MusicEvent> result = service.getPaginatedEvents(0, 2);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MusicEvent::getTitle)
                .containsExactly("Jazz Night", "Rock Blast");
    }

    @Test
    @DisplayName("getPaginatedEvents - second page should return remaining events")
    void testGetPaginatedEventsSecondPage() {
        PageRequest pageRequest = PageRequest.of(1, 2);
        Page<MusicEvent> mockPage = new PageImpl<>(
                List.of(tuesdayPopEvent), pageRequest, 3
        );
        when(repository.findAll(PageRequest.of(1, 2))).thenReturn(mockPage);

        List<MusicEvent> result = service.getPaginatedEvents(1, 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Pop Party");
    }



    @Test
    @DisplayName("addEvent - should save event, broadcast update, and return saved event")
    void testAddEvent() {
        when(repository.save(mondayJazzEvent)).thenReturn(mondayJazzEvent);
        when(repository.findAll()).thenReturn(List.of(mondayJazzEvent));

        MusicEvent result = service.addEvent(mondayJazzEvent);

        assertThat(result).isEqualTo(mondayJazzEvent);
        verify(repository, times(1)).save(mondayJazzEvent);
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/events"), any(List.class));
    }


    @Test
    @DisplayName("getEventsCountByGenre - should return correct genre-to-count map")
    void testGetEventsCountByGenre() {
        List<Object[]> rawStats = List.of(
                new Object[]{MusicEvent.Genre.JAZZ, 1L},
                new Object[]{MusicEvent.Genre.ROCK, 1L},
                new Object[]{MusicEvent.Genre.POP,  1L}
        );
        when(repository.countEventsByGenre()).thenReturn(rawStats);

        Map<MusicEvent.Genre, Long> result = service.getEventsCountByGenre();

        assertThat(result).hasSize(3);
        assertThat(result).containsEntry(MusicEvent.Genre.JAZZ, 1L);
        assertThat(result).containsEntry(MusicEvent.Genre.ROCK, 1L);
        assertThat(result).containsEntry(MusicEvent.Genre.POP,  1L);
    }

    @Test
    @DisplayName("getEventsCountByGenre - should return empty map when no data")
    void testGetEventsCountByGenreEmpty() {
        when(repository.countEventsByGenre()).thenReturn(List.of());

        Map<MusicEvent.Genre, Long> result = service.getEventsCountByGenre();

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("getEventsCountByWeekDay - should count correctly and include all 7 days")
    void testGetEventsCountByWeekDay() {
        when(repository.findAll()).thenReturn(
                List.of(mondayJazzEvent, tuesdayRockEvent, tuesdayPopEvent)
        );

        Map<DayOfWeek, Long> result = service.getEventsCountByWeekDay();

        assertThat(result).hasSize(7); // all days present
        assertThat(result).containsEntry(DayOfWeek.MONDAY, 1L);
        assertThat(result).containsEntry(DayOfWeek.TUESDAY, 2L);
        assertThat(result).containsEntry(DayOfWeek.WEDNESDAY, 0L);
        assertThat(result).containsEntry(DayOfWeek.SUNDAY, 0L);
    }

    @Test
    @DisplayName("getEventsCountByWeekDay - should return all days as 0 when no events")
    void testGetEventsCountByWeekDayEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        Map<DayOfWeek, Long> result = service.getEventsCountByWeekDay();

        assertThat(result).hasSize(7);
        assertThat(result.values()).allMatch(count -> count == 0L);
    }


    @Test
    @DisplayName("getTagUsageStats - should count tag usage across events")
    void testGetTagUsageStats() {
        EventTag tagRomantic = new EventTag();
        tagRomantic.setName("romantic");

        EventTag tagLive = new EventTag();
        tagLive.setName("live");

        mondayJazzEvent.setTags(List.of(tagRomantic, tagLive));
        tuesdayRockEvent.setTags(List.of(tagLive));
        tuesdayPopEvent.setTags(List.of());

        when(repository.findAll()).thenReturn(
                List.of(mondayJazzEvent, tuesdayRockEvent, tuesdayPopEvent)
        );

        Map<String, Long> result = service.getTagUsageStats();

        assertThat(result).containsEntry("romantic", 1L);
        assertThat(result).containsEntry("live", 2L);
        assertThat(result).doesNotContainKey("nonexistent");
    }

    @Test
    @DisplayName("getTagUsageStats - should return empty map when events have no tags")
    void testGetTagUsageStatsNoTags() {
        mondayJazzEvent.setTags(List.of());
        tuesdayRockEvent.setTags(List.of());

        when(repository.findAll()).thenReturn(List.of(mondayJazzEvent, tuesdayRockEvent));

        Map<String, Long> result = service.getTagUsageStats();

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("deleteEvent - should delete existing event, broadcast, and return true")
    void testDeleteEventExists() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of(tuesdayRockEvent, tuesdayPopEvent));

        boolean result = service.deleteEvent(1L);

        assertThat(result).isTrue();
        verify(repository, times(1)).deleteById(1L);
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/events"), any(List.class));
    }

    @Test
    @DisplayName("deleteEvent - should return false and not delete when event not found")
    void testDeleteEventNotFound() {
        when(repository.existsById(999L)).thenReturn(false);

        boolean result = service.deleteEvent(999L);

        assertThat(result).isFalse();
        verify(repository, never()).deleteById(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }


    @Test
    @DisplayName("updateEvent - should update existing event, broadcast, and return saved event")
    void testUpdateEventExists() {
        MusicEvent updated = new MusicEvent();
        updated.setTitle("Jazz Night Extended");
        updated.setCity("Cluj-Napoca");
        updated.setGenre(MusicEvent.Genre.JAZZ);
        updated.setDateTime(LocalDateTime.of(2026, 5, 4, 22, 0));

        MusicEvent savedEvent = new MusicEvent();
        savedEvent.setId(1L);
        savedEvent.setTitle("Jazz Night Extended");

        when(repository.findById(1L)).thenReturn(Optional.of(mondayJazzEvent));
        when(repository.save(updated)).thenReturn(savedEvent);
        when(repository.findAll()).thenReturn(List.of(savedEvent));

        MusicEvent result = service.updateEvent(1L, updated);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Jazz Night Extended");
        assertThat(updated.getId()).isEqualTo(1L); // id was set on the updated object
        verify(repository, times(1)).save(updated);
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/events"), any(List.class));
    }

    @Test
    @DisplayName("updateEvent - should return null when event not found")
    void testUpdateEventNotFound() {
        MusicEvent updated = new MusicEvent();
        updated.setTitle("Ghost Event");

        when(repository.findById(999L)).thenReturn(Optional.empty());

        MusicEvent result = service.updateEvent(999L, updated);

        assertThat(result).isNull();
        verify(repository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}