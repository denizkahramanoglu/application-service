package com.example.application_service.controller;

import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.dto.ApplicationResponseDTO;
import com.example.application_service.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<String> createApplication(@RequestBody ApplicationRequestDTO request) {
        applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Başvuru başarıyla oluşturuldu.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDTO> getApplication(@PathVariable Long id) {
        ApplicationResponseDTO response = applicationService.getApplication(id);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponseDTO> updateApplication(@PathVariable Long id, @RequestBody ApplicationRequestDTO request)
    {
        ApplicationResponseDTO response = applicationService.updateApplication(id, request);
        return ResponseEntity.ok(response);
    }
}

