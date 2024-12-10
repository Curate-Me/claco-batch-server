package com.curateme.clacobatchserver.repository;

import com.curateme.clacobatchserver.dto.CategoryScoreDto;
import com.curateme.clacobatchserver.entity.ConcertCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConcertCategoryRepository extends JpaRepository<ConcertCategory, Long> {
    @Query("SELECT new com.curateme.clacobatchserver.dto.CategoryScoreDto(cc.category.category, cc.score) " +
        "FROM ConcertCategory cc WHERE cc.concert.id = :concertId")
    List<CategoryScoreDto> findByConcertId(@Param("concertId") Long concertId);

    @Query("SELECT cc FROM ConcertCategory cc WHERE cc.concert.id = :concertId")
    List<ConcertCategory> findAllByConcertId(@Param("concertId") Long concertId);

}


