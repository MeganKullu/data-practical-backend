package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobService {

    // Store job status in memory

    private final ConcurrentHashMap<String, String> jobStatus = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jobResult = new ConcurrentHashMap<>();

    public String createJob() {
        String jobId = UUID.randomUUID().toString();
        jobStatus.put(jobId, JobStatus.SUBMITTED.name());
        return jobId;
    }

    public void updateStatus(String jobId, JobStatus status, String result) {
        jobStatus.put(jobId, status.name());
        if (result != null ) {
            jobResult.put(jobId, result);
        }
    }

    public String getStatus(String jobId) {
        return jobResult.get(jobId);
    }

}
