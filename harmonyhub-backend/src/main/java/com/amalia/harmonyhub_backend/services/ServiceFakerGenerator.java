package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.MusicEvent;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ServiceFakerGenerator {

    @Autowired
    private ServiceMusicEvents service;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Faker faker = new Faker();
    private Thread generatorThread;

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            generatorThread = new Thread(this::generateEvents, "faker-generator-thread");
            generatorThread.setDaemon(true);
            generatorThread.start();
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (generatorThread != null) {
                generatorThread.interrupt();
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    @Async
    public void generateEvents() {
        while (isRunning.get()) {
            try {
                MusicEvent fakeEvent = new MusicEvent();
                fakeEvent.setTitle(faker.music().genre() + " session");
                fakeEvent.setLocation(faker.address().streetAddress());
                fakeEvent.setCity(faker.address().city());
                fakeEvent.setCountry("Romania");
                fakeEvent.setEventType(MusicEvent.EventType.JAM_SESSION);
                fakeEvent.setGenre(MusicEvent.Genre.ALL);
                fakeEvent.setCapacity(faker.number().numberBetween(20, 300));
                fakeEvent.setDescription(faker.lorem().sentence());
                fakeEvent.setDateTime(LocalDateTime.now().plusDays(faker.number().numberBetween(1, 30)));
                fakeEvent.setPhotoUrl("https://picsum.photos/seed/" + faker.random().nextInt() + "/400/400");

                service.addEvent(fakeEvent);
                messagingTemplate.convertAndSend("/topic/updates", "New event added");

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}