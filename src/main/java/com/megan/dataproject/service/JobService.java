package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobService {

    private final ConcurrentHashMap<String, JobInfo> jobs = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobInfo {
        private JobStatus status;
        private String result;
    }

    public String createJob() {
        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new JobInfo(JobStatus.SUBMITTED, null));
        return jobId;
    }

    public void updateStatus(String jobId, JobStatus status, String result) {
        jobs.put(jobId, new JobInfo(status, result));
    }

    public JobInfo getJob(String jobId) {
        return jobs.get(jobId);
    }
}
