package com.example.application_service.controller;

import com.example.application_service.dto.ApplicationDetailResponseDTO;
import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.dto.ApplicationResponseDTO;
import com.example.application_service.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "Başvuru oluşturma")
    @PostMapping
    public ResponseEntity<String> createApplication(@RequestBody ApplicationRequestDTO request) {
        applicationService.createApplication(request);

        return ResponseEntity.status(HttpStatus.CREATED).body("Başvuru başarıyla oluşturuldu.");
    }

    @Operation(summary ="id ile bütün bilgileri getirme")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDetailResponseDTO> getApplication(@PathVariable Long id) {
        ApplicationDetailResponseDTO response = applicationService.getApplicationDetails(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary ="id ile bilgi güncelleme")
    @PutMapping("/{id}")
    public ApplicationResponseDTO updateApplication(
            @PathVariable Long id,
            @RequestBody ApplicationRequestDTO request) {

        return applicationService.updateApplication(id, request);
    }

    @Operation(summary = "Başvuru silme")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);

        return ResponseEntity.noContent().build();

    }
}

