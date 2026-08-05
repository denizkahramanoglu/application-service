package com.example.application_service.util;

import com.example.application_service.exception.BusinessException;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import java.util.function.Supplier;

public class FeignIntegrationUtil {

    private FeignIntegrationUtil() {
        // Utility sınıflarının instance'ı oluşturulmaz
    }

    /**
     * Feign çağrılarını ortak bir try-catch bloğunda sarar ve uygun BusinessException'ları fırlatır.
     */
    public static <T> T executeSafely(Supplier<T> clientCall, String notFoundMsg, String badRequestMsg, String serviceName) {
        try {
            T result = clientCall.get();
            if (result == null) {
                throw new BusinessException(notFoundMsg, HttpStatus.NOT_FOUND);
            }
            return result;
        } catch (FeignException.NotFound e) {
            throw new BusinessException(notFoundMsg, HttpStatus.NOT_FOUND);
        } catch (FeignException.BadRequest e) {
            throw new BusinessException(badRequestMsg, HttpStatus.BAD_REQUEST);
        } catch (FeignException e) {
            throw new BusinessException(serviceName + " servisi ile iletişim kurulamıyor. Lütfen daha sonra tekrar deneyin.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new BusinessException(serviceName + " işleminde beklenmeyen hata: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}