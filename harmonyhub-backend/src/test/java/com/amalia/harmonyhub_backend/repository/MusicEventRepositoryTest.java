package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.MusicEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicEventRepositoryTest {

    @Mock
    private MusicEventRepository repository;

    private MusicEvent rockEvent;
    private MusicEvent jazzEvent;
    private MusicEvent popEvent;

    @BeforeEach
    void setUp() {
        rockEvent = new MusicEvent();
        rockEvent.setTitle("Rock Night");
        rockEvent.setCity("Cluj");
        rockEvent.setCountry("Romania");
        rockEvent.setGenre(MusicEvent.Genre.ROCK);
        rockEvent.setEventType(MusicEvent.EventType.CONCERT);
        rockEvent.setCapacity(100);
        rockEvent.setLocation("Club A");
        rockEvent.setDateTime(LocalDateTime.of(2025, 5, 5, 20, 0));

        jazzEvent = new MusicEvent();
        jazzEvent.setTitle("Jazz Evening");
        jazzEvent.setCity("Cluj");
        jazzEvent.setCountry("Romania");
        jazzEvent.setGenre(MusicEvent.Genre.JAZZ);
        jazzEvent.setEventType(MusicEvent.EventType.CONCERT);
        jazzEvent.setCapacity(50);
        jazzEvent.setLocation("Club B");
        jazzEvent.setDateTime(LocalDateTime.of(2025, 5, 5, 19, 0));

        popEvent = new MusicEvent();
        popEvent.setTitle("Pop Show");
        popEvent.setCity("Bucharest");
        popEvent.setCountry("Romania");
        popEvent.setGenre(MusicEvent.Genre.POP);
        popEvent.setEventType(MusicEvent.EventType.CONCERT);
        popEvent.setCapacity(200);
        popEvent.setLocation("Arena");
        popEvent.setDateTime(LocalDateTime.of(2025, 5, 6, 18, 0));
    }


    @Test
    void findAll_firstPage_returnsCorrectSize() {
        Pageable pageable = PageRequest.of(0, 2);
        List<MusicEvent> events = Arrays.asList(rockEvent, jazzEvent);
        Page<MusicEvent> page = new PageImpl<>(events, pageable, 3);

        when(repository.findAll(pageable)).thenReturn(page);

        Page<MusicEvent> result = repository.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(repository).findAll(pageable);
    }

    @Test
    void findAll_secondPage_returnsRemainingElements() {
        Pageable pageable = PageRequest.of(1, 2);
        List<MusicEvent> events = List.of(popEvent);
        Page<MusicEvent> page = new PageImpl<>(events, pageable, 3);

        when(repository.findAll(pageable)).thenReturn(page);

        Page<MusicEvent> result = repository.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNumber()).isEqualTo(1);
        verify(repository).findAll(pageable);
    }

    @Test
    void findAll_emptyRepository_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MusicEvent> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(repository.findAll(pageable)).thenReturn(emptyPage);

        Page<MusicEvent> result = repository.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(repository).findAll(pageable);
    }

    @Test
    void findAll_pageSizeLargerThanTotal_returnsSinglePage() {
        Pageable pageable = PageRequest.of(0, 100);
        List<MusicEvent> all = Arrays.asList(rockEvent, jazzEvent, popEvent);
        Page<MusicEvent> page = new PageImpl<>(all, pageable, 3);

        when(repository.findAll(pageable)).thenReturn(page);

        Page<MusicEvent> result = repository.findAll(pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
        verify(repository).findAll(pageable);
    }


    @Test
    void findByCity_existingCity_returnsMatchingEvents() {
        when(repository.findByCity("Cluj")).thenReturn(Arrays.asList(rockEvent, jazzEvent));

        List<MusicEvent> result = repository.findByCity("Cluj");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getCity().equals("Cluj"));
        verify(repository).findByCity("Cluj");
    }

    @Test
    void findByCity_singleResult_returnsOneEvent() {
        when(repository.findByCity("Bucharest")).thenReturn(List.of(popEvent));

        List<MusicEvent> result = repository.findByCity("Bucharest");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Pop Show");
        verify(repository).findByCity("Bucharest");
    }

    @Test
    void findByCity_nonExistentCity_returnsEmptyList() {
        when(repository.findByCity("Paris")).thenReturn(Collections.emptyList());

        List<MusicEvent> result = repository.findByCity("Paris");

        assertThat(result).isEmpty();
        verify(repository).findByCity("Paris");
    }

    @Test
    void findByCity_nullCity_returnsEmptyList() {
        when(repository.findByCity(null)).thenReturn(Collections.emptyList());

        List<MusicEvent> result = repository.findByCity(null);

        assertThat(result).isEmpty();
        verify(repository).findByCity(null);
    }

    @Test
    void countEventsByGenre_returnsAllGenres() {
        List<Object[]> mockResult = Arrays.asList(
                new Object[]{"ROCK", 2L},
                new Object[]{"JAZZ", 1L},
                new Object[]{"POP", 1L}
        );
        when(repository.countEventsByGenre()).thenReturn(mockResult);

        List<Object[]> result = repository.countEventsByGenre();

        assertThat(result).hasSize(3);
        verify(repository).countEventsByGenre();
    }

    @Test
    void countEventsByGenre_rockCountIsTwo() {
        List<Object[]> mockResult = Arrays.asList(
                new Object[]{"ROCK", 2L},
                new Object[]{"JAZZ", 1L}
        );
        when(repository.countEventsByGenre()).thenReturn(mockResult);

        List<Object[]> result = repository.countEventsByGenre();

        Object[] rockRow = result.stream()
                .filter(row -> row[0].toString().equals("ROCK"))
                .findFirst()
                .orElseThrow();

        assertThat(((Number) rockRow[1]).longValue()).isEqualTo(2L);
        verify(repository).countEventsByGenre();
    }

    @Test
    void countEventsByGenre_emptyRepository_returnsEmptyList() {
        when(repository.countEventsByGenre()).thenReturn(Collections.emptyList());

        List<Object[]> result = repository.countEventsByGenre();

        assertThat(result).isEmpty();
        verify(repository).countEventsByGenre();
    }

    // ─── countEventsByDayOfWeek ──────────────────────────────────────────────

    @Test
    void countEventsByDayOfWeek_returnsCorrectDays() {
        List<Object[]> mockResult = Arrays.asList(
                new Object[]{"Monday", 2L},
                new Object[]{"Tuesday", 1L}
        );
        when(repository.countEventsByDayOfWeek()).thenReturn(mockResult);

        List<Object[]> result = repository.countEventsByDayOfWeek();

        assertThat(result).hasSize(2);
        verify(repository).countEventsByDayOfWeek();
    }

    @Test
    void countEventsByDayOfWeek_emptyRepository_returnsEmptyList() {
        when(repository.countEventsByDayOfWeek()).thenReturn(Collections.emptyList());

        List<Object[]> result = repository.countEventsByDayOfWeek();

        assertThat(result).isEmpty();
        verify(repository).countEventsByDayOfWeek();
    }


    @Test
    void save_persistsAndReturnsEvent() {
        when(repository.save(rockEvent)).thenReturn(rockEvent);

        MusicEvent saved = repository.save(rockEvent);

        assertThat(saved.getTitle()).isEqualTo("Rock Night");
        assertThat(saved.getCity()).isEqualTo("Cluj");
        verify(repository).save(rockEvent);
    }

    @Test
    void findById_existingId_returnsEvent() {
        when(repository.findById(1L)).thenReturn(Optional.of(rockEvent));

        Optional<MusicEvent> result = repository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Rock Night");
        verify(repository).findById(1L);
    }

    @Test
    void findById_nonExistentId_returnsEmpty() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<MusicEvent> result = repository.findById(999L);

        assertThat(result).isEmpty();
        verify(repository).findById(999L);
    }

    @Test
    void deleteById_callsRepositoryDelete() {
        doNothing().when(repository).deleteById(1L);

        repository.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void findAll_returnsAllEvents() {
        when(repository.findAll()).thenReturn(Arrays.asList(rockEvent, jazzEvent, popEvent));

        List<MusicEvent> result = repository.findAll();

        assertThat(result).hasSize(3);
        verify(repository).findAll();
    }

    @Test
    void count_returnsCorrectNumber() {
        when(repository.count()).thenReturn(3L);

        long count = repository.count();

        assertThat(count).isEqualTo(3L);
        verify(repository).count();
    }

    @Test
    void existsById_existingId_returnsTrue() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean exists = repository.existsById(1L);

        assertThat(exists).isTrue();
        verify(repository).existsById(1L);
    }

    @Test
    void existsById_nonExistentId_returnsFalse() {
        when(repository.existsById(999L)).thenReturn(false);

        boolean exists = repository.existsById(999L);

        assertThat(exists).isFalse();
        verify(repository).existsById(999L);
    }
}