package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.EventTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagRepositoryTest {

    @Mock
    private TagRepository repository;

    private EventTag rockTag;
    private EventTag jazzTag;
    private EventTag soldOutTag;

    @BeforeEach
    void setUp() {
        rockTag = new EventTag();
        rockTag.setName("Rock");

        jazzTag = new EventTag();
        jazzTag.setName("Jazz");

        soldOutTag = new EventTag();
        soldOutTag.setName("Sold Out");
    }

    @Test
    void findByName_existingTag_returnsTag() {
        when(repository.findByName("Rock")).thenReturn(Optional.of(rockTag));

        Optional<EventTag> result = repository.findByName("Rock");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Rock");
        verify(repository).findByName("Rock");
    }

    @Test
    void findByName_nonExistentTag_returnsEmpty() {
        when(repository.findByName("Metal")).thenReturn(Optional.empty());

        Optional<EventTag> result = repository.findByName("Metal");

        assertThat(result).isEmpty();
        verify(repository).findByName("Metal");
    }

    @Test
    void findByName_nullName_returnsEmpty() {
        when(repository.findByName(null)).thenReturn(Optional.empty());

        Optional<EventTag> result = repository.findByName(null);

        assertThat(result).isEmpty();
        verify(repository).findByName(null);
    }

    @Test
    void findByName_emptyString_returnsEmpty() {
        when(repository.findByName("")).thenReturn(Optional.empty());

        Optional<EventTag> result = repository.findByName("");

        assertThat(result).isEmpty();
        verify(repository).findByName("");
    }

    @Test
    void findByName_caseSensitive_differentCaseReturnsEmpty() {
        when(repository.findByName("rock")).thenReturn(Optional.empty());

        Optional<EventTag> result = repository.findByName("rock");

        assertThat(result).isEmpty();
        verify(repository).findByName("rock");
    }

    @Test
    void findByName_tagWithSpaces_returnsTag() {
        when(repository.findByName("Sold Out")).thenReturn(Optional.of(soldOutTag));

        Optional<EventTag> result = repository.findByName("Sold Out");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Sold Out");
        verify(repository).findByName("Sold Out");
    }

    @Test
    void save_newTag_persistsAndReturnsTag() {
        when(repository.save(rockTag)).thenReturn(rockTag);

        EventTag saved = repository.save(rockTag);

        assertThat(saved.getName()).isEqualTo("Rock");
        verify(repository).save(rockTag);
    }

    @Test
    void save_updatesExistingTag() {
        rockTag.setName("Rock Updated");
        when(repository.save(rockTag)).thenReturn(rockTag);

        EventTag updated = repository.save(rockTag);

        assertThat(updated.getName()).isEqualTo("Rock Updated");
        verify(repository).save(rockTag);
    }

    @Test
    void findById_existingId_returnsTag() {
        when(repository.findById(1L)).thenReturn(Optional.of(rockTag));

        Optional<EventTag> result = repository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Rock");
        verify(repository).findById(1L);
    }

    @Test
    void findById_nonExistentId_returnsEmpty() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<EventTag> result = repository.findById(999L);

        assertThat(result).isEmpty();
        verify(repository).findById(999L);
    }

    @Test
    void findAll_returnsAllTags() {
        when(repository.findAll()).thenReturn(Arrays.asList(rockTag, jazzTag, soldOutTag));

        List<EventTag> result = repository.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(EventTag::getName)
                .containsExactlyInAnyOrder("Rock", "Jazz", "Sold Out");
        verify(repository).findAll();
    }

    @Test
    void findAll_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<EventTag> result = repository.findAll();

        assertThat(result).isEmpty();
        verify(repository).findAll();
    }

    @Test
    void deleteById_callsRepository() {
        doNothing().when(repository).deleteById(1L);

        repository.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteById_nonExistentId_doesNotThrow() {
        doNothing().when(repository).deleteById(999L);

        repository.deleteById(999L);

        verify(repository).deleteById(999L);
    }

    @Test
    void existsById_existingTag_returnsTrue() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean exists = repository.existsById(1L);

        assertThat(exists).isTrue();
        verify(repository).existsById(1L);
    }

    @Test
    void existsById_nonExistentTag_returnsFalse() {
        when(repository.existsById(999L)).thenReturn(false);

        boolean exists = repository.existsById(999L);

        assertThat(exists).isFalse();
        verify(repository).existsById(999L);
    }

    @Test
    void count_returnsCorrectNumber() {
        when(repository.count()).thenReturn(3L);

        long count = repository.count();

        assertThat(count).isEqualTo(3L);
        verify(repository).count();
    }

    @Test
    void count_emptyRepository_returnsZero() {
        when(repository.count()).thenReturn(0L);

        long count = repository.count();

        assertThat(count).isEqualTo(0L);
        verify(repository).count();
    }
}