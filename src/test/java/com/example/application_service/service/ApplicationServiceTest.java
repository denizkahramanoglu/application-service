package com.example.application_service.service;

import com.example.application_service.client.CustomerClient;
import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.exception.BusinessException;
import com.example.application_service.mapper.ApplicationMapper;
import com.example.application_service.repository.ApplicationRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ProductClient productClient;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private ApplicationService applicationService;


    // =========================================================
    // CREATE APPLICATION
    // =========================================================

    @Test
    void createApplication_shouldCreateSuccessfully() {

        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(1L);
        request.setProductId(2L);

        CustomerResponseDTO customer = new CustomerResponseDTO();
        customer.setId(1L);

        InsuranceProductResponseDTO product =
                new InsuranceProductResponseDTO();
        product.setProductId(2L);

        PricingResponseDTO pricingResponse =
                new PricingResponseDTO();
        pricingResponse.setFinalPrice(BigDecimal.valueOf(856.03));
        pricingResponse.setCurrency("EUR");

        ApplicationEntity application =
                new ApplicationEntity();

        ApplicationEntity savedApplication =
                new ApplicationEntity();
        savedApplication.setId(10L);
        savedApplication.setCustomerId(1L);
        savedApplication.setProductId(2L);
        savedApplication.setPrice(BigDecimal.valueOf(856.03));
        savedApplication.setCurrency("EUR");

        ApplicationResponseDTO response =
                new ApplicationResponseDTO();

        when(customerClient.getCustomerById(1L))
                .thenReturn(customer);

        when(productClient.getProduct(2L))
                .thenReturn(product);

        when(applicationMapper.toPricingRequest(
                customer,
                product,
                request
        )).thenReturn(new PricingRequestDTO());

        when(productClient.calculatePrice(any(PricingRequestDTO.class)))
                .thenReturn(pricingResponse);

        when(applicationMapper.toEntity(request))
                .thenReturn(application);

        when(applicationRepository.save(application))
                .thenReturn(savedApplication);

        when(applicationMapper.toResponseDTO(savedApplication))
                .thenReturn(response);

        ApplicationResponseDTO result =
                applicationService.createApplication(request);

        assertNotNull(result);

        assertEquals(2L, application.getProductId());
        assertEquals(BigDecimal.valueOf(856.03), application.getPrice());
        assertEquals("EUR", application.getCurrency());

        verify(customerClient).getCustomerById(1L);
        verify(productClient).getProduct(2L);
        verify(productClient).calculatePrice(any(PricingRequestDTO.class));
        verify(applicationRepository).save(application);
        verify(applicationMapper).toResponseDTO(savedApplication);
    }


    @Test
    void createApplication_shouldThrow_whenCustomerNotFound() {

        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(1L);
        request.setProductId(2L);

        when(customerClient.getCustomerById(1L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.createApplication(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals(
                "Müşteri bulunamadı. Hatalı Müşteri ID: 1",
                exception.getMessage()
        );

        verify(customerClient).getCustomerById(1L);
        verifyNoInteractions(productClient);
        verifyNoInteractions(applicationRepository);
    }


    @Test
    void createApplication_shouldThrow_whenProductIsNull() {

        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(1L);
        request.setProductId(2L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(customerClient.getCustomerById(1L))
                .thenReturn(customer);

        when(productClient.getProduct(2L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.createApplication(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(customerClient).getCustomerById(1L);
        verify(productClient).getProduct(2L);
        verifyNoInteractions(applicationRepository);
    }


    @Test
    void createApplication_shouldThrowNotFound_whenProductNotFound() {

        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(1L);
        request.setProductId(2L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(customerClient.getCustomerById(1L))
                .thenReturn(customer);

        FeignException.NotFound exception =
                mock(FeignException.NotFound.class);

        when(productClient.getProduct(2L))
                .thenThrow(exception);

        BusinessException result = assertThrows(
                BusinessException.class,
                () -> applicationService.createApplication(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, result.getStatus());

        verify(productClient).getProduct(2L);
        verifyNoInteractions(applicationRepository);
    }


    @Test
    void createApplication_shouldThrowServiceUnavailable_whenProductServiceFails() {

        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setCustomerId(1L);
        request.setProductId(2L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(customerClient.getCustomerById(1L))
                .thenReturn(customer);

        FeignException exception =
                mock(FeignException.class);

        when(productClient.getProduct(2L))
                .thenThrow(exception);

        BusinessException result = assertThrows(
                BusinessException.class,
                () -> applicationService.createApplication(request)
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                result.getStatus()
        );

        verify(productClient).getProduct(2L);
        verifyNoInteractions(applicationRepository);
    }


    // =========================================================
    // GET APPLICATION DETAILS
    // =========================================================

    @Test
    void getApplicationDetails_shouldReturnSuccessfully() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();
        customer.setId(10L);

        InsuranceProductResponseDTO product =
                new InsuranceProductResponseDTO();
        product.setProductId(20L);

        ApplicationDetailResponseDTO response =
                new ApplicationDetailResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenReturn(product);

        when(applicationMapper.toDetailResponse(
                application,
                customer,
                product
        )).thenReturn(response);

        ApplicationDetailResponseDTO result =
                applicationService.getApplicationDetails(1L);

        assertNotNull(result);

        verify(applicationRepository).findById(1L);
        verify(customerClient).getCustomerById(10L);
        verify(productClient).getProduct(20L);
        verify(applicationMapper)
                .toDetailResponse(application, customer, product);
    }


    @Test
    void getApplicationDetails_shouldThrow_whenApplicationNotFound() {

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(applicationRepository).findById(1L);
        verifyNoInteractions(customerClient);
        verifyNoInteractions(productClient);
    }


    @Test
    void getApplicationDetails_shouldThrowNotFound_whenCustomerNotFound() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(customerClient).getCustomerById(10L);
        verifyNoInteractions(productClient);
        verifyNoInteractions(applicationMapper);
    }


    @Test
    void getApplicationDetails_shouldThrowNotFound_whenCustomerFeignReturns404() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenThrow(mock(FeignException.NotFound.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(customerClient).getCustomerById(10L);
        verifyNoInteractions(productClient);
    }


    @Test
    void getApplicationDetails_shouldThrowServiceUnavailable_whenCustomerServiceFails() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenThrow(mock(FeignException.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatus()
        );

        verify(customerClient).getCustomerById(10L);
        verifyNoInteractions(productClient);
    }


    @Test
    void getApplicationDetails_shouldThrow_whenProductIsNull() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(productClient).getProduct(20L);
        verifyNoInteractions(applicationMapper);
    }


    @Test
    void getApplicationDetails_shouldThrowNotFound_whenProductNotFound() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenThrow(mock(FeignException.NotFound.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(productClient).getProduct(20L);
        verifyNoInteractions(applicationMapper);
    }


    @Test
    void getApplicationDetails_shouldThrowServiceUnavailable_whenProductServiceFails() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);
        application.setCustomerId(10L);
        application.setProductId(20L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenThrow(mock(FeignException.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.getApplicationDetails(1L)
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatus()
        );

        verify(productClient).getProduct(20L);
        verifyNoInteractions(applicationMapper);
    }


    // =========================================================
    // UPDATE APPLICATION
    // =========================================================

    @Test
    void updateApplication_shouldUpdateSuccessfully() {

        ApplicationRequestDTO request =
                new ApplicationRequestDTO();

        request.setCustomerId(10L);
        request.setProductId(20L);

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);

        CustomerResponseDTO customer =
                new CustomerResponseDTO();
        customer.setId(10L);

        InsuranceProductResponseDTO product =
                new InsuranceProductResponseDTO();
        product.setProductId(20L);

        PricingResponseDTO pricingResponse =
                new PricingResponseDTO();

        pricingResponse.setFinalPrice(BigDecimal.valueOf(856.03));
        pricingResponse.setCurrency("EUR");

        ApplicationEntity updatedEntity =
                new ApplicationEntity();

        updatedEntity.setId(1L);

        ApplicationResponseDTO response =
                new ApplicationResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenReturn(product);

        when(applicationMapper.toPricingRequest(
                customer,
                product,
                request
        )).thenReturn(new PricingRequestDTO());

        when(productClient.calculatePrice(any(PricingRequestDTO.class)))
                .thenReturn(pricingResponse);

        when(applicationRepository.save(application))
                .thenReturn(updatedEntity);

        when(applicationMapper.toResponseDTO(updatedEntity))
                .thenReturn(response);

        ApplicationResponseDTO result =
                applicationService.updateApplication(1L, request);

        assertNotNull(result);

        assertEquals(10L, application.getCustomerId());
        assertEquals(20L, application.getProductId());
        assertEquals(BigDecimal.valueOf(856.03), application.getPrice());
        assertEquals("EUR", application.getCurrency());

        verify(applicationRepository).save(application);
        verify(applicationMapper).toResponseDTO(updatedEntity);
    }


    @Test
    void updateApplication_shouldThrow_whenApplicationNotFound() {

        ApplicationRequestDTO request =
                new ApplicationRequestDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.updateApplication(1L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(applicationRepository).findById(1L);
        verifyNoInteractions(customerClient);
        verifyNoInteractions(productClient);
    }


    @Test
    void updateApplication_shouldThrow_whenCustomerNotFound() {

        ApplicationRequestDTO request =
                new ApplicationRequestDTO();

        request.setCustomerId(10L);
        request.setProductId(20L);

        ApplicationEntity application =
                new ApplicationEntity();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.updateApplication(1L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(customerClient).getCustomerById(10L);
        verifyNoInteractions(productClient);
        verify(applicationRepository, never()).save(any());
    }


    @Test
    void updateApplication_shouldThrow_whenProductIsNull() {

        ApplicationRequestDTO request =
                new ApplicationRequestDTO();

        request.setCustomerId(10L);
        request.setProductId(20L);

        ApplicationEntity application =
                new ApplicationEntity();

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.updateApplication(1L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(productClient).getProduct(20L);
        verify(applicationRepository, never()).save(any());
    }


    @Test
    void updateApplication_shouldThrowNotFound_whenProductNotFound() {

        ApplicationRequestDTO request =
                new ApplicationRequestDTO();

        request.setCustomerId(10L);
        request.setProductId(20L);

        ApplicationEntity application =
                new ApplicationEntity();

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenThrow(mock(FeignException.NotFound.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.updateApplication(1L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(applicationRepository, never()).save(any());
    }


    @Test
    void updateApplication_shouldThrowServiceUnavailable_whenProductServiceFails() {

        ApplicationRequestDTO request =
                new ApplicationRequestDTO();

        request.setCustomerId(10L);
        request.setProductId(20L);

        ApplicationEntity application =
                new ApplicationEntity();

        CustomerResponseDTO customer =
                new CustomerResponseDTO();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(customerClient.getCustomerById(10L))
                .thenReturn(customer);

        when(productClient.getProduct(20L))
                .thenThrow(mock(FeignException.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.updateApplication(1L, request)
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatus()
        );

        verify(applicationRepository, never()).save(any());
    }


    // =========================================================
    // DELETE APPLICATION
    // =========================================================

    @Test
    void deleteApplication_shouldDeleteSuccessfully() {

        ApplicationEntity application =
                new ApplicationEntity();

        application.setId(1L);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        applicationService.deleteApplication(1L);

        verify(applicationRepository).findById(1L);
        verify(applicationRepository).delete(application);
    }


    @Test
    void deleteApplication_shouldThrow_whenApplicationNotFound() {

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> applicationService.deleteApplication(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(applicationRepository).findById(1L);
        verify(applicationRepository, never()).delete(any());
    }
}