package com.example.application_service.controller;

import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<String> createApplication(@RequestBody ApplicationRequestDTO request) {
        applicationService.createApplication(request);
        return ResponseEntity.ok("Başvuru başarıyla oluşturuldu.");
    }

}
