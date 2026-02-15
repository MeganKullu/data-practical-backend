package com.megan.dataproject.service;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileStorageService {
    private final String BASE_PATH = "C:\\var\\log\\applications\\API\\dataprocessing\\";

    @PostConstruct
    public void init() {
        File dir = new File(BASE_PATH);

        if (!dir.exists()) {
            dir.mkdirs(); //Creates the C:\var\log automatically
        }
    }

    public String getPath(String fileName) {
        return BASE_PATH + fileName;
    }


}
