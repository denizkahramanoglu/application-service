package com.example.application_service.mapper;

import com.example.application_service.dto.*;
import com.example.application_service.entity.ApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ApplicationEntity toEntity(ApplicationRequestDTO dto);

    ApplicationResponseDTO toResponseDTO(ApplicationEntity entity);


        // Product Service'in beklediği PricingRequestDTO'yu 3 farklı kaynaktan harmanlayarak oluşturuyoruz
        @Mapping(target = "productId", source = "request.productId")
        @Mapping(target = "age", expression = "java(customer.getDateOfBirth() != null ? java.time.Period.between(customer.getDateOfBirth(), java.time.LocalDate.now()).getYears() : 0)")
        PricingRequestDTO toPricingRequest(CustomerResponseDTO customer, InsuranceProductResponseDTO product, ApplicationRequestDTO request);

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "price", source = "application.price")
    @Mapping(target = "createdAt", source = "application.createdAt")
    ApplicationDetailResponseDTO toDetailResponse(ApplicationEntity application, CustomerResponseDTO customer, InsuranceProductResponseDTO product);
}
