package com.curateme.clacobatchserver.batch;

import com.curateme.clacobatchserver.config.s3.S3Service;
import com.curateme.clacobatchserver.dto.CategoryScoreDto;
import com.curateme.clacobatchserver.entity.Concert;
import com.curateme.clacobatchserver.repository.ConcertCategoryRepository;
import com.curateme.clacobatchserver.repository.ConcertRepository;
import com.curateme.clacobatchserver.service.ConcertCategoryExtractor;
import com.curateme.clacobatchserver.service.ConcertSummaryExtractor;
import com.curateme.clacobatchserver.service.KopisConcertApiReader;
import com.curateme.clacobatchserver.service.KopisDetailApiReader;
import com.curateme.clacobatchserver.service.KopisEntityWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.config.Task;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ConcertBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final ConcertRepository concertRepository;
    private final ConcertCategoryRepository concertCategoryRepository;

    private final KopisConcertApiReader kopisApiReader;
    private final KopisDetailApiReader kopisDetailApiReader;
    private final ConcertCategoryExtractor concertCategoryExtractor;
    private final S3Service s3Service;
    private final ConcertSummaryExtractor concertSummaryExtractor;

    public ConcertBatch(JobRepository jobRepository,
        PlatformTransactionManager platformTransactionManager, ConcertRepository concertRepository,
        ConcertCategoryRepository concertCategoryRepository,
        KopisConcertApiReader kopisApiReader,
        KopisDetailApiReader kopisDetailApiReader,
        ConcertCategoryExtractor concertCategoryExtractor,
        S3Service s3Service,
        ConcertSummaryExtractor concertSummaryExtractor) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
        this.concertRepository = concertRepository;
        this.concertCategoryRepository = concertCategoryRepository;
        this.kopisApiReader = kopisApiReader;
        this.kopisDetailApiReader = kopisDetailApiReader;
        this.concertCategoryExtractor = concertCategoryExtractor;
        this.s3Service = s3Service;
        this.concertSummaryExtractor = concertSummaryExtractor;
    }

    @Bean
    public Job kopisJob(KopisEntityWriter writer) {
        return new JobBuilder("kopisJob", jobRepository)
            .start(firstStep(writer))
            .next(secondStep())
            .next(thirdStep())
            .next(fourthStep())
            .next(fifthStep())
            .build();
    }

    // 1. Kopis에서 해당 기간에 대한 공연 정보 가져 오기
    @Bean
    public Step firstStep(KopisEntityWriter writer) {
        return new StepBuilder("firstStep", jobRepository)
            .<Concert, Concert>chunk(10, platformTransactionManager)
            .reader(kopisApiReader)
            .writer(writer)
            .build();
    }

    // 2. Step1 에서 가져온 공연에 대해 상세 내역 가져 오기
    @Bean
    public Step secondStep() {
        return new StepBuilder("secondStep", jobRepository)
            .tasklet(kopisDetailApiReader, platformTransactionManager)
            .build();
    }

    // 3. 공연 포스터 이미지를 통해서 Flask 서버로 부터 카테고리 추출
    @Bean
    public Step thirdStep() {
        return new StepBuilder("thirdStep", jobRepository)
            .tasklet(concertCategoryExtractorTasklet(), platformTransactionManager)
            .build();
    }

    // 4. 추출한 카테고리 값을 CSV 파일에 저장
    @Bean
    public Step fourthStep() {
        return new StepBuilder("fourthStep", jobRepository)
            .tasklet((StepContribution contribution, ChunkContext chunkContext) -> {

                String folderPath = "datasets";
                String fileName = "concerts.csv";

                String localFilePath = s3Service.downloadCsvFile(folderPath, fileName);
                if (!isFileExists(localFilePath)) {
                    return RepeatStatus.FINISHED;
                }

                StringJoiner csvContent = readExistingCsvContent(localFilePath);
                appendNewConcertData(csvContent);

                saveAndUploadCsvFile(csvContent, folderPath, fileName);

                return RepeatStatus.FINISHED;
            }, platformTransactionManager).build();
    }

    // 5. 클라코 큐레이션: 공연 정보 요약
    @Bean
    public Step fifthStep() {
        return new StepBuilder("fifthStep", jobRepository)
            .tasklet(concertSummaryExtractorTasklet(), platformTransactionManager)
            .build();
    }

    // 5. 해당 Batch에서 가져온 값들을 전부 삭제하는 로직
    @Bean
    public Step finalStep() {
        return new StepBuilder("finalStep", jobRepository)
            .tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
                // 데이터베이스에서 ConcertEntity와 관련된 모든 데이터를 삭제
                deleteAllConcertData();
                return RepeatStatus.FINISHED;
            }, platformTransactionManager).build();
    }

    // ConcertEntity와 관련된 모든 데이터를 삭제하는 메서드
    private void deleteAllConcertData() {
        try {
            concertRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("ConcertEntity 데이터 삭제 오류: " + e.getMessage());
        }
    }


    // 파일 존재 확인 메서드
    private boolean isFileExists(String localFilePath) {
        File downloadedFile = new File(localFilePath);
        if (!downloadedFile.exists()) {
            System.err.println("파일이 존재하지 않습니다: " + localFilePath);
            return false;
        }
        System.out.println("다운로드된 파일 경로: " + localFilePath);
        return true;
    }

    // 기존 CSV 파일 내용을 읽는 메서드
    private StringJoiner readExistingCsvContent(String localFilePath) {
        StringJoiner csvContent = new StringJoiner("\n");
        try (BufferedReader reader = new BufferedReader(new FileReader(localFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                csvContent.add(line);
            }
        } catch (IOException e) {
            System.err.println("기존 CSV 파일 읽기 오류: " + e.getMessage());
        }
        return csvContent;
    }

    // 새로운 Concert 데이터를 CSV에 추가하는 메서드
    private void appendNewConcertData(StringJoiner csvContent) {
        List<String> columns = Arrays.asList("concertId", "grand", "delicate", "classical",
            "modern",
            "lyrical", "dynamic", "romantic", "tragic", "familiar", "novel");

        Map<String, String> translationMap = Map.of(
            "웅장한", "grand",
            "섬세한", "delicate",
            "고전적인", "classical",
            "현대적인", "modern",
            "서정적인", "lyrical",
            "역동적인", "dynamic",
            "낭만적인", "romantic",
            "비극적인", "tragic",
            "친숙한", "familiar",
            "새로운", "novel"
        );

        List<Long> concertIds = concertRepository.findAllConcertIds();

        for (Long concertId : concertIds) {
            // 각 Concert ID에 대해 category와 score 조회
            List<CategoryScoreDto> categoryScores = concertCategoryRepository.findByConcertId(
                concertId);
            System.out.println("categoryScores = " + categoryScores);
            // 각 카테고리의 기본 값을 0.0으로 초기화한 Map 생성
            Map<String, Double> categoryScoreMap = new HashMap<>();
            for (String column : columns.subList(1, columns.size())) {
                categoryScoreMap.put(column, 0.0);
            }

            // categoryScores에서 각 카테고리의 점수를 Map에 업데이트
            for (CategoryScoreDto categoryScore : categoryScores) {
                String koreanCategory = categoryScore.getCategory();
                System.out.println("koreanCategory = " + koreanCategory);
                String englishCategory = translationMap.get(koreanCategory);
                System.out.println("englishCategory = " + englishCategory);
                if (categoryScoreMap.containsKey(englishCategory)) {
                    categoryScoreMap.put(englishCategory, categoryScore.getScore());
                }
            }

            // CSV의 한 행을 구성하는 StringJoiner 생성
            StringJoiner rowContent = new StringJoiner(",");
            rowContent.add(String.valueOf(concertId)); // concertId 추가

            // 각 카테고리의 점수를 순서대로 추가
            for (String column : columns.subList(1, columns.size())) {
                rowContent.add(String.valueOf(categoryScoreMap.get(column)));
            }

            // 완성된 행을 csvContent에 추가
            csvContent.add(rowContent.toString());
        }
    }

    // 새로운 Concert 데이터 행을 초기화하는 메서드
    private Map<String, Object> initializeRowData(List<String> columns, Concert concert) {
        Map<String, Object> row = new HashMap<>();
        row.put("concertId", concert.getMt20id());

        for (String column : columns.subList(1, columns.size())) {
            row.put(column, 0.0);
        }

        if (concert.getCategories() != null) {
            for (Map.Entry<String, Double> entry : concert.getCategories().entrySet()) {
                if (columns.contains(entry.getKey())) {
                    row.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return row;
    }

    // 임시 파일에 저장하고 S3에 업로드하는 메서드
    private void saveAndUploadCsvFile(StringJoiner csvContent, String folderPath, String fileName) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("concerts_", ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(csvContent.toString());
            }

            s3Service.updateAndUploadCsvFile(folderPath, fileName, tempFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("파일 쓰기 오류: " + e.getMessage());
        } finally {
            // 임시 파일 정리
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Bean
    public Tasklet concertCategoryExtractorTasklet() {
        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
                throws Exception {
                return concertCategoryExtractor.execute(contribution, chunkContext);
            }
        };
    }

    @Bean
    public Tasklet concertSummaryExtractorTasklet() {
        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
                throws Exception {
                return concertSummaryExtractor.execute(contribution, chunkContext);
            }
        };
    }
}
