package com.symolia.DeskS.web;

import com.symolia.DeskS.services.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/ingest")
    public void ingest(@RequestBody String content) {
        knowledgeBaseService.ingestDocument(content);
    }
}