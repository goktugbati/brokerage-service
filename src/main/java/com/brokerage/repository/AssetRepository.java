// AssetRepository.java
package com.brokerage.repository;

import com.brokerage.domain.Asset;
import com.brokerage.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByCustomerId(Long customerId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Asset a WHERE a.customer.id = :customerId AND a.assetName = :assetName")
    Optional<Asset> findByCustomerIdAndAssetNameWithLock(
            @Param("customerId") Long customerId, 
            @Param("assetName") String assetName);
    
    Optional<Asset> findByCustomerIdAndAssetName(Long customerId, String assetName);
    
    @Query("SELECT a FROM Asset a WHERE a.customer = :customer AND a.assetName = :assetName")
    Optional<Asset> findByCustomerAndAssetName(
            @Param("customer") Customer customer, 
            @Param("assetName") String assetName);
}
