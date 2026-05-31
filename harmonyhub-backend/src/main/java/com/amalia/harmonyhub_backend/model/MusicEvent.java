package com.amalia.harmonyhub_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="music_events")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MusicEvent {

    public enum EventType {
        KARAOKE, OPEN_MIC, CASTING, COMPETITION, WORKSHOP,
        MEET_THE_ARTIST, JAM_SESSION, RECITAL, CONCERT, OTHER
    }

    public enum Genre {
        ROCK, JAZZ, POP, COUNTRY, HIP_HOP, CLASSIC, REGGAE, METAL, SACRED, ALL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    @JsonAlias({"Id", "id"})
    private Long id;

    @Column(nullable = false, name="title")
    @JsonProperty("title")
    @JsonAlias({"Title", "title"})
    private String title;

    @Column(nullable = false, name="location")
    @JsonProperty("location")
    @JsonAlias({"Location", "location"})
    private String location;

    @Column(nullable = false, name="city")
    @JsonProperty("city")
    @JsonAlias({"City", "city"})
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name="event_type")
    @JsonProperty("event_type")
    @JsonAlias({"EventType", "event_type"})
    private EventType eventType;

    @Column(name="capacity")
    @JsonProperty("capacity")
    @JsonAlias({"Capacity", "capacity"})
    private Integer capacity;

    @Column(name="reserved_spots")
    @JsonProperty("reserved_spots")
    @JsonAlias({"ReservedSpots", "reserved_spots"})
    private Integer reservedSpots = 0;

    @Column(columnDefinition = "TEXT", name="description")
    @JsonProperty("description")
    @JsonAlias({"Description", "description"})
    private String description;

    @Column(name="date_time")
    @JsonProperty("date_time")
    @JsonAlias({"DateTime", "date_time"})
    private LocalDateTime dateTime;

    @Column(columnDefinition = "TEXT", name="country")
    @JsonProperty("country")
    @JsonAlias({"Country", "country"})
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name="genre")
    @JsonProperty("genre")
    @JsonAlias({"Genre", "genre"})
    private Genre genre;

    @Column(name="form_link")
    @JsonProperty("form_link")
    @JsonAlias({"FormLink", "form_link"})
    private String formLink;

    @ManyToMany(cascade={CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name="event_tags_mapping",
            joinColumns=@JoinColumn(name="event_id"),
            inverseJoinColumns = @JoinColumn(name="tag_id")
    )
    @JsonAlias({"Tags", "tags"})
    private List<EventTag> tags = new ArrayList<>();

    @Column(name="photo_url", length = 500)
    @JsonProperty("photo_url")
    @JsonAlias({"PhotoUrl", "photo_url"})
    private String photoUrl = "com/amalia/harmonyhub_backend/eventPhotoPlaceholder.jpg";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="created_by_user_id")
    @JsonProperty("created_by_user_id")
    @JsonAlias({"CreatedBy", "created_by"})
    private User createdBy;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public MusicEvent() {}

    public MusicEvent(Long id, String title, String location, String city, EventType eventType, Integer capacity, Integer reservedSpots, String description, LocalDateTime dateTime, String country, Genre genre, String formLink, String photoUrl) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.city = city;
        this.eventType = eventType;
        this.capacity = capacity;
        this.reservedSpots = reservedSpots != null ? reservedSpots : 0;
        this.description = description;
        this.dateTime = dateTime;
        this.country = country;
        this.genre = genre;
        this.formLink = formLink;
        this.photoUrl = photoUrl;
    }

    public MusicEvent(Long id, String title, String location, String city, EventType eventType,
                      Integer capacity, LocalDateTime dateTime, String country, Genre genre) {
        this(id, title, location, city, eventType, capacity, 0, null, dateTime, country, genre, null, "com/amalia/harmonyhub_backend/eventPhotoPlaceholder.jpg");
    }

    public MusicEvent(String title, String location, String city, EventType eventType,
                      Integer capacity, LocalDateTime dateTime, String country, Genre genre) {
        this(null, title, location, city, eventType, capacity, 0, null, dateTime, country, genre, null, "com/amalia/harmonyhub_backend/eventPhotoPlaceholder.jpg");
    }


    public void addTag(EventTag tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getReservedSpots() { return reservedSpots; }
    public void setReservedSpots(Integer reservedSpots) { this.reservedSpots = reservedSpots; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    public String getFormLink() { return formLink; }
    public void setFormLink(String formLink) { this.formLink = formLink; }

    public List<EventTag> getTags() { return tags; }
    public void setTags(List<EventTag> tags) { this.tags = tags; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}