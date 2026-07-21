package com.example.application_service.service;

import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.exception.BusinessException;
import com.example.application_service.mapper.ApplicationMapper;
import com.example.application_service.repository.ApplicationRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ProductClient productClient;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationMapper applicationMapper;

    @InjectMocks private ApplicationService applicationService;

    // ==========================================
    // CREATE APPLICATION TESTS
    // ==========================================

    @Test
    void createApplication_Success() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setProductId(1L);

        InsuranceProductResponseDTO product = new InsuranceProductResponseDTO();
        product.setProductId(1L);
        product.setPrice(new BigDecimal("100.00"));

        ApplicationEntity entity = new ApplicationEntity();

        when(productClient.getProduct(1L)).thenReturn(product);
        when(applicationMapper.toEntity(request)).thenReturn(entity);

        applicationService.createApplication(request);

        verify(applicationRepository).save(entity);
        assertEquals(1L, entity.getProductId());
        assertEquals(new BigDecimal("100.00"), entity.getPrice());
    }

    @Test
    void createApplication_ProductNull_ThrowsBusinessException() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setProductId(1L);

        when(productClient.getProduct(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.createApplication(request));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
    }

    @Test
    void createApplication_ProductNotFound_ThrowsBusinessException() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setProductId(1L);

        Request mockRequest = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        when(productClient.getProduct(1L)).thenThrow(new FeignException.NotFound("", mockRequest, null, null));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.createApplication(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createApplication_FeignGeneralException_ThrowsBusinessException() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setProductId(1L);

        Request mockRequest = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        when(productClient.getProduct(1L)).thenThrow(new FeignException.ServiceUnavailable("", mockRequest, null, null));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.createApplication(request));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    // ==========================================
    // GET APPLICATION TESTS
    // ==========================================

    @Test
    void getApplication_NotFound_ThrowsBusinessException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.getApplication(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getApplication_Success() {
        ApplicationEntity entity = new ApplicationEntity();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationMapper.toResponseDTO(entity)).thenReturn(new ApplicationResponseDTO());

        assertNotNull(applicationService.getApplication(1L));
    }

    // ==========================================
    // UPDATE APPLICATION TESTS
    // ==========================================

    @Test
    void updateApplication_Success_SameProduct() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(2L);
        request.setProductId(1L); // Aynı ürün

        ApplicationEntity existingEntity = new ApplicationEntity();
        existingEntity.setId(1L);
        existingEntity.setCustomerId(1L);
        existingEntity.setProductId(1L);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
        when(applicationRepository.save(any(ApplicationEntity.class))).thenReturn(existingEntity);
        when(applicationMapper.toResponseDTO(existingEntity)).thenReturn(new ApplicationResponseDTO());

        ApplicationResponseDTO response = applicationService.updateApplication(1L, request);

        assertNotNull(response);
        verify(applicationRepository).save(existingEntity);
        assertEquals(2L, existingEntity.getCustomerId());
        assertEquals(1L, existingEntity.getProductId()); // Ürün değişmedi
    }

    @Test
    void updateApplication_Success_DifferentProduct() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(2L);
        request.setProductId(99L); // Farklı bir ürün ID

        ApplicationEntity existingEntity = new ApplicationEntity();
        existingEntity.setId(1L);
        existingEntity.setCustomerId(1L);
        existingEntity.setProductId(1L);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
        when(applicationRepository.save(any(ApplicationEntity.class))).thenReturn(existingEntity);
        when(applicationMapper.toResponseDTO(existingEntity)).thenReturn(new ApplicationResponseDTO());

        applicationService.updateApplication(1L, request);

        verify(applicationRepository).save(existingEntity);
        assertEquals(99L, existingEntity.getProductId()); // Ürün ID güncellenmiş olmalı
    }

    @Test
    void updateApplication_NotFound_ThrowsBusinessException() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();

        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.updateApplication(1L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ==========================================
    // CALCULATE FINAL PREMIUM TESTS
    // ==========================================

    @Test
    void calculateFinalPremiumForCustomer_Success_TRY() {
        LifePremiumRequestDTO request = new LifePremiumRequestDTO();

        LifePremiumResponseDTO response = new LifePremiumResponseDTO();
        response.setCalculatedPremium(1000L);
        response.setCurrency("TRY");

        when(productClient.calculatePremium(request)).thenReturn(response);

        Long finalPremium = applicationService.calculateFinalPremiumForCustomer(request);

        // TRY geldiği için kur çarpımı yapılmaz (veya 1 ile çarpılır), sonuç 1000 olmalı
        assertEquals(1000L, finalPremium);
    }

    @Test
    void calculateFinalPremiumForCustomer_NullResponse_ThrowsBusinessException() {
        LifePremiumRequestDTO request = new LifePremiumRequestDTO();

        when(productClient.calculatePremium(request)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.calculateFinalPremiumForCustomer(request));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
    }

    @Test
    void calculateFinalPremiumForCustomer_BadRequest_ThrowsBusinessException() {
        LifePremiumRequestDTO request = new LifePremiumRequestDTO();

        Request mockRequest = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        when(productClient.calculatePremium(request)).thenThrow(new FeignException.BadRequest("", mockRequest, null, null));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.calculateFinalPremiumForCustomer(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void calculateFinalPremiumForCustomer_NotFound_ThrowsBusinessException() {
        LifePremiumRequestDTO request = new LifePremiumRequestDTO();

        Request mockRequest = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        when(productClient.calculatePremium(request)).thenThrow(new FeignException.NotFound("", mockRequest, null, null));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.calculateFinalPremiumForCustomer(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void calculateFinalPremiumForCustomer_ServiceUnavailable_ThrowsBusinessException() {
        LifePremiumRequestDTO request = new LifePremiumRequestDTO();

        Request mockRequest = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, new RequestTemplate());
        when(productClient.calculatePremium(request)).thenThrow(new FeignException.ServiceUnavailable("", mockRequest, null, null));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                applicationService.calculateFinalPremiumForCustomer(request));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }
    @Test
    void calculateFinalPremiumForCustomer_Success() {
        LifePremiumRequestDTO request = new LifePremiumRequestDTO();
        request.setRequestedCurrency("USD"); // Müşteri USD istedi

        LifePremiumResponseDTO response = new LifePremiumResponseDTO();
        response.setCalculatedPremium(5000L); // Product 5000 hesapladı
        response.setCurrency("USD");

        when(productClient.calculatePremium(request)).thenReturn(response);

        Long finalPremium = applicationService.calculateFinalPremiumForCustomer(request);

        // Hiçbir çarpım yapılmadan doğrudan 5000 dönmeli
        assertEquals(5000L, finalPremium);
    }
}