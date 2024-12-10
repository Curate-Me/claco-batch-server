package com.curateme.clacobatchserver.dto;

public class CategoryScoreDto {
    private String category;
    private Double score;

    public CategoryScoreDto(String category, Double score) {
        this.category = category;
        this.score = score;
    }

    public String getCategory() {
        return category;
    }

    public Double getScore() {
        return score;
    }
}