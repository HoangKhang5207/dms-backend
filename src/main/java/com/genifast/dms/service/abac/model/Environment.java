package com.genifast.dms.service.abac.model;

import com.genifast.dms.entity.enums.DeviceType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Thuộc tính môi trường trong ABAC
 */
@Data
@Builder
public class Environment {
    private Instant currentTime;
    private String ipAddress;
    private DeviceType deviceType;
    private String deviceId;
    private String sessionId;
    private String userAgent;
    
    public boolean isBusinessHours() {
        if (currentTime == null) return true;
        
        // Giờ hành chính: 8:00 - 17:00, thứ 2 - thứ 6
        java.time.LocalDateTime localTime = java.time.LocalDateTime.ofInstant(
            currentTime, java.time.ZoneId.systemDefault());
        
        int hour = localTime.getHour();
        int dayOfWeek = localTime.getDayOfWeek().getValue();
        
        return dayOfWeek >= 1 && dayOfWeek <= 5 && hour >= 8 && hour <= 17;
    }
    
    public boolean isCompanyDevice() {
        return DeviceType.COMPANY_DEVICE.equals(deviceType);
    }
}
