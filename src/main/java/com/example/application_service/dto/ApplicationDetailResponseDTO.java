package com.example.application_service.dto;

import com.example.application_service.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDetailResponseDTO {

    private Long applicationId;
    private BigDecimal price;
    private String currency;
    private LocalDateTime createdAt;
    private PaymentMethod paymentMethod;
    private Integer installmentCount;
    private Long cardId;
    private CustomerResponseDTO customer;
    private InsuranceProductResponseDTO product;
}