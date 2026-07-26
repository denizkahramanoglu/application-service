package com.example.application_service.dto;

import lombok.Data;


@Data
public class LifePremiumResponseDTO {
    private Long calculatedPremium;
    private String currency;
}