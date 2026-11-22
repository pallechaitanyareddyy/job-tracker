package com.chaitu.jobtracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobTrackerApplication {
    private static final Logger logger = LoggerFactory.getLogger(JobTrackerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JobTrackerApplication.class, args);
        System.out.println("Job Tracker API is running live on GitHub...");
        logger.info("Job Tracker API is running live on GitHub...");
    }
}
