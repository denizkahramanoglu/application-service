package com.example.application_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private Clock clock;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("BusinessException fırlatıldığında sabit zamanlı ve doğru formatlı JSON yanıtı dönmeli")
    void handleBusinessException_shouldReturnFormattedErrorResponse() {
        Instant fixedInstant = Instant.parse("2026-07-28T10:00:00Z");
        ZoneId zoneId = ZoneId.of("UTC");

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(zoneId);

        BusinessException exception = new BusinessException("Müşteri bulunamadı", HttpStatus.NOT_FOUND);

        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleBusinessException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assert response.getBody() != null;
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Müşteri bulunamadı", response.getBody().get("message"));
        assertEquals(LocalDateTime.ofInstant(fixedInstant, zoneId), response.getBody().get("timestamp"));
    }
}