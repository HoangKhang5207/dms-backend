package com.genifast.dms.repository;

import com.genifast.dms.entity.Device;
import com.genifast.dms.entity.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    
    /**
     * Tìm device theo user ID
     */
    List<Device> findByUserId(Long userId);
    
    /**
     * Tìm device theo device type
     */
    List<Device> findByDeviceType(DeviceType deviceType);
    
    /**
     * Tìm device theo user ID và device type
     */
    List<Device> findByUserIdAndDeviceType(Long userId, DeviceType deviceType);
    
    /**
     * Tìm device theo MAC address
     */
    Optional<Device> findByMacAddress(String macAddress);
    
    /**
     * Tìm device theo IP address
     */
    List<Device> findByIpAddress(String ipAddress);
    
    /**
     * Tìm device đang active của user
     */
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.status = 1")
    List<Device> findActiveDevicesByUserId(@Param("userId") Long userId);
    
    /**
     * Kiểm tra device có tồn tại và active không
     */
    @Query("SELECT COUNT(d) > 0 FROM Device d WHERE d.id = :deviceId AND d.status = 1")
    boolean existsByIdAndActive(@Param("deviceId") String deviceId);
    
    /**
     * Tìm tất cả devices của một organization thông qua user
     */
    @Query("SELECT d FROM Device d WHERE d.user.organization.id = :organizationId")
    List<Device> findByOrganizationId(@Param("organizationId") Long organizationId);
}
