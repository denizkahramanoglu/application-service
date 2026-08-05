package com.example.application_service.dto;

import com.example.application_service.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionRequestDTO {

    private Long applicationId;
    private Long customerId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private Integer installmentCount;
    private Long cardId;
    private String cvcNo;
    
    // Application-service'ten gelen müşteri bilgileri
    private CustomerResponseDTO customer;
    
    // Müşterinin kartları (kredi kartı ödeme için gerekli)
    private List<CustomerCardResponseDTO> cards;
    
    // Sigorta ürün bilgileri (Iyzico ödemesi için gerekli)
    private InsuranceProductResponseDTO product;
}


