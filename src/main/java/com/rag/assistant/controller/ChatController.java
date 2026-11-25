package com.rag.assistant.controller; 

import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerErrorException;

import com.rag.assistant.service.ChatService;
import com.rag.assistant.service.ContentAnalyse;


import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Slf4j 
public class ChatController {


    @Autowired
    private ChatService chatService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ContentAnalyse contentAnalyse;


    @GetMapping("/api/{message}")
    public ResponseEntity<String> getAnswer(@PathVariable String message) throws IOException {

        String response = chatService.getBotResponse(message);
        log.info("User message text: {}", message);
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping(value = "/api/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadDocument(@RequestParam(name = "file", required = true) MultipartFile file)
            throws ServerErrorException, Exception {

        contentAnalyse.validateFile(file);
        var content = contentAnalyse.getContent(file.getInputStream());
        chatService.addDocument(content);
        return ResponseEntity.ok("  File uploaded and processed successfully.");
    }

    
    @GetMapping("/api/similaritySearch/{query}")
    public ResponseEntity<List<Document>> query(@PathVariable String query) {

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(5).build());
        return ResponseEntity.ok(documents);

    }
    
}