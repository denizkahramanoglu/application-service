package com.example.application_service.client;

import com.example.application_service.dto.InsuranceProductResponseDTO;
import com.example.application_service.dto.PricingRequestDTO;
import com.example.application_service.dto.PricingResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-client", url = "http://localhost:8082")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    InsuranceProductResponseDTO getProduct(@PathVariable("id") Long id);

    @PostMapping("/api/pricing/calculate")
    PricingResponseDTO calculatePrice(@RequestBody PricingRequestDTO request);
}