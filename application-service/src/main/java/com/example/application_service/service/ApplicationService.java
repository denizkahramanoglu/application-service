package com.example.application_service.service;

import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.dto.InsuranceProductResponseDTO;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ProductClient productClient;
    private final ApplicationRepository applicationRepository;

    public void createApplication(ApplicationRequestDTO request) {

        // 1. Feign ile Product Service'e istek at (8081 veya 8082'ye)
        ResponseEntity<InsuranceProductResponseDTO> productResponse = productClient.getProduct(request.getProductId());

        // 2. Dönen veriyi kontrol et (eğer response başarılı değilse hata fırlatabilirsin)
        InsuranceProductResponseDTO product = productResponse.getBody();

        if (product == null) {
            throw new IllegalArgumentException("Ürün bulunamadı, başvuru oluşturulamaz.");
        }

        // 3. Application nesnesini oluştur (Product'tan gelen fiyat + Customer'dan gelen ID)
        ApplicationEntity application = ApplicationEntity.builder()
                .customerId(request.getCustomerId()) // İstekten geliyor
                .productId(product.getProductId())          // Product Service'ten geliyor
                .price(product.getPrice())           // Product Service'ten geliyor
                .build();

        // 4. Veritabanına kaydet
        applicationRepository.save(application);
    }
}