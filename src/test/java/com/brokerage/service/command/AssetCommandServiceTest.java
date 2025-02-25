package com.brokerage.service.command;

import com.brokerage.domain.Asset;
import com.brokerage.domain.Customer;
import com.brokerage.domain.OrderSide;
import com.brokerage.exception.AssetNotFoundException;
import com.brokerage.exception.InsufficientAssetsException;
import com.brokerage.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetCommandServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetCommandService assetCommandService;

    private Customer testCustomer;
    private Asset tryAsset;
    private Asset otherAsset;
    private static final String TRY_ASSET_NAME = "TRY";
    private static final String OTHER_ASSET_NAME = "AAPL"; // Used when testing selling assets

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .isAdmin(false)
                .build();

        tryAsset = Asset.builder()
                .id(1L)
                .customer(testCustomer)
                .assetName(TRY_ASSET_NAME)
                .size(BigDecimal.valueOf(10000))
                .usableSize(BigDecimal.valueOf(10000))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        otherAsset = Asset.builder()
                .id(2L)
                .customer(testCustomer)
                .assetName(OTHER_ASSET_NAME)
                .size(BigDecimal.valueOf(100))
                .usableSize(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createOrUpdateAsset_WhenAssetExists_ShouldUpdateAsset() {
        when(assetRepository.findByCustomerAndAssetName(any(Customer.class), anyString()))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(tryAsset);
        BigDecimal additionalSize = BigDecimal.valueOf(1000);

        Asset result = assetCommandService.createOrUpdateAsset(testCustomer, TRY_ASSET_NAME, additionalSize);

        assertNotNull(result);
        assertEquals(TRY_ASSET_NAME, result.getAssetName());

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());

        Asset capturedAsset = assetCaptor.getValue();
        assertEquals(BigDecimal.valueOf(11000), capturedAsset.getSize());
        assertEquals(BigDecimal.valueOf(11000), capturedAsset.getUsableSize());
    }

    @Test
    void createOrUpdateAsset_WhenAssetDoesNotExist_ShouldCreateNewAsset() {
        when(assetRepository.findByCustomerAndAssetName(any(Customer.class), anyString()))
                .thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset savedAsset = invocation.getArgument(0);
            savedAsset.setId(3L);
            return savedAsset;
        });
        BigDecimal initialSize = BigDecimal.valueOf(500);

        Asset result = assetCommandService.createOrUpdateAsset(testCustomer, "NEW_ASSET", initialSize);

        assertNotNull(result);
        assertEquals("NEW_ASSET", result.getAssetName());
        assertEquals(BigDecimal.valueOf(500), result.getSize());
        assertEquals(BigDecimal.valueOf(500), result.getUsableSize());

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());

        Asset capturedAsset = assetCaptor.getValue();
        assertEquals(testCustomer, capturedAsset.getCustomer());
        assertEquals("NEW_ASSET", capturedAsset.getAssetName());
        assertEquals(BigDecimal.valueOf(500), capturedAsset.getSize());
        assertEquals(BigDecimal.valueOf(500), capturedAsset.getUsableSize());
    }

    @Test
    void reserveTryForBuyOrder_WhenSufficientFunds_ShouldReserveTry() {
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(TRY_ASSET_NAME)))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(tryAsset);

        BigDecimal size = BigDecimal.valueOf(5);
        BigDecimal price = BigDecimal.valueOf(100);

        assetCommandService.reserveTryForBuyOrder(1L, size, price);

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());

        Asset capturedAsset = assetCaptor.getValue();
        assertEquals(BigDecimal.valueOf(10000), capturedAsset.getSize());
        assertEquals(BigDecimal.valueOf(9500), capturedAsset.getUsableSize());
    }

    @Test
    void reserveTryForBuyOrder_WhenInsufficientFunds_ShouldThrowException() {
        tryAsset.setUsableSize(BigDecimal.valueOf(400)); // Not enough for 500 cost
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(TRY_ASSET_NAME)))
                .thenReturn(Optional.of(tryAsset));
        BigDecimal size = BigDecimal.valueOf(5);
        BigDecimal price = BigDecimal.valueOf(100);
        assertThrows(InsufficientAssetsException.class, () -> {
            assetCommandService.reserveTryForBuyOrder(1L, size, price);
        });
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void reserveTryForBuyOrder_WhenAssetNotFound_ShouldThrowException() {
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(TRY_ASSET_NAME)))
                .thenReturn(Optional.empty());

        BigDecimal size = BigDecimal.valueOf(5);
        BigDecimal price = BigDecimal.valueOf(100);
        assertThrows(AssetNotFoundException.class, () -> {
            assetCommandService.reserveTryForBuyOrder(1L, size, price);
        });
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void reserveAssetForSellOrder_WhenSufficientAssets_ShouldReserveAsset() {
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(OTHER_ASSET_NAME)))
                .thenReturn(Optional.of(otherAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(otherAsset);

        BigDecimal size = BigDecimal.valueOf(50);

        assetCommandService.reserveAssetForSellOrder(1L, OTHER_ASSET_NAME, size);

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());

        Asset capturedAsset = assetCaptor.getValue();
        assertEquals(BigDecimal.valueOf(100), capturedAsset.getSize()); // Size remains unchanged
        assertEquals(BigDecimal.valueOf(50), capturedAsset.getUsableSize()); // 100 - 50
    }

    @Test
    void reserveAssetsForOrder_ForBuyOrder_ShouldReserveTry() {
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq("TRY")))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(tryAsset);

        assetCommandService.reserveAssetsForOrder(1L, "TRY", OrderSide.BUY, BigDecimal.valueOf(5), BigDecimal.valueOf(100));

        verify(assetRepository).findByCustomerIdAndAssetName(eq(1L), eq("TRY"));
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void finalizeBuyOrder_ShouldDecreaseTryAndIncreaseAsset() {
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(OTHER_ASSET_NAME)))
                .thenReturn(Optional.of(otherAsset));
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(TRY_ASSET_NAME)))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(otherAsset).thenReturn(tryAsset);

        BigDecimal size = BigDecimal.valueOf(10);
        BigDecimal price = BigDecimal.valueOf(50);

        assetCommandService.finalizeBuyOrder(1L, OTHER_ASSET_NAME, size, price);

        verify(assetRepository, times(2)).save(any(Asset.class));
    }

    @Test
    void finalizeSellOrder_ShouldDecreaseAssetAndIncreaseTry() {
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(OTHER_ASSET_NAME)))
                .thenReturn(Optional.of(otherAsset));
        when(assetRepository.findByCustomerIdAndAssetName(anyLong(), eq(TRY_ASSET_NAME)))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.save(any(Asset.class))).thenReturn(otherAsset).thenReturn(tryAsset);

        BigDecimal size = BigDecimal.valueOf(10);
        BigDecimal price = BigDecimal.valueOf(50);

        assetCommandService.finalizeSellOrder(1L, OTHER_ASSET_NAME, size, price);

        verify(assetRepository, times(2)).save(any(Asset.class));
    }
}