package com.rag.assistant.service; 

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private PgVectorStore vectorStore;

    private ChatClient chatClient;
    private ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();

        public ChatService(ChatClient.Builder builder  ) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String getBotResponse(String query) throws IOException{
       
        String promptStringTemplate = Files.readString(
            resourceLoader.getResource("classpath:prompt/assistant-prompt.st")
                .getFile()
                .toPath()
        );
        
        String context = getSemanticContext(query);

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(promptStringTemplate)
                .variables(Map.of("query", query, "context", context))
                .build();

        return chatClient.prompt(promptTemplate.create()).call().content();

    }

    private String getSemanticContext(String query) {

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .similarityThreshold(0.6f)
                        .build()
        );
        StringBuilder context = new StringBuilder();
        for (Document document : documents) {
            context.append(document.getFormattedContent()).append("\n");
        }
        return context.toString();
    }


    public void addDocument(String content) {

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(new Document(content));
        vectorStore.add(chunks);

    }
}