package com.genifast.dms.repository;

import com.genifast.dms.entity.RefreshToken;
import com.genifast.dms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface cho RefreshToken entity.
 * Chuyển đổi từ IRefreshTokenRepository của Golang.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm RefreshToken bằng chính chuỗi token của nó.
     * Tương ứng FindRefreshTokenByRefreshTokenString.
     */
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    /**
     * Tìm RefreshToken bằng đối tượng User liên kết.
     * Tương ứng FindRefreshTokenByUserID.
     */
    Optional<RefreshToken> findByUser(User user);
}