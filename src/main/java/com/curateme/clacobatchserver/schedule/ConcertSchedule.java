package com.curateme.clacobatchserver.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class ConcertSchedule {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    public ConcertSchedule(JobLauncher jobLauncher, JobRegistry jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @Scheduled(cron = "10 * * * * *", zone = "Asia/Seoul")
    //@Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul") -> 추후 한달 단위로 수정(매달 1일)
    public void runFirstJob() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = dateFormat.format(new Date());

        JobParameters jobParameters = new JobParametersBuilder()
            .addString("date", date)
            .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("kopisJob"), jobParameters);
    }
}