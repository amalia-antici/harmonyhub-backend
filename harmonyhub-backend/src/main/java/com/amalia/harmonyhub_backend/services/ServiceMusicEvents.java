package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.EventTag;
import com.amalia.harmonyhub_backend.model.MusicEvent;
import com.amalia.harmonyhub_backend.repository.MusicEventRepository;
import com.amalia.harmonyhub_backend.repository.TagRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class ServiceMusicEvents {
    private final MusicEventRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TagRepository tagRepository;
    @Autowired private net.datafaker.Faker faker;

    public ServiceMusicEvents(MusicEventRepository repo, SimpMessagingTemplate messagingTemplate, TagRepository tagRepository)
    {
        this.repository=repo;
        this.messagingTemplate=messagingTemplate;
        this.tagRepository=tagRepository;
    }

    private void broadcastUpdate()
    {
        messagingTemplate.convertAndSend("/topic/events", repository.findAll());
    }

    public List<MusicEvent> getAllEvents()
    {
        return repository.findAll();
    }

    public List<MusicEvent> getPaginatedEvents(int page, int size)
    {
        if(page==-1)
            return repository.findAll();
        return repository.findAll(PageRequest.of(page, size)).getContent();
    }


    private List<EventTag> resolveTags(List<EventTag> incomingTags)
    {
        if (incomingTags == null)
            return new ArrayList<>();
        return incomingTags.stream()
                .filter(t -> t.getName() != null && !t.getName().isBlank())
                .map(t -> tagRepository.findByName(t.getName())
                        .orElseGet(() -> tagRepository.save(new EventTag(null, t.getName()))))
                .collect(Collectors.toList());
    }

    @Transactional
    public MusicEvent addEvent(MusicEvent event){
        event.setTags(resolveTags(event.getTags()));
        MusicEvent saved=repository.save(event);
        broadcastUpdate();
        return saved;
    }

    public Map<MusicEvent.Genre, Long> getEventsCountByGenre()
    {
        return repository.countEventsByGenre().stream().collect(Collectors.toMap(row-> (MusicEvent.Genre) row[0],
                row-> (Long) row[1]));
    }

    public Map<DayOfWeek, Long> getEventsCountByWeekDay()
    {
        Map<DayOfWeek, Long> counts=repository.findAll().stream().collect(Collectors.groupingBy(
                event->event.getDateTime().getDayOfWeek(),
                ()-> new EnumMap<>(DayOfWeek.class),
                Collectors.counting()
        ));
        for(DayOfWeek day: DayOfWeek. values()){
            counts.putIfAbsent(day, 0L);
        }

        return counts;
    }

    public Map<String, Long> getTagUsageStats(){
        return repository.findAll().stream().flatMap(event->event.getTags().stream()).collect(Collectors.groupingBy(tag->tag.getName(), Collectors.counting()));
    }

    @Transactional
    public boolean deleteEvent(Long id){
        if(repository.existsById(id))
        {
            repository.deleteById(id);
            broadcastUpdate();
            return true;
        }
        return false;
    }

    public MusicEvent updateEvent(Long id, MusicEvent updatedEvent) {
        return repository.findById(id).map(existingEvent->{
            updatedEvent.setId(id);
            updatedEvent.setTags(resolveTags(updatedEvent.getTags()));
            MusicEvent saved=repository.save(updatedEvent);
            broadcastUpdate();
            return saved;
        }).orElse(null);
    }

    public MusicEvent getEventById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Cacheable(value = "heavyTagStats", key = "'all'")
    public List<Map<String, Object>> getHeavyTagStats() {
        return repository.getHeavyTagStats();
    }

    @Transactional
    public Map<String, Long> seedWithFaker(int count) {
        List<String> tagNames = List.of("Sold Out", "Live", "Free Entry", "VIP", "Outdoor",
                "Acoustic", "Electric", "Family Friendly", "18+", "Festival");

        List<EventTag> tags = tagNames.stream().map(name ->
                tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new EventTag(name)))
        ).toList();

        for (int i = 0; i < count; i++) {
            MusicEvent event = new MusicEvent();
            event.setTitle(faker.music().instrument() + " " + faker.lorem().word());
            event.setLocation(faker.address().streetAddress());
            event.setCity(faker.address().city());
            event.setCountry(faker.address().country());
            event.setCapacity(faker.number().numberBetween(50, 5000));
            event.setDateTime(LocalDateTime.now().plusDays(faker.number().numberBetween(1, 365)));
            event.setGenre(MusicEvent.Genre.values()[faker.number().numberBetween(0, MusicEvent.Genre.values().length)]);
            event.setEventType(MusicEvent.EventType.values()[faker.number().numberBetween(0, MusicEvent.EventType.values().length)]);
            event.setDescription(faker.lorem().paragraph());

            List<EventTag> eventTags = new ArrayList<>();
            for (int t = 0; t < faker.number().numberBetween(2, 5); t++) {
                eventTags.add(tags.get(faker.number().numberBetween(0, tags.size())));
            }
            event.setTags(eventTags);
            repository.save(event);
        }

        return Map.of(
                "totalEvents", repository.count(),
                "totalTags", tagRepository.count()
        );
    }

    @CacheEvict(value = "heavyTagStats", allEntries = true)
    public void evictHeavyStatsCache() {}

    public List<MusicEvent> searchEvents(int page, int size, String genre, String city, String date) {
        return repository.searchEvents(genre, city, date, PageRequest.of(page, size));
    }

    @Transactional
    public MusicEvent toggleAttendance(Long eventId, boolean isAttending) {
        MusicEvent event = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

        int currentReserved = event.getReservedSpots() != null ? event.getReservedSpots() : 0;
        int capacity = event.getCapacity() != null ? event.getCapacity() : Integer.MAX_VALUE;

        if (isAttending) {
            if (currentReserved > 0) {
                event.setReservedSpots(currentReserved - 1);
            }
        } else {
            if (currentReserved >= capacity) {
                throw new IllegalStateException("Cannot attend: This event is already full!");
            }
            event.setReservedSpots(currentReserved + 1);
        }

        MusicEvent savedEvent = repository.save(event);
        broadcastUpdate();
        return savedEvent;
    }
}
