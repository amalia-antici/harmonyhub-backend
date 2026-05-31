package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.SuspiciousUser;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.utils.OptionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiMonitorService {

    private final OllamaAPI ollamaAPI;

    public AiMonitorService() {
        this.ollamaAPI = new OllamaAPI("http://localhost:11434");
        this.ollamaAPI.setRequestTimeoutSeconds(60);
    }

    public String analyzeThreats(List<SuspiciousUser> observations) {
        if (observations.isEmpty()) {
            return "No suspicious activity detected.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Security log summary:\n");
        observations.stream().limit(10).forEach(o -> {
            summary.append(String.format("- User %s: %s (Severity: %s) at %s\n",
                    o.getUserId(), o.getReason(), o.getSeverity(), o.getTimestamp()));
        });

        String prompt = String.format(
                "You are a cybersecurity analyst. Analyze this security log and give a brief threat assessment in 3 sentences max:\n\n%s",
                summary
        );

        try {
            io.github.ollama4j.models.OllamaResult result = ollamaAPI.generate("tinyllama", prompt,
                    false, new OptionsBuilder().build());
            return result.getResponse();
        } catch (Exception e) {
            return "AI analysis unavailable: " + e.getMessage();
        }
    }
}
