package com.curateme.clacobatchserver.service;

import com.curateme.clacobatchserver.entity.Concert;
import com.curateme.clacobatchserver.repository.ConcertRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class KopisEntityWriter implements ItemWriter<Concert> {

    private final ConcertRepository concertRepository;

    public KopisEntityWriter(ConcertRepository concertRepository) {

        this.concertRepository = concertRepository;
    }

    @Override
    public void write(Chunk<? extends Concert> items) throws Exception {

        concertRepository.saveAll(items.getItems());
    }


}