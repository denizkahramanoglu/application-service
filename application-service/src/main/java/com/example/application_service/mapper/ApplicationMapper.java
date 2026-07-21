package com.example.application_service.mapper;

import com.example.application_service.dto.ApplicationRequestDTO;
import com.example.application_service.dto.ApplicationResponseDTO;
import com.example.application_service.entity.ApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)

    ApplicationEntity toEntity(ApplicationRequestDTO dto);

    ApplicationResponseDTO toResponseDTO(ApplicationEntity entity);

}