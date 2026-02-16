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
        private int progress;        // 0-100 percentage
        private long processedCount; // Records processed so far
        private long totalCount;     // Total records to process
    }

    public String createJob() {
        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new JobInfo(JobStatus.SUBMITTED, null, 0, 0, 0));
        return jobId;
    }

    public void updateStatus(String jobId, JobStatus status, String result) {
        JobInfo existing = jobs.get(jobId);
        if (existing != null) {
            existing.setStatus(status);
            existing.setResult(result);
            if (status == JobStatus.COMPLETED) {
                existing.setProgress(100);
            }
        } else {
            jobs.put(jobId, new JobInfo(status, result, status == JobStatus.COMPLETED ? 100 : 0, 0, 0));
        }
    }

    public void updateProgress(String jobId, long processedCount, long totalCount) {
        JobInfo jobInfo = jobs.get(jobId);
        if (jobInfo != null) {
            jobInfo.setProcessedCount(processedCount);
            jobInfo.setTotalCount(totalCount);
            if (totalCount > 0) {
                jobInfo.setProgress((int) ((processedCount * 100) / totalCount));
            }
        }
    }

    public JobInfo getJob(String jobId) {
        return jobs.get(jobId);
    }
}
