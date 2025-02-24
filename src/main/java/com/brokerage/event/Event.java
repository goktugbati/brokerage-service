package com.brokerage.event;

import java.time.LocalDateTime;

public interface Event {
    String getEventId();
    String getEventType();
    LocalDateTime getTimestamp();
}
