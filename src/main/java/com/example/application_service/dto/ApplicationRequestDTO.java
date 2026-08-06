package com.example.application_service.dto;

import com.example.application_service.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequestDTO {
    private Long customerId;
    private Long productId;
    private double height;
    private double weight;
    private Long occupationId;
    private boolean smoker;
    private List<Long> personalDiseaseIds;

    @NotNull(message = "Ödeme yöntemi seçilmelidir")
    private PaymentMethod paymentMethod;

    private Long cardId;
}

