package com.example.application_service.client;

import com.example.application_service.dto.CollectionRequestDTO;
import com.example.application_service.dto.PaymentRequestDTO;
import com.example.application_service.dto.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "collection-service", url = "http://localhost:8084/api/collections")
public interface CollectionServiceClient {

    @PostMapping()
    PaymentResponseDTO processCollection(@RequestBody PaymentRequestDTO requestDTO);

    @PostMapping("/request")
    PaymentResponseDTO initiateCollection(@RequestBody CollectionRequestDTO collectionRequest);
}