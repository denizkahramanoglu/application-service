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
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
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


        CustomerResponseDTO customer = customerClient.getCustomerById(request.getCustomerId());
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(customer == null ,"Müşteri bulunamadı. Hatalı Müşteri ID: " + request.getCustomerId(), HttpStatus.NOT_FOUND);
        InsuranceProductResponseDTO product;

        try {
            product = productClient.getProduct(request.getProductId());
            BusinessExceptionUtil.businessExceptionCheckerAndThrowException(product == null ,"Ürün servisi başarılı yanıt verdi ancak ürün verisi (body) boş.", HttpStatus.BAD_GATEWAY );
        } catch (FeignException.NotFound e) {
            throw new BusinessException(
                    "Ürün bulunamadı, başvuru oluşturulamaz. Hatalı Ürün ID: " + request.getProductId(),
                    HttpStatus.NOT_FOUND
            );
        } catch (FeignException e) {
            throw new BusinessException(
                    "Ürün servisi ile iletişim kurulamıyor. Lütfen daha sonra tekrar deneyin.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        PricingResponseDTO pricingResponse = productClient.calculatePrice(applicationMapper.toPricingRequest(customer, product,request)
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

        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Başvuru bulunamadı. ID: " + applicationId, HttpStatus.NOT_FOUND));
        CustomerResponseDTO customer;
        try {
            customer = customerClient.getCustomerById(application.getCustomerId());
            BusinessExceptionUtil.businessExceptionCheckerAndThrowException(customer == null, "Müşteri bulunamadı. Müşteri ID: " + application.getCustomerId(), HttpStatus.NOT_FOUND);

        } catch (FeignException.NotFound e) {
            throw new BusinessException("Müşteri bulunamadı. Müşteri ID: " + application.getCustomerId(), HttpStatus.NOT_FOUND);

        } catch (FeignException e) {
            throw new BusinessException("Müşteri servisi ile iletişim kurulamıyor.", HttpStatus.SERVICE_UNAVAILABLE
            );
        }
        InsuranceProductResponseDTO product;
        try {
            product = productClient.getProduct(application.getProductId());
            BusinessExceptionUtil.businessExceptionCheckerAndThrowException(product == null, "Ürün bulunamadı. Ürün ID: " + application.getProductId(), HttpStatus.NOT_FOUND);

        } catch (FeignException.NotFound e) {
            throw new BusinessException("Ürün bulunamadı. Ürün ID: " + application.getProductId(), HttpStatus.NOT_FOUND);

        } catch (FeignException e) {
            throw new BusinessException("Ürün servisi ile iletişim kurulamıyor.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return applicationMapper.toDetailResponse(application, customer, product);
    }
    @Transactional
    public ApplicationResponseDTO updateApplication(Long applicationId, ApplicationRequestDTO request) {

        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Başvuru bulunamadı. ID: " + applicationId, HttpStatus.NOT_FOUND));

        CustomerResponseDTO customer = customerClient.getCustomerById(request.getCustomerId());
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(customer == null ,"Müşteri bulunamadı. Hatalı Müşteri ID: " + request.getCustomerId(), HttpStatus.NOT_FOUND);
        InsuranceProductResponseDTO product;

        try {
            product = productClient.getProduct(request.getProductId());
            BusinessExceptionUtil.businessExceptionCheckerAndThrowException(product == null , "Ürün servisi başarılı yanıt verdi ancak ürün verisi boş.", HttpStatus.BAD_GATEWAY);

        } catch (FeignException.NotFound e) {
            throw new BusinessException("Ürün bulunamadı. Hatalı Ürün ID: " + request.getProductId(), HttpStatus.NOT_FOUND);

        } catch (FeignException e) {
            throw new BusinessException("Ürün servisi ile iletişim kurulamıyor.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        PricingResponseDTO pricingResponse = productClient.calculatePrice(applicationMapper.toPricingRequest(customer, product, request));
        application.setCustomerId(request.getCustomerId());
        application.setProductId(product.getProductId());
        application.setPrice(pricingResponse.getFinalPrice());
        application.setCurrency(pricingResponse.getCurrency());
        ApplicationEntity updatedApplication = applicationRepository.save(application);
        return applicationMapper.toResponseDTO(updatedApplication);

    }
    @Transactional
    public void deleteApplication(Long id) {

        ApplicationEntity application = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Silinmek istenen başvuru bulunamadı veya zaten silinmiş. ID: " + id, HttpStatus.NOT_FOUND));
        applicationRepository.delete(application);
    }
}