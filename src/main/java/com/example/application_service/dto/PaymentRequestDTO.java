package com.example.application_service.dto;

import com.example.application_service.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private String cvcNo;

    @NotNull(message = "Taksit sayısı boş olamaz")
    @Positive(message = "Taksit sayısı 1 veya daha büyük olmalıdır")
    private Integer installmentCount;

    @JsonIgnore // JSON'da (Postman'de) gözükmez, dışarıdan alınmaz!
    private PaymentMethod paymentMethod;

    @JsonIgnore // JSON'da (Postman'de) gözükmez, dışarıdan alınmaz!
    private Long cardId;
}
