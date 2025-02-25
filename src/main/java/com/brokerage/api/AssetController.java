package com.brokerage.api;

import com.brokerage.api.dto.response.ApiResponse;
import com.brokerage.api.dto.response.AssetListResponse;
import com.brokerage.api.dto.response.AssetResponse;
import com.brokerage.api.mapper.AssetMapper;
import com.brokerage.domain.Asset;
import com.brokerage.service.query.AssetQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assets", description = "Asset management API")
public class AssetController {

    private final AssetQueryService assetQueryService;
    private final CustomerHelper customerHelper;
    private final AssetMapper assetMapper;

    @GetMapping
    @Operation(summary = "List assets", description = "List all assets for the authenticated customer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AssetListResponse>> getAssets(
            @RequestParam(required = false) String assetName,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long customerId = customerHelper.getCustomerIdFromUserDetails(userDetails);
        log.info("Fetching assets for customer ID: {}", customerId);

        List<Asset> assets = assetQueryService.getAssetsByCustomerId(customerId);

        if (assetName != null && !assetName.isEmpty()) {
            assets = assets.stream()
                    .filter(asset -> asset.getAssetName().equalsIgnoreCase(assetName))
                    .collect(Collectors.toList());
        }

        List<AssetResponse> assetResponses = assetMapper.toResponseList(assets);

        AssetListResponse response = AssetListResponse.builder()
                .assets(assetResponses)
                .count(assetResponses.size())
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Assets retrieved successfully", response));
    }

    @GetMapping("/{assetName}")
    @Operation(summary = "Get asset", description = "Get a specific asset by name")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AssetResponse>> getAsset(
            @PathVariable String assetName,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long customerId = customerHelper.getCustomerIdFromUserDetails(userDetails);
        log.info("Fetching asset {} for customer ID: {}", assetName, customerId);

        Asset asset = assetQueryService.getAssetByCustomerIdAndAssetName(customerId, assetName);
        AssetResponse response = assetMapper.toResponse(asset);

        return ResponseEntity.ok(new ApiResponse<>(true, "Asset retrieved successfully", response));
    }
}