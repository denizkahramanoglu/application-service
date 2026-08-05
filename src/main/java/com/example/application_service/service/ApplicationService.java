package com.example.application_service.service;

import com.example.application_service.client.CollectionServiceClient;
import com.example.application_service.client.CustomerClient;
import com.example.application_service.client.ProductClient;
import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import com.example.application_service.enums.ApplicationStatus;
import com.example.application_service.enums.PaymentMethod;
import com.example.application_service.exception.BusinessException;
import com.example.application_service.mapper.ApplicationMapper;
import com.example.application_service.repository.ApplicationRepository;
import com.example.application_service.util.BusinessExceptionUtil;
import com.example.application_service.util.FeignIntegrationUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sigorta başvuru (application) işlemlerinin yürütüldüğü ana servis sınıfı.
 * Müşteri (Customer), Ürün (Product) ve Tahsilat (Collection) mikroservisleri ile iletişime geçerek
 * başvuru oluşturma, güncelleme, silme ve detaylandırma süreçlerini orkestre eder.
 *
 * @author deniz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ProductClient productClient;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final CustomerClient customerClient;
    private final CollectionServiceClient collectionServiceClient;

    /**
     * Yeni bir sigorta başvurusu oluşturur.
     * Müşteri ve ürün bilgilerini diğer mikroservislerden doğrular,
     * fiyat hesaplamasını yapar, başvuru kaydını oluşturur ve eş zamanlı olarak tahsilat sürecini başlatır.
     *
     * @param request Başvuru oluşturmak için gerekli verileri barındıran {@link ApplicationRequestDTO} nesnesi
     * @return Veritabanına kaydedilen başvurunun bilgilerini içeren {@link ApplicationResponseDTO} nesnesi
     * @throws BusinessException Ödeme bilgileri geçersizse veya dış servislere (Müşteri, Ürün, Tahsilat) ulaşılamazsa fırlatılır
     */
    @Transactional
    public ApplicationResponseDTO createApplication(ApplicationRequestDTO request) {
        validatePaymentInfo(request);

        CustomerResponseDTO customer = getCustomerSafely(request.getCustomerId());
        InsuranceProductResponseDTO product = getProductSafely(request.getProductId());
        PricingResponseDTO pricingResponse = productClient.calculatePrice(applicationMapper.toPricingRequest(customer, product, request));

        ApplicationEntity application = applicationMapper.toEntity(request);
        application.setProductId(product.getProductId());
        application.setPrice(pricingResponse.getFinalPrice());
        application.setCurrency(pricingResponse.getCurrency());
        application.setStatus(ApplicationStatus.PENDING_PAYMENT);

        setPaymentInfo(application, request);

        ApplicationEntity savedApplication = applicationRepository.save(application);

        initiateCollectionProcess(savedApplication, request, customer, product);

        return applicationMapper.toResponseDTO(savedApplication);
    }

    /**
     * Belirtilen başvuru ID'sine ait detayları, ilgili müşteri ve ürün bilgileriyle birleştirerek getirir.
     *
     * @param applicationId Detayları getirilecek başvurunun benzersiz ID'si
     * @return Başvuru, müşteri ve ürün bilgilerini kapsayan zenginleştirilmiş {@link ApplicationDetailResponseDTO} nesnesi
     * @throws BusinessException Başvuru bulunamazsa veya ilgili dış servislerden veri alınamazsa fırlatılır
     */
    @Transactional(readOnly = true)
    public ApplicationDetailResponseDTO getApplicationDetails(Long applicationId) {
        ApplicationEntity application = getApplicationById(applicationId);
        CustomerResponseDTO customer = getCustomerSafely(application.getCustomerId());
        InsuranceProductResponseDTO product = getProductSafely(application.getProductId());

        return applicationMapper.toDetailResponse(application, customer, product);
    }

    /**
     * Mevcut bir başvurunun bilgilerini günceller.
     * Güncel müşteri, ürün ve ödeme verilerine göre fiyat hesaplamasını yeniden yapar ve kaydeder.
     *
     * @param applicationId Güncellenecek başvurunun benzersiz ID'si
     * @param request       Güncel başvuru bilgilerini barındıran {@link ApplicationRequestDTO} nesnesi
     * @return Güncellenmiş başvurunun verilerini içeren {@link ApplicationResponseDTO} nesnesi
     * @throws BusinessException Başvuru sistemde bulunamazsa veya ödeme validasyonları/dış servis istekleri başarısız olursa fırlatılır
     */
    @Transactional
    public ApplicationResponseDTO updateApplication(Long applicationId, ApplicationRequestDTO request) {
        validatePaymentInfo(request);

        ApplicationEntity application = getApplicationById(applicationId);
        CustomerResponseDTO customer = getCustomerSafely(request.getCustomerId());
        InsuranceProductResponseDTO product = getProductSafely(request.getProductId());
        PricingResponseDTO pricingResponse = productClient.calculatePrice(applicationMapper.toPricingRequest(customer, product, request));
        application.setCustomerId(request.getCustomerId());
        application.setProductId(product.getProductId());
        application.setPrice(pricingResponse.getFinalPrice());
        application.setCurrency(pricingResponse.getCurrency());
        setPaymentInfo(application, request);
        ApplicationEntity updatedApplication = applicationRepository.save(application);

        return applicationMapper.toResponseDTO(updatedApplication);
    }

    /**
     * Belirtilen ID'ye sahip başvuruyu veritabanından kalıcı olarak siler.
     *
     * @param id Silinecek başvurunun benzersiz ID'si
     * @throws BusinessException Verilen ID ile eşleşen bir başvuru bulunamazsa fırlatılır
     */
    @Transactional
    public void deleteApplication(Long id) {
        ApplicationEntity application = getApplicationById(id);
        applicationRepository.delete(application);
    }

    /* --- PRIVATE METOTLAR --- */

    private void validatePaymentInfo(ApplicationRequestDTO request) {
        PaymentMethod method = request.getPaymentMethod();
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(method == null, "Ödeme yöntemi seçilmelidir", HttpStatus.BAD_REQUEST);

        switch (method) {
            case CREDIT_CARD -> validateCreditCardPayment(request);
            case CASH -> validateCashPayment(request);
        }
    }

    private void validateCreditCardPayment(ApplicationRequestDTO request) {
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(request.getInstallmentCount() == null || request.getInstallmentCount() < 1, "Kredi kartı ödeme için taksit sayısı 1'den büyük olmalıdır", HttpStatus.BAD_REQUEST);
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(request.getCardId() == null, "Kredi kartı ödeme için kart ID gereklidir", HttpStatus.BAD_REQUEST);
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(request.getCvcNo() == null || request.getCvcNo().trim().isEmpty(), "Kredi kartı ödeme için CVC kodu gereklidir", HttpStatus.BAD_REQUEST);
    }

    private void validateCashPayment(ApplicationRequestDTO request) {
        if (request.getInstallmentCount() != null && request.getInstallmentCount() != 1) {
            log.warn("Nakit ödeme seçilmiş ancak taksit sayısı 1'den farklı. Taksit sayısı 1 olarak ayarlanacaktır");
        }
    }

    private void setPaymentInfo(ApplicationEntity application, ApplicationRequestDTO request) {

        boolean isCash = request.getPaymentMethod() == PaymentMethod.CASH;
        application.setPaymentMethod(request.getPaymentMethod());
        application.setInstallmentCount(isCash ? 1 : request.getInstallmentCount());
        application.setCardId(isCash ? null : request.getCardId());
    }

    private void initiateCollectionProcess(ApplicationEntity application, ApplicationRequestDTO request, CustomerResponseDTO customer, InsuranceProductResponseDTO product) {

        CollectionRequestDTO collectionRequest = applicationMapper.toCollectionRequest(application, request, customer, product);
        PaymentResponseDTO paymentResponse = FeignIntegrationUtil.executeSafely(() -> collectionServiceClient.initiateCollection(collectionRequest), "Tahsilat servisi bulunamadı.", "Tahsilat bilgileri geçersiz", "Tahsilat");
        log.info("Tahsilat işlemi başarıyla başlatıldı. Application ID: {}, Transaction ID: {}", application.getId(), paymentResponse.getTransactionId());
    }

    private ApplicationEntity getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Başvuru bulunamadı. ID: " + applicationId, HttpStatus.NOT_FOUND));
    }

    private CustomerResponseDTO getCustomerSafely(Long customerId) {
        return FeignIntegrationUtil.executeSafely(() -> customerClient.getCustomerById(customerId), "Müşteri bulunamadı. Hatalı Müşteri ID: " + customerId, "Geçersiz müşteri talebi.", "Müşteri");
    }

    private InsuranceProductResponseDTO getProductSafely(Long productId) {
        return FeignIntegrationUtil.executeSafely(() -> productClient.getProduct(productId), "Ürün bulunamadı. Hatalı Ürün ID: " + productId, "Geçersiz ürün talebi.", "Ürün");
    }
}