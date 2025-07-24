package com.genifast.dms.service;

import com.genifast.dms.entity.User;

public interface RefreshTokenService {
    String createRefreshToken(User user);

    User verifyAndGetUser(String token);
}
