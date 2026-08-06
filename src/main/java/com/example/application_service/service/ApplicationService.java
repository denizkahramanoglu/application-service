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

    /**
     * Ödeme yönteminin seçili olup olmadığını kontrol eder ve ödeme tipine göre detaylı doğrulama yapar.
     *
     * @param request Doğrulanacak başvuru verilerini içeren {@link ApplicationRequestDTO} nesnesi
     * @throws BusinessException Ödeme yöntemi boş bırakıldıysa veya kredi kartı seçilip kart ID belirtilmediyse fırlatılır
     */
    private void validatePaymentInfo(ApplicationRequestDTO request) {
        PaymentMethod method = request.getPaymentMethod();
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(method == null, "Ödeme yöntemi seçilmelidir", HttpStatus.BAD_REQUEST);

        if (method == PaymentMethod.CREDIT_CARD) {
            validateCreditCardPayment(request);
        }
        // Nakit ödeme için taksit veya ekstra kontrol kalmadığından CASH durumu kaldırıldı.
    }

    /**
     * Kredi kartı ile yapılan ödemelerde gerekli alanların doldurulup doldurulmadığını doğrular.
     *
     * @param request Doğrulanacak başvuru verilerini içeren {@link ApplicationRequestDTO} nesnesi
     * @throws BusinessException Kredi kartı ID bilgisi eksikse fırlatılır
     */
    private void validateCreditCardPayment(ApplicationRequestDTO request) {
        BusinessExceptionUtil.businessExceptionCheckerAndThrowException(request.getCardId() == null, "Kredi kartı ödeme için kart ID gereklidir", HttpStatus.BAD_REQUEST);
    }

    /**
     * Başvuru nesnesine ödeme yöntemi, taksit sayısı ve kart ID bilgilerini atar.
     *
     * @param application Ödeme bilgileri güncellenecek {@link ApplicationEntity} nesnesi
     * @param request     Kaynak ödeme verilerini barındıran {@link ApplicationRequestDTO} nesnesi
     */
    private void setPaymentInfo(ApplicationEntity application, ApplicationRequestDTO request) {
        boolean isCash = request.getPaymentMethod() == PaymentMethod.CASH;

        application.setPaymentMethod(request.getPaymentMethod());
        // Taksit sayısı request'ten alınmıyor, sistem her zaman tek çekim (1) olarak ayarlıyor
        application.setInstallmentCount(1);
        application.setCardId(isCash ? null : request.getCardId());
    }

    /**
     * Verilen ID değerine göre veritabanından başvuru kaydını çeker; bulunamazsa hata fırlatır.
     *
     * @param applicationId Aranacak başvurunun benzersiz ID'si
     * @return Bulunan {@link ApplicationEntity} nesnesi
     * @throws BusinessException Belirtilen ID ile eşleşen başvuru yoksa fırlatılır
     */
    private ApplicationEntity getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Başvuru bulunamadı. ID: " + applicationId, HttpStatus.NOT_FOUND));
    }

    /**
     * CustomerClient mikroservisi üzerinden güvenli bir şekilde müşteri bilgilerini sorgular.
     *
     * @param customerId Bilgileri getirilecek müşterinin ID'si
     * @return Müşteri detaylarını içeren {@link CustomerResponseDTO} nesnesi
     * @throws BusinessException Dış servise ulaşılamazsa veya müşteri bulunamazsa fırlatılır
     */
    private CustomerResponseDTO getCustomerSafely(Long customerId) {
        return FeignIntegrationUtil.executeSafely(() -> customerClient.getCustomerById(customerId), "Müşteri bulunamadı. Hatalı Müşteri ID: " + customerId, "Geçersiz müşteri talebi.", "Müşteri");
    }

    /**
     * ProductClient mikroservisi üzerinden güvenli bir şekilde sigorta ürün bilgilerini sorgular.
     *
     * @param productId Bilgileri getirilecek ürünün ID'si
     * @return Ürün detaylarını içeren {@link InsuranceProductResponseDTO} nesnesi
     * @throws BusinessException Dış servise ulaşılamazsa veya ürün bulunamazsa fırlatılır
     */
    private InsuranceProductResponseDTO getProductSafely(Long productId) {
        return FeignIntegrationUtil.executeSafely(() -> productClient.getProduct(productId), "Ürün bulunamadı. Hatalı Ürün ID: " + productId, "Geçersiz ürün talebi.", "Ürün");
    }
}