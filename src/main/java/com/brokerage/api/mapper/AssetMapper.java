package com.brokerage.api.mapper;

import com.brokerage.api.dto.response.AssetResponse;
import com.brokerage.domain.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssetMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "assetName", source = "assetName")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "usableSize", source = "usableSize")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    AssetResponse toResponse(Asset asset);

    List<AssetResponse> toResponseList(List<Asset> assets);
}