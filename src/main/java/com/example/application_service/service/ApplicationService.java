package com.example.application_service.service;

import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.exception.BusinessException;
import com.example.application_service.mapper.ApplicationMapper;
import com.example.application_service.repository.ApplicationRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ProductClient productClient;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;


    @Transactional
    public void createApplication(ApplicationRequestDTO request) {

        InsuranceProductResponseDTO product;

        try {
            // Feign Client ile ürün servisine istek atıyoruz
            product = productClient.getProduct(request.getProductId());

            if (product == null) {
                throw new BusinessException(
                        "Ürün servisi başarılı yanıt verdi ancak ürün verisi (body) boş.",
                        HttpStatus.BAD_GATEWAY
                );
            }

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

        // DTO'dan Entity'ye çevirim
        ApplicationEntity application = applicationMapper.toEntity(request);

        // Ürün servisinden gelen gerçek bilgileri Entity'ye işliyoruz
        application.setProductId(product.getProductId());
        application.setPrice(product.getPrice());

        // Veritabanına kayıt
        applicationRepository.save(application);
    }
    public ApplicationResponseDTO updateApplication(Long id, ApplicationRequestDTO request) {

        // 1. Veritabanından mevcut başvuruyu bul
        ApplicationEntity existingApplication = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Güncellenmek istenen başvuru bulunamadı. Geçersiz ID: " + id,
                        HttpStatus.NOT_FOUND
                ));

        // 2. Sadece Entity'de var olan alanları güncelliyoruz
        existingApplication.setCustomerId(request.getCustomerId());

        // Eğer müşteri farklı bir ürüne (Product) geçmek istediyse:
        if (!existingApplication.getProductId().equals(request.getProductId())) {
            existingApplication.setProductId(request.getProductId());
        }

        // 3. Veritabanına kaydet
        ApplicationEntity updatedApplication = applicationRepository.save(existingApplication);

        // 4. Sonucu dön
        return applicationMapper.toResponseDTO(updatedApplication);
    }

    public ApplicationResponseDTO getApplication(Long id) {


        ApplicationEntity application = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Sistemde böyle bir başvuru bulunmamaktadır. Geçersiz Başvuru ID: " + id,
                        HttpStatus.NOT_FOUND
                ));


        return applicationMapper.toResponseDTO(application);
    }
    @Transactional
    public void deleteApplication(Long id) {

        ApplicationEntity application = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Silinmek istenen başvuru bulunamadı veya zaten silinmiş. ID: " + id,
                        HttpStatus.NOT_FOUND
                ));

        applicationRepository.delete(application);
    }
}