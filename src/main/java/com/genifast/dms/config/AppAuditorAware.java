package com.genifast.dms.config;

import com.genifast.dms.common.utils.JwtUtils;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component("appAuditorAware")
public class AppAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // Tận dụng JwtUtils đã có để lấy email của người dùng đang đăng nhập
        return JwtUtils.getCurrentUserLogin();
    }
}