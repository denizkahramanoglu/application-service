package com.example.application_service.client;

import com.example.application_service.dto.InsuranceProductRequestDTO;
import com.example.application_service.dto.InsuranceProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-client", url = "http://localhost:8082")
public interface ProductClient {

    @PostMapping("/api/products")
    InsuranceProductResponseDTO createProduct(@RequestBody InsuranceProductRequestDTO requestDto);

    @GetMapping("/api/products/{id}")
    InsuranceProductResponseDTO getProduct(@PathVariable("id") Long id);
}