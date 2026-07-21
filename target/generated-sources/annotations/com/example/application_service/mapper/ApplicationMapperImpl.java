package com.example.application_service.mapper;

import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.dto.ApplicationResponseDTO;
import com.example.application_service.entity.ApplicationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-20T23:02:25+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 24.0.1 (Oracle Corporation)"
)
@Component
public class ApplicationMapperImpl implements ApplicationMapper {

    @Override
    public ApplicationEntity toEntity(ApplicationRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ApplicationEntity.ApplicationEntityBuilder applicationEntity = ApplicationEntity.builder();

        applicationEntity.customerId( dto.getCustomerId() );

        return applicationEntity.build();
    }

    @Override
    public ApplicationResponseDTO toResponseDTO(ApplicationEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ApplicationResponseDTO.ApplicationResponseDTOBuilder applicationResponseDTO = ApplicationResponseDTO.builder();

        applicationResponseDTO.id( entity.getId() );
        applicationResponseDTO.customerId( entity.getCustomerId() );
        applicationResponseDTO.productId( entity.getProductId() );
        applicationResponseDTO.price( entity.getPrice() );
        applicationResponseDTO.createdAt( entity.getCreatedAt() );
        applicationResponseDTO.updatedAt( entity.getUpdatedAt() );

        return applicationResponseDTO.build();
    }
}
