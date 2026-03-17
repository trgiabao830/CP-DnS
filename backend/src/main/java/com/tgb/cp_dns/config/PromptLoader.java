package com.tgb.cp_dns.config;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Getter
public class PromptLoader {

    private final String systemInstruction;

    public PromptLoader() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system_instruction.txt");
            byte[] data = resource.getInputStream().readAllBytes();
            systemInstruction = new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Không thể load system prompt", e);
        }
    }
}