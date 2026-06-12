package com.amalia.harmonyhub_backend.utils;

import com.amalia.harmonyhub_backend.model.QuizQuestion;
import com.amalia.harmonyhub_backend.model.QuoteType;
import com.amalia.harmonyhub_backend.repository.QuizQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizDataSeeder implements CommandLineRunner {

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        if (questionRepository.count() > 0) return;

        List<QuizQuestion> questions = List.of(
                q("You, with your words like knives and swords and weapons that you use against me.", "Taylor Swift", QuoteType.SONGWRITER, ""),
                q("I was not born under a rhyming planet", "William Shakespeare", QuoteType.SHAKESPEARE,""),
                q("Maybe it's hatred I spew, maybe it's food for the spirit.", "Eminem", QuoteType.SONGWRITER, ""),
                q("Those Windermere peaks look like a perfect place to cry. I'm setting off, but not without my muse.", "Taylor Swift", QuoteType.SONGWRITER, ""),
                q("My crown is in my heart, not on my head.", "William Shakespeare", QuoteType.SHAKESPEARE, ""),
                q("I grew a flower that can't be bloomed, in a dream that can't come true.", "BTS", QuoteType.SHAKESPEARE, ""),
                q("By the pricking of my thumbs, something wicked this way comes.", "William Shakespeare", QuoteType.SHAKESPEARE, ""),
                q("They say the tongue is the hardest stone to break.", "Zayn", QuoteType.SONGWRITER, ""),
                q("Comfortable silence is so overstated.", "Harry Styles", QuoteType.SONGWRITER,""),
                q("There's no language in her eye, her cheek, her lip.", "William Shakespeare", QuoteType.SHAKESPEARE, "")
        );

        questionRepository.saveAll(questions);
        System.out.println("Quiz questions seeded: " + questions.size());
    }

    private QuizQuestion q(String text, String author, QuoteType type, String hint) {
        QuizQuestion q = new QuizQuestion();
        q.setText(text);
        q.setAuthor(author);
        q.setCorrectAnswer(type);
        q.setHint(hint);
        return q;
    }
}
