package com.curateme.clacobatchserver.repository;

import com.curateme.clacobatchserver.entity.Concert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
    @Query("SELECT c FROM Concert c LEFT JOIN FETCH c.categories")
    List<Concert> getAllConcertsWithCategories();

    @Query("SELECT c.id FROM Concert c")
    List<Long> findAllConcertIds();
}
