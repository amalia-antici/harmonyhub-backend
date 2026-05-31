package com.amalia.harmonyhub_backend.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EventTagTest {

    @Test
    void defaultConstructor_createsEmptyTag() {
        EventTag tag = new EventTag();
        assertThat(tag.getId()).isNull();
        assertThat(tag.getName()).isNull();
    }

    @Test
    void setName_withSpecialCharacters_storesCorrectly() {
        EventTag tag = new EventTag();
        tag.setName("18+Only");
        assertThat(tag.getName()).isEqualTo("18+Only");
    }

    @Test
    void setName_withSpaces_storesCorrectly() {
        EventTag tag = new EventTag();
        tag.setName("Free Entry");
        assertThat(tag.getName()).isEqualTo("Free Entry");
    }

    @Test
    void setId_storesCorrectValue() {
        EventTag tag = new EventTag();
        tag.setId(5L);
        assertThat(tag.getId()).isEqualTo(5L);
    }

    @Test
    void setName_null_storesNull() {
        EventTag tag = new EventTag();
        tag.setName(null);
        assertThat(tag.getName()).isNull();
    }
}