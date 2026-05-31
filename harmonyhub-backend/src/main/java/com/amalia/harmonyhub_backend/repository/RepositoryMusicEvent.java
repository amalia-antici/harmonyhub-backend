package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.MusicEvent;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RepositoryMusicEvent {
    private final List<MusicEvent> events=new ArrayList<>();

    public RepositoryMusicEvent()
    {
        events.add(new MusicEvent(1L, "Jazz Night", "Yolka", "Cluj",
                MusicEvent.EventType.JAM_SESSION, 50, LocalDateTime.now().plusDays(2), "Romania", MusicEvent.Genre.JAZZ));

        events.add(new MusicEvent(2L, "Rock Blast", "Form Space", "Cluj",
                MusicEvent.EventType.CONCERT, 200, LocalDateTime.now().plusDays(5), "Romania", MusicEvent.Genre.ROCK));
    }

    public List<MusicEvent> findAll(){
        return events;
    }
    public MusicEvent save(MusicEvent event)
    {
        if(event.getId()==null)
        {
            event.setId((long)(events.size()+1));
        }
        events.add(event);
        return event;
    }

    public void deleteById(Long id)
    {
        events.removeIf(event-> event.getId().equals(id));
    }
    public Optional<MusicEvent> findById(Long id)
    {
        return events.stream().filter(event->event.getId().equals(id)).findFirst();
    }

    public List<MusicEvent> findPaginated(int page, int size)
    {
        int fromIndex=page*size;
        if (fromIndex>=events.size())
        {
            return new ArrayList<>();
        }
        return events.subList(fromIndex, Math.min(fromIndex+size, events.size()));
    }
}
