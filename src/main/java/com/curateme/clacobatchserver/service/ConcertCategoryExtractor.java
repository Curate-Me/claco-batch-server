package com.curateme.clacobatchserver.service;

import com.curateme.clacobatchserver.entity.Concert;
import com.curateme.clacobatchserver.repository.ConcertRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ConcertCategoryExtractor {

    private final RestTemplate restTemplate;
    private final ConcertRepository concertRepository;

    @Autowired
    public ConcertCategoryExtractor(RestTemplate restTemplate, ConcertRepository concertRepository) {
        this.restTemplate = restTemplate;
        this.concertRepository = concertRepository;
    }

    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Concert> concerts = concertRepository.findAll();

        for (Concert concert : concerts) {
            try {
                String introduction = concert.getStyurl();

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("image_url", introduction);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

                // Flask 서버에 요청 보내기
                ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:8081/categories", requestEntity, Map.class);

                if (response.getBody() != null) {
                    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                    Object clovaResponse = responseBody.get("clova_response");

                    Map<String, Double> categories = new HashMap<>();

                    // clova_response 처리
                    if (clovaResponse instanceof Map) {
                        // clova_response가 Map인 경우
                        Map<String, Object> categoryData = (Map<String, Object>) clovaResponse;
                        String koreanCategory = categoryData.get("name").toString();
                        Double score = Double.parseDouble(categoryData.get("score").toString());

                        categories.put(koreanCategory, score);
                    } else if (clovaResponse instanceof List) {
                        // clova_response가 List인 경우
                        List<Map<String, Object>> categoryList = (List<Map<String, Object>>) clovaResponse;

                        for (Map<String, Object> categoryData : categoryList) {
                            String koreanCategory = categoryData.get("name").toString();
                            Double score = Double.parseDouble(categoryData.get("score").toString());

                            categories.put(koreanCategory, score);
                        }
                    }

                    concert.setCategories(categories);

                    // 업데이트된 concert 엔티티를 저장
                    concertRepository.save(concert);
                }
            } catch (Exception e) {
                // 특정 concert에서 예외 발생 시 로그를 남기고, 다음 concert로 계속 진행
                System.err.println("concert ID " + concert.getMt20id() + " 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return RepeatStatus.FINISHED;
    }


}
