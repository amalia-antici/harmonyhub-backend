package com.amalia.harmonyhub_backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MusicEventTest {

    @Test
    void defaultConstructor_createsEmptyEvent() {
        MusicEvent event = new MusicEvent();
        assertThat(event.getId()).isNull();
        assertThat(event.getTitle()).isNull();
        assertThat(event.getReservedSpots()).isEqualTo(0);
    }

    @Test
    void constructor_withAllFields_setsCorrectly() {
        LocalDateTime dt = LocalDateTime.of(2025, 6, 1, 20, 0);
        MusicEvent event = new MusicEvent(
                1L, "Jazz Night", "Club A", "Cluj",
                MusicEvent.EventType.CONCERT, 50, 0,
                "A jazz evening", dt, "Romania",
                MusicEvent.Genre.JAZZ, "http://form.link", "/photo.jpg"
        );

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getTitle()).isEqualTo("Jazz Night");
        assertThat(event.getLocation()).isEqualTo("Club A");
        assertThat(event.getCity()).isEqualTo("Cluj");
        assertThat(event.getCapacity()).isEqualTo(50);
        assertThat(event.getReservedSpots()).isEqualTo(0);
        assertThat(event.getDescription()).isEqualTo("A jazz evening");
        assertThat(event.getDateTime()).isEqualTo(dt);
        assertThat(event.getGenre()).isEqualTo(MusicEvent.Genre.JAZZ);
        assertThat(event.getFormLink()).isEqualTo("http://form.link");
    }

    @Test
    void addTag_addsTagToList() {
        MusicEvent event = new MusicEvent();
        EventTag tag = new EventTag();
        tag.setName("Sold Out");

        event.addTag(tag);

        assertThat(event.getTags()).hasSize(1);
        assertThat(event.getTags().get(0).getName()).isEqualTo("Sold Out");
    }

    @Test
    void addTag_whenTagsNull_initializesListAndAdds() {
        MusicEvent event = new MusicEvent();
        event.setTags(null);
        EventTag tag = new EventTag();
        tag.setName("Live");

        event.addTag(tag);

        assertThat(event.getTags()).isNotNull();
        assertThat(event.getTags()).hasSize(1);
    }

    @Test
    void setTags_replacesExistingTags() {
        MusicEvent event = new MusicEvent();
        EventTag tag1 = new EventTag();
        tag1.setName("Tag1");
        event.addTag(tag1);

        List<EventTag> newTags = new ArrayList<>();
        EventTag tag2 = new EventTag();
        tag2.setName("Tag2");
        newTags.add(tag2);

        event.setTags(newTags);

        assertThat(event.getTags()).hasSize(1);
        assertThat(event.getTags().get(0).getName()).isEqualTo("Tag2");
    }

    @Test
    void setCreatedBy_linksUserToEvent() {
        MusicEvent event = new MusicEvent();
        User user = new User();
        user.setUsername("testuser");

        event.setCreatedBy(user);

        assertThat(event.getCreatedBy()).isNotNull();
        assertThat(event.getCreatedBy().getUsername()).isEqualTo("testuser");
    }

    @Test
    void allGenreValues_areValid() {
        assertThat(MusicEvent.Genre.values()).contains(
                MusicEvent.Genre.ROCK, MusicEvent.Genre.JAZZ, MusicEvent.Genre.POP,
                MusicEvent.Genre.COUNTRY, MusicEvent.Genre.HIP_HOP, MusicEvent.Genre.CLASSIC,
                MusicEvent.Genre.REGGAE, MusicEvent.Genre.METAL, MusicEvent.Genre.SACRED,
                MusicEvent.Genre.ALL
        );
    }

    @Test
    void allEventTypeValues_areValid() {
        assertThat(MusicEvent.EventType.values()).contains(
                MusicEvent.EventType.CONCERT, MusicEvent.EventType.KARAOKE,
                MusicEvent.EventType.JAM_SESSION, MusicEvent.EventType.WORKSHOP,
                MusicEvent.EventType.OPEN_MIC, MusicEvent.EventType.CASTING,
                MusicEvent.EventType.COMPETITION, MusicEvent.EventType.MEET_THE_ARTIST,
                MusicEvent.EventType.RECITAL, MusicEvent.EventType.OTHER
        );
    }

    @Test
    void setDateTime_storesCorrectValue() {
        MusicEvent event = new MusicEvent();
        LocalDateTime dt = LocalDateTime.of(2025, 12, 25, 18, 0);
        event.setDateTime(dt);
        assertThat(event.getDateTime()).isEqualTo(dt);
    }

    @Test
    void setReservedSpots_updatesCorrectly() {
        MusicEvent event = new MusicEvent();
        event.setReservedSpots(42);
        assertThat(event.getReservedSpots()).isEqualTo(42);
    }
}