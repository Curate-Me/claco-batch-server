package com.curateme.clacobatchserver.service;

import com.curateme.clacobatchserver.entity.ConcertEntity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.batch.item.ItemReader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class KopisConcertApiReader implements ItemReader<ConcertEntity> {

    private final RestTemplate restTemplate;
    private final String apiUrl = "http://www.kopis.or.kr/openApi/restful/pblprfr?service=f222668534db409b8769f640387de9c3";
    private int currentPage = 1;
    private List<ConcertEntity> beforeEntities = new ArrayList<>();
    private int index = 0;

    public KopisConcertApiReader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ConcertEntity read() throws Exception {

        if (index >= beforeEntities.size()) {
            loadNextPage();
            index = 0;
        }

        if (beforeEntities.isEmpty()) {
            return null;
        }

        return beforeEntities.get(index++);
    }

    private void loadNextPage() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedStartDate = startDate.format(formatter);
        String formattedEndDate = endDate.format(formatter);

        String requestUrl = String.format("%s&stdate=%s&eddate=%s&rows=10&cpage=%d",
            apiUrl, formattedStartDate, formattedEndDate, currentPage++);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ConcertEntity[]> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, ConcertEntity[].class);
            System.out.println("response = " + response);
            if (response.getBody() != null && response.getBody().length > 0) {
                beforeEntities.clear();
                for (ConcertEntity beforeentity : response.getBody()) {
                    ConcertEntity concertEntity = new ConcertEntity();
                    concertEntity.setConcertDetails(
                        beforeentity.getMt20id(),
                        beforeentity.getPrfnm(),
                        beforeentity.getPrfpdfrom(),
                        beforeentity.getPrfpdto(),
                        beforeentity.getFcltynm(),
                        beforeentity.getPoster(),
                        beforeentity.getArea(),
                        beforeentity.getGenrenm(),
                        beforeentity.getOpenrun(),
                        beforeentity.getPrfstate()
                    );

                    beforeEntities.add(concertEntity);
                }
            } else {
                beforeEntities.clear();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            beforeEntities.clear();
        }
    }
}
