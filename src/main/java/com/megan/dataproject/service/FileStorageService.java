package com.megan.dataproject.service;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {
    private final String BASE_PATH;

    public FileStorageService() {
        // Use platform-appropriate path
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            BASE_PATH = "C:\\var\\log\\applications\\API\\dataprocessing";
        } else {
            // Linux/Unix (Railway, Docker, etc.)
            BASE_PATH = "/var/log/applications/API/dataprocessing";
        }
    }

    @PostConstruct
    public void init() {
        File dir = new File(BASE_PATH);

        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public String getPath(String fileName) {
        return Paths.get(BASE_PATH, fileName).toString();
    }
}
