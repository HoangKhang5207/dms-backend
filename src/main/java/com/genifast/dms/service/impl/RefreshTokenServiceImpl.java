package com.genifast.dms.service.impl;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.CommonUtils;
import com.genifast.dms.entity.RefreshToken;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.RefreshTokenRepository;
import com.genifast.dms.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public String createRefreshToken(User user) {
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken = null;
        String refreshTokenString = CommonUtils.randomString(128);

        if (!rtOpt.isPresent()) {
            refreshToken = refreshTokenRepository.save(RefreshToken.builder()
                    .user(user)
                    .refreshToken(refreshTokenString)
                    .build());
        } else {
            refreshToken = rtOpt.get();
            refreshToken.setRefreshToken(refreshTokenString);
            refreshToken = refreshTokenRepository.save(refreshToken);
        }

        return refreshToken.getRefreshToken();
    }

    @Override
    public User verifyAndGetUser(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(token)
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND,
                        ErrorMessage.REFRESH_TOKEN_NOT_FOUND.getMessage()));

        return refreshToken.getUser();
    }
}