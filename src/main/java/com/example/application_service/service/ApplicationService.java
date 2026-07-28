package com.example.application_service.service;

import com.example.application_service.client.CustomerClient;
import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.exception.BusinessException;
import com.example.application_service.mapper.ApplicationMapper;
import com.example.application_service.repository.ApplicationRepository;
import com.example.application_service.util.BusinessExceptionUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ProductClient productClient;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final CustomerClient customerClient;

    @Transactional
    public ApplicationResponseDTO createApplication(ApplicationRequestDTO request) {
        CustomerResponseDTO customer = getCustomerSafely(request.getCustomerId());
        InsuranceProductResponseDTO product = getProductSafely(request.getProductId());
        PricingResponseDTO pricingResponse = productClient.calculatePrice(applicationMapper.toPricingRequest(customer, product, request)
        );

        ApplicationEntity application = applicationMapper.toEntity(request);
        application.setProductId(product.getProductId());
        application.setPrice(pricingResponse.getFinalPrice());
        application.setCurrency(pricingResponse.getCurrency());
        ApplicationEntity savedApplication = applicationRepository.save(application);

        return applicationMapper.toResponseDTO(savedApplication);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponseDTO getApplicationDetails(Long applicationId) {
        ApplicationEntity application = getApplicationById(applicationId);
        CustomerResponseDTO customer = getCustomerSafely(application.getCustomerId());
        InsuranceProductResponseDTO product = getProductSafely(application.getProductId());

        return applicationMapper.toDetailResponse(application, customer, product);
    }

    @Transactional
    public ApplicationResponseDTO updateApplication(Long applicationId, ApplicationRequestDTO request) {
        ApplicationEntity application = getApplicationById(applicationId);
        CustomerResponseDTO customer = getCustomerSafely(request.getCustomerId());
        InsuranceProductResponseDTO product = getProductSafely(request.getProductId());
        PricingResponseDTO pricingResponse = productClient.calculatePrice(applicationMapper.toPricingRequest(customer, product, request)
        );

        application.setCustomerId(request.getCustomerId());
        application.setProductId(product.getProductId());
        application.setPrice(pricingResponse.getFinalPrice());
        application.setCurrency(pricingResponse.getCurrency());
        ApplicationEntity updatedApplication = applicationRepository.save(application);

        return applicationMapper.toResponseDTO(updatedApplication);
    }

    @Transactional
    public void deleteApplication(Long id) {
        ApplicationEntity application = getApplicationById(id);
        applicationRepository.delete(application);
    }

    private ApplicationEntity getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Başvuru bulunamadı. ID: " + applicationId, HttpStatus.NOT_FOUND));
    }

    private CustomerResponseDTO getCustomerSafely(Long customerId) {
        try {
            CustomerResponseDTO customer = customerClient.getCustomerById(customerId);
            BusinessExceptionUtil.businessExceptionCheckerAndThrowException(customer == null, "Müşteri bulunamadı. Hatalı Müşteri ID: " + customerId, HttpStatus.NOT_FOUND);
            return customer;
        } catch (FeignException.NotFound e) {
            throw new BusinessException("Müşteri bulunamadı. Müşteri ID: " + customerId, HttpStatus.NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException("Müşteri servisi ile iletişim kurulamıyor. Lütfen daha sonra tekrar deneyin.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private InsuranceProductResponseDTO getProductSafely(Long productId) {
        try {
            InsuranceProductResponseDTO product = productClient.getProduct(productId);
            BusinessExceptionUtil.businessExceptionCheckerAndThrowException(product == null, "Ürün servisi başarılı yanıt verdi ancak ürün verisi (body) boş.", HttpStatus.BAD_GATEWAY);
            return product;
        } catch (FeignException.NotFound e) {
            throw new BusinessException("Ürün bulunamadı. Hatalı Ürün ID: " + productId, HttpStatus.NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException("Ürün servisi ile iletişim kurulamıyor. Lütfen daha sonra tekrar deneyin.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}