package com.curateme.clacobatchserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "concert_category")
public class ConcertCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "score")
    private Double score;

    @ManyToOne
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    public ConcertCategory(Category category, Double score, Concert concert) {
        this.category = category;
        this.score = score;
        this.concert = concert;
    }
}
