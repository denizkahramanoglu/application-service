package com.example.application_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerCardResponseDTO {

    private Long id;
    private String cardAlias;
    private String cardNumber;
    private Integer expireMonth;
    private Integer expireYear;
}