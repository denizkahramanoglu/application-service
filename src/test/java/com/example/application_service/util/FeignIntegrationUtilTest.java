package com.example.application_service.util;

import com.example.application_service.exception.BusinessException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FeignIntegrationUtilTest {

    @Test
    @DisplayName("executeSafely - Başarılı sonuç dönmeli")
    void executeSafely_shouldReturnResult() {

        String result = FeignIntegrationUtil.executeSafely(
                () -> "SUCCESS",
                "Not Found",
                "Bad Request",
                "Customer"
        );

        assertEquals("SUCCESS", result);
    }

    @Test
    @DisplayName("executeSafely - Null sonuç dönerse BusinessException fırlatılmalı")
    void executeSafely_shouldThrowWhenResultIsNull() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> FeignIntegrationUtil.executeSafely(
                        () -> null,
                        "Not Found",
                        "Bad Request",
                        "Customer"
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Not Found", exception.getMessage());
    }

    @Test
    @DisplayName("executeSafely - Feign NotFound hatası BusinessException'a çevrilmeli")
    void executeSafely_shouldHandleFeignNotFound() {

        Request request = Request.create(
                Request.HttpMethod.GET,
                "/customers/1",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        FeignException.NotFound exception =
                new FeignException.NotFound(
                        "404",
                        request,
                        null,
                        Collections.emptyMap()
                );

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> FeignIntegrationUtil.executeSafely(
                        () -> { throw exception; },
                        "Customer Not Found",
                        "Bad Request",
                        "Customer"
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    }

    @Test
    @DisplayName("executeSafely - Feign BadRequest hatası BusinessException'a çevrilmeli")
    void executeSafely_shouldHandleFeignBadRequest() {

        Request request = Request.create(
                Request.HttpMethod.GET,
                "/customers",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        FeignException.BadRequest exception =
                new FeignException.BadRequest(
                        "400",
                        request,
                        null,
                        Collections.emptyMap()
                );

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> FeignIntegrationUtil.executeSafely(
                        () -> { throw exception; },
                        "Not Found",
                        "Invalid Request",
                        "Customer"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatus());
        assertEquals("Invalid Request", thrown.getMessage());
    }

    @Test
    @DisplayName("executeSafely - Genel FeignException BusinessException'a çevrilmeli")
    void executeSafely_shouldHandleFeignException() {

        Request request = Request.create(
                Request.HttpMethod.GET,
                "/customers",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        FeignException exception =
                FeignException.errorStatus(
                        "customerClient",
                        feign.Response.builder()
                                .status(500)
                                .reason("Internal Server Error")
                                .request(request)
                                .headers(Collections.emptyMap())
                                .build()
                );

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> FeignIntegrationUtil.executeSafely(
                        () -> { throw exception; },
                        "Not Found",
                        "Bad Request",
                        "Customer"
                )
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getStatus());
    }

    @Test
    @DisplayName("executeSafely - Beklenmeyen hata BusinessException'a çevrilmeli")
    void executeSafely_shouldHandleUnexpectedException() {

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> FeignIntegrationUtil.executeSafely(
                        () -> {
                            throw new RuntimeException("Boom");
                        },
                        "Not Found",
                        "Bad Request",
                        "Customer"
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatus());
        assertTrue(thrown.getMessage().contains("Boom"));
    }
}