package com.example.application_service.dto;

import lombok.Data;

@Data
public class LifePremiumRequestDTO {
    // Temel Bilgiler
    private int age;
    private String gender;

    // Sağlık ve Fiziksel Bilgiler
    private double heightInMeters;
    private double weightInKg;
    private boolean isSmoker;
    private boolean hasPriorSurgery;
    private boolean hasChronicDisease;
    private boolean hasFamilyHistoryOfCriticalIllness;

    // Meslek Bilgisi
    private Long occupationId;

    private String requestedCurrency;
    private long productId;
}