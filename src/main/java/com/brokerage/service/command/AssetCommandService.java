package com.brokerage.service.command;

import com.brokerage.domain.Asset;
import com.brokerage.domain.Customer;
import com.brokerage.domain.OrderSide;
import com.brokerage.exception.AssetNotFoundException;
import com.brokerage.exception.InsufficientAssetsException;
import com.brokerage.repository.AssetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCommandService {

    private final AssetRepository assetRepository;
    private static final String TRY_ASSET = "TRY";

    /**
     * Creates or updates an asset for a customer
     */
    @Transactional
    @CacheEvict(value = {"assets", "customerAssets"}, allEntries = true)
    public Asset createOrUpdateAsset(Customer customer, String assetName, BigDecimal size) {
        return assetRepository.findByCustomerAndAssetName(customer, assetName)
                .map(existingAsset -> {
                    existingAsset.setSize(existingAsset.getSize().add(size));
                    existingAsset.setUsableSize(existingAsset.getUsableSize().add(size));
                    return assetRepository.save(existingAsset);
                })
                .orElseGet(() -> {
                    Asset newAsset = Asset.builder()
                            .customer(customer)
                            .assetName(assetName)
                            .size(size)
                            .usableSize(size)
                            .build();
                    return assetRepository.save(newAsset);
                });
    }

    /**
     * Reserves assets for a new order
     */
    @Transactional
    @CacheEvict(value = {"assets", "customerAssets"}, allEntries = true)
    public void reserveAssetsForOrder(Long customerId, String assetName, OrderSide side,
                                      BigDecimal size, BigDecimal price) {
        if (OrderSide.BUY.equals(side)) {
            reserveTryForBuyOrder(customerId, size, price);
        } else if (OrderSide.SELL.equals(side)) {
            reserveAssetForSellOrder(customerId, assetName, size);
        }
    }

    /**
     * Releases reserved assets when an order is cancelled
     */
    @Transactional
    @CacheEvict(value = {"assets", "customerAssets"}, allEntries = true)
    public void releaseReservedAssets(Long customerId, String assetName, OrderSide side,
                                      BigDecimal size, BigDecimal price) {
        if (OrderSide.BUY.equals(side)) {
            releaseTryForBuyOrder(customerId, size, price);
        } else if (OrderSide.SELL.equals(side)) {
            releaseAssetForSellOrder(customerId, assetName, size);
        }
    }

    /**
     * Updates assets when an order is matched
     */
    @Transactional
    @CacheEvict(value = {"assets", "customerAssets"}, allEntries = true)
    public void updateAssetsForMatchedOrder(Long customerId, String assetName, OrderSide side,
                                            BigDecimal size, BigDecimal price) {
        if (OrderSide.BUY.equals(side)) {
            finalizeBuyOrder(customerId, assetName, size, price);
        } else if (OrderSide.SELL.equals(side)) {
            finalizeSellOrder(customerId, assetName, size, price);
        }
    }

    /**
     * Reserves TRY for a buy order
     */
    @Transactional
    public void reserveTryForBuyOrder(Long customerId, BigDecimal size, BigDecimal price) {
        BigDecimal totalCost = size.multiply(price);

        Asset tryAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, TRY_ASSET)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have TRY asset"));

        if (tryAsset.getUsableSize().compareTo(totalCost) < 0) {
            throw new InsufficientAssetsException("Insufficient TRY balance for buy order");
        }

        tryAsset.setUsableSize(tryAsset.getUsableSize().subtract(totalCost));
        assetRepository.save(tryAsset);

        log.debug("Reserved {} TRY for customer ID {}", totalCost, customerId);
    }

    /**
     * Reserves asset for a sell order
     */
    @Transactional
    public void reserveAssetForSellOrder(Long customerId, String assetName, BigDecimal size) {
        Asset asset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, assetName)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have " + assetName + " asset"));

        if (asset.getUsableSize().compareTo(size) < 0) {
            throw new InsufficientAssetsException("Insufficient " + assetName + " balance for sell order");
        }

        asset.setUsableSize(asset.getUsableSize().subtract(size));
        assetRepository.save(asset);

        log.debug("Reserved {} {} for customer ID {}", size, assetName, customerId);
    }

    /**
     * Releases TRY for a cancelled buy order
     */
    @Transactional
    public void releaseTryForBuyOrder(Long customerId, BigDecimal size, BigDecimal price) {
        BigDecimal totalCost = size.multiply(price);

        Asset tryAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, TRY_ASSET)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have TRY asset"));

        tryAsset.setUsableSize(tryAsset.getUsableSize().add(totalCost));
        assetRepository.save(tryAsset);

        log.debug("Released {} TRY for customer ID {}", totalCost, customerId);
    }

    /**
     * Releases asset for a cancelled sell order
     */
    @Transactional
    public void releaseAssetForSellOrder(Long customerId, String assetName, BigDecimal size) {
        Asset asset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, assetName)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have " + assetName + " asset"));

        asset.setUsableSize(asset.getUsableSize().add(size));
        assetRepository.save(asset);

        log.debug("Released {} {} for customer ID {}", size, assetName, customerId);
    }

    /**
     * Finalizes a buy order by adding the bought asset to customer's portfolio
     */
    @Transactional
    public void finalizeBuyOrder(Long customerId, String assetName, BigDecimal size, BigDecimal price) {
        BigDecimal totalCost = size.multiply(price);

        Asset asset = assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                .orElseGet(() -> {
                    Asset newAsset = Asset.builder()
                            .customer(Customer.builder().id(customerId).build())
                            .assetName(assetName)
                            .size(BigDecimal.ZERO)
                            .usableSize(BigDecimal.ZERO)
                            .build();
                    return assetRepository.save(newAsset);
                });

        // Update TRY asset
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, TRY_ASSET)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have TRY asset"));

        // Already reserved during order creation, so just reduce the total size
        tryAsset.setSize(tryAsset.getSize().subtract(totalCost));
        assetRepository.save(tryAsset);

        // Update the asset being bought
        asset.setSize(asset.getSize().add(size));
        asset.setUsableSize(asset.getUsableSize().add(size));
        assetRepository.save(asset);

        log.debug("Finalized buy order of {} {} for customer ID {}", size, assetName, customerId);
    }

    /**
     * Finalizes a sell order by adding TRY to customer's account
     */
    @Transactional
    public void finalizeSellOrder(Long customerId, String assetName, BigDecimal size, BigDecimal price) {
        BigDecimal totalValue = size.multiply(price);

        // Update the asset being sold
        Asset asset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, assetName)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have " + assetName + " asset"));

        // Already reserved during order creation, so just reduce the total size
        asset.setSize(asset.getSize().subtract(size));
        assetRepository.save(asset);

        // Update TRY asset
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(customerId, TRY_ASSET)
                .orElseThrow(() -> new AssetNotFoundException("Customer does not have TRY asset"));

        tryAsset.setSize(tryAsset.getSize().add(totalValue));
        tryAsset.setUsableSize(tryAsset.getUsableSize().add(totalValue));
        assetRepository.save(tryAsset);

        log.debug("Finalized sell order of {} {} for {} TRY for customer ID {}",
                size, assetName, totalValue, customerId);
    }
}