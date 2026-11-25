

package com.rag.assistant.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerErrorException;



@Service
@Slf4j
public class ContentAnalyse   {

    private final Tika tika;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit

    public ContentAnalyse() {
        this.tika = new Tika();
    }

    
    public void validateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new Exception("File size exceeds 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("application/") && !contentType.startsWith("text/")) {
            throw new Exception("Invalid file type.  Only document types are supported.");
        }
    }

    
    public String getContent(InputStream inputStream) throws Exception {
        try {
            return tika.parseToString(inputStream);
        } catch (IOException | TikaException e) {
            log.error("Error extracting text: {}", e.getMessage());
            throw new Exception("Failed to extract text from document");
        }
    }


}