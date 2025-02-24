package com.brokerage.service.query;

import com.brokerage.domain.Asset;
import com.brokerage.exception.AssetNotFoundException;
import com.brokerage.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetQueryService {

    private final AssetRepository assetRepository;
    
    /**
     * Get all assets for a customer
     */
    @Cacheable(value = "customerAssets", key = "#customerId")
    public List<Asset> getAssetsByCustomerId(Long customerId) {
        log.debug("Fetching assets for customer ID: {}", customerId);
        return assetRepository.findByCustomerId(customerId);
    }
    
    /**
     * Get a specific asset for a customer
     */
    @Cacheable(value = "assets", key = "#customerId + '-' + #assetName")
    public Asset getAssetByCustomerIdAndAssetName(Long customerId, String assetName) {
        log.debug("Fetching asset {} for customer ID: {}", assetName, customerId);
        return assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                .orElseThrow(() -> new AssetNotFoundException(
                    String.format("Asset %s not found for customer ID %d", assetName, customerId)));
    }
    
    /**
     * Get an asset by ID
     */
    @Cacheable(value = "assets", key = "#assetId")
    public Asset getAssetById(Long assetId) {
        log.debug("Fetching asset by ID: {}", assetId);
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException("Asset not found with ID: " + assetId));
    }
}