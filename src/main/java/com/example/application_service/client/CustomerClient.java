package com.example.application_service.client;

import com.example.application_service.dto.CustomerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-client", url = "http://localhost:8080")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}")
    CustomerResponseDTO getCustomerById(@PathVariable("id") Long id);


}