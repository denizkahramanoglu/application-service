package com.example.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PricingRequestDTO {
    private Long productId;
    private int age;
    private String gender;
    private double height;
    private double weight;
    private Long occupationId;
    private boolean smoker;
    private List<Long> personalDiseaseIds;
}