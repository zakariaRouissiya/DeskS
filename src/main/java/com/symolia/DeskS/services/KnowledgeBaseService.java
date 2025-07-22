package com.symolia.DeskS.services;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final VectorStore vectorStore;

    public void ingestDocument(String content) {
        String title = extractTitle(content);

        Document doc = new Document(
            content,
            Map.of(
                "title", title != null ? title : "Procédure sans titre"
            )
        );
        vectorStore.add(List.of(doc));
    }

    private String extractTitle(String content) {
        for (String line : content.split("\n")) {
            if (line.toLowerCase().startsWith("titre du ticket")) {
                return line.replace("Titre du ticket :", "").trim();
            }
        }
        return null;
    }
}