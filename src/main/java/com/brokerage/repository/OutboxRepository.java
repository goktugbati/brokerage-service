package com.brokerage.repository;

import com.brokerage.event.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.attempts < 5 ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
}
