package com.example.application_service.service;

import com.example.application_service.client.CollectionServiceClient;
import com.example.application_service.client.CustomerClient;
import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.enums.PaymentMethod;
import com.example.application_service.exception.BusinessException;
import com.example.application_service.mapper.ApplicationMapper;
import com.example.application_service.repository.ApplicationRepository;
import com.example.application_service.util.FeignIntegrationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ProductClient productClient;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private CollectionServiceClient collectionServiceClient;

    @InjectMocks
    private ApplicationService applicationService;

    private ApplicationRequestDTO request;
    private CustomerResponseDTO customer;
    private InsuranceProductResponseDTO product;
    private PricingResponseDTO pricing;
    private ApplicationEntity entity;
    private ApplicationResponseDTO response;

    @BeforeEach
    void setUp() {

        request = ApplicationRequestDTO.builder()
                .customerId(1L)
                .productId(2L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardId(10L)
                .build();

        customer = new CustomerResponseDTO();
        customer.setId(1L);

        product = InsuranceProductResponseDTO.builder()
                .productId(2L)
                .build();

        pricing = PricingResponseDTO.builder()
                .finalPrice(BigDecimal.valueOf(500))
                .currency("TRY")
                .build();

        entity = new ApplicationEntity();
        entity.setCustomerId(1L);

        response = new ApplicationResponseDTO();
    }

    @Test
    @DisplayName("createApplication - Başvuru başarıyla oluşturulmalı")
    void createApplication_shouldReturnApplication() {

        PricingRequestDTO pricingRequest = new PricingRequestDTO();

        try (MockedStatic<FeignIntegrationUtil> mocked = mockStatic(FeignIntegrationUtil.class)) {

            mocked.when(() ->
                            FeignIntegrationUtil.executeSafely(any(), anyString(), anyString(), anyString()))
                    .thenReturn(customer)
                    .thenReturn(product);

            when(applicationMapper.toPricingRequest(customer, product, request))
                    .thenReturn(pricingRequest);

            when(productClient.calculatePrice(pricingRequest))
                    .thenReturn(pricing);

            when(applicationMapper.toEntity(request))
                    .thenReturn(entity);

            when(applicationRepository.save(any(ApplicationEntity.class)))
                    .thenReturn(entity);

            when(applicationMapper.toResponseDTO(entity))
                    .thenReturn(response);

            ApplicationResponseDTO result = applicationService.createApplication(request);

            assertEquals(response, result);

            verify(applicationRepository).save(any(ApplicationEntity.class));
            verify(applicationMapper).toResponseDTO(entity);
        }
    }

    @Test
    @DisplayName("createApplication - Ödeme yöntemi boş ise exception fırlatılmalı")
    void createApplication_shouldThrowExceptionWhenPaymentMethodNull() {

        request.setPaymentMethod(null);

        assertThrows(BusinessException.class,
                () -> applicationService.createApplication(request));
    }

    @Test
    @DisplayName("createApplication - Kart ID yoksa exception fırlatılmalı")
    void createApplication_shouldThrowExceptionWhenCardIdNull() {

        request.setCardId(null);

        assertThrows(BusinessException.class,
                () -> applicationService.createApplication(request));
    }

    @Test
    @DisplayName("getApplicationDetails - Başvuru detayları dönülmeli")
    void getApplicationDetails_shouldReturnDetails() {

        ApplicationDetailResponseDTO detail = new ApplicationDetailResponseDTO();

        entity.setId(1L);
        entity.setProductId(2L);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        try (MockedStatic<FeignIntegrationUtil> mocked = mockStatic(FeignIntegrationUtil.class)) {

            mocked.when(() ->
                            FeignIntegrationUtil.executeSafely(any(), anyString(), anyString(), anyString()))
                    .thenReturn(customer)
                    .thenReturn(product);

            when(applicationMapper.toDetailResponse(entity, customer, product))
                    .thenReturn(detail);

            ApplicationDetailResponseDTO result =
                    applicationService.getApplicationDetails(1L);

            assertEquals(detail, result);
        }
    }

    @Test
    @DisplayName("getApplicationDetails - Başvuru bulunamazsa exception fırlatılmalı")
    void getApplicationDetails_shouldThrowException() {

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> applicationService.getApplicationDetails(1L));
    }
    @Test
    @DisplayName("updateApplication - Başvuru başarıyla güncellenmeli")
    void updateApplication_shouldReturnUpdatedApplication() {

        PricingRequestDTO pricingRequest = new PricingRequestDTO();

        entity.setId(1L);
        entity.setProductId(2L);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        try (MockedStatic<FeignIntegrationUtil> mocked = mockStatic(FeignIntegrationUtil.class)) {

            mocked.when(() ->
                            FeignIntegrationUtil.executeSafely(any(), anyString(), anyString(), anyString()))
                    .thenReturn(customer)
                    .thenReturn(product);

            when(applicationMapper.toPricingRequest(customer, product, request))
                    .thenReturn(pricingRequest);

            when(productClient.calculatePrice(pricingRequest))
                    .thenReturn(pricing);

            when(applicationRepository.save(entity))
                    .thenReturn(entity);

            when(applicationMapper.toResponseDTO(entity))
                    .thenReturn(response);

            ApplicationResponseDTO result =
                    applicationService.updateApplication(1L, request);

            assertEquals(response, result);

            verify(applicationRepository).save(entity);
            verify(applicationMapper).toResponseDTO(entity);
        }
    }

    @Test
    @DisplayName("updateApplication - Başvuru bulunamazsa exception fırlatılmalı")
    void updateApplication_shouldThrowExceptionWhenApplicationNotFound() {

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> applicationService.updateApplication(1L, request));
    }

    @Test
    @DisplayName("updateApplication - Ödeme yöntemi boş ise exception fırlatılmalı")
    void updateApplication_shouldThrowExceptionWhenPaymentMethodNull() {

        request.setPaymentMethod(null);

        assertThrows(BusinessException.class,
                () -> applicationService.updateApplication(1L, request));
    }

    @Test
    @DisplayName("updateApplication - Kart ID boş ise exception fırlatılmalı")
    void updateApplication_shouldThrowExceptionWhenCardIdNull() {

        request.setCardId(null);

        assertThrows(BusinessException.class,
                () -> applicationService.updateApplication(1L, request));
    }

    @Test
    @DisplayName("deleteApplication - Başvuru silinmeli")
    void deleteApplication_shouldDeleteApplication() {

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        applicationService.deleteApplication(1L);

        verify(applicationRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteApplication - Başvuru bulunamazsa exception fırlatılmalı")
    void deleteApplication_shouldThrowExceptionWhenApplicationNotFound() {

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> applicationService.deleteApplication(1L));
    }
    @Test
    @DisplayName("createApplication - Nakit ödeme için kart bilgisi null olmalı")
    void createApplication_shouldSetCardIdNullWhenCashPayment() {

        request.setPaymentMethod(PaymentMethod.CASH);
        request.setCardId(999L);

        PricingRequestDTO pricingRequest = new PricingRequestDTO();

        try (MockedStatic<FeignIntegrationUtil> mocked = mockStatic(FeignIntegrationUtil.class)) {

            mocked.when(() ->
                            FeignIntegrationUtil.executeSafely(any(), anyString(), anyString(), anyString()))
                    .thenReturn(customer)
                    .thenReturn(product);

            when(applicationMapper.toPricingRequest(customer, product, request))
                    .thenReturn(pricingRequest);

            when(productClient.calculatePrice(pricingRequest))
                    .thenReturn(pricing);

            when(applicationMapper.toEntity(request))
                    .thenReturn(entity);

            when(applicationRepository.save(any(ApplicationEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(applicationMapper.toResponseDTO(any()))
                    .thenReturn(response);

            applicationService.createApplication(request);

            assertNull(entity.getCardId());
            assertEquals(PaymentMethod.CASH, entity.getPaymentMethod());
            assertEquals(1, entity.getInstallmentCount());
        }
    }
}