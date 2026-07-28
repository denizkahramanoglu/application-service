package com.example.application_service.controller;


import com.example.application_service.dto.ApplicationDetailResponseDTO;
import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.dto.ApplicationResponseDTO;
import com.example.application_service.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApplicationController applicationController;

    @Test
    @DisplayName("Başvuru oluşturma isteği geldiğinde servis çağrılmalı ve 201 Created dönmeli")
    void createApplication_shouldCallServiceAndReturn201() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();

        // Controller metodunun döndüğü ResponseEntity'yi doğrudan test ediyoruz
        ResponseEntity<String> response = applicationController.createApplication(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Başvuru başarıyla oluşturuldu.", response.getBody());

        verify(applicationService).createApplication(request);
    }

    @Test
    @DisplayName("Geçerli ID ile başvuru istendiğinde servis çağrılmalı ve 200 OK ile veriyi dönmeli")
    void getApplication_shouldReturn200AndData_whenIdIsValid() {
        ApplicationDetailResponseDTO mockServiceResponse = new ApplicationDetailResponseDTO();

        when(applicationService.getApplicationDetails(1L))
                .thenReturn(mockServiceResponse);

        ResponseEntity<ApplicationDetailResponseDTO> response = applicationController.getApplication(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockServiceResponse, response.getBody());

        verify(applicationService).getApplicationDetails(1L);
    }

    @Test
    @DisplayName("Başvuru güncelleme isteği geldiğinde servis çağrılmalı ve güncel DTO dönmeli")
    void updateApplication_shouldReturnUpdatedDTO_whenRequestIsValid() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        ApplicationResponseDTO mockServiceResponse = new ApplicationResponseDTO();

        when(applicationService.updateApplication(1L, request))
                .thenReturn(mockServiceResponse);

        // Update metodu ResponseEntity dönmediği için doğrudan DTO'yu kontrol ediyoruz
        ApplicationResponseDTO response = applicationController.updateApplication(1L, request);

        assertEquals(mockServiceResponse, response);

        verify(applicationService).updateApplication(1L, request);
    }

    @Test
    @DisplayName("Geçerli ID ile silme isteği geldiğinde servis çağrılmalı ve 204 No Content dönmeli")
    void deleteApplication_shouldCallServiceAndReturn204() {
        ResponseEntity<Void> response = applicationController.deleteApplication(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Servisin çağrıldığını doğrulamak void metodlar için yeterlidir (doNothing() varsayılan davranıştır)
        verify(applicationService).deleteApplication(1L);
    }
}