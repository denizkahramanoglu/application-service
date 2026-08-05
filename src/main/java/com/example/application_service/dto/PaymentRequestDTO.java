package com.example.application_service.dto;

import com.example.application_service.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Builder
@Getter
@Setter
public class PaymentRequestDTO {

    @NotNull(message = "Application ID boş olamaz")
    private Long applicationId;

    @NotNull(message = "Ödeme yöntemi seçilmelidir")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Taksit sayısı boş olamaz")
    @Positive(message = "Taksit sayısı 1 veya daha büyük olmalıdır")
    private Integer installmentCount;

    private Long cardId;
    private String cvcNo;

}