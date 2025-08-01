package com.genifast.dms.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.repository.UserRepository;

@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsCustom(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.genifast.dms.entity.User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_USER,
                        ErrorMessage.INVALID_USER.getMessage()));

        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Lấy quyền từ Roles (RBAC)
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        });

        // 2. Lấy quyền riêng của User (ABAC)
        user.getUserPermissions().forEach(userPermission -> {
            authorities.add(new SimpleGrantedAuthority(userPermission.getPermission().getName()));
        });

        return new User(user.getEmail(), user.getPassword(), authorities);
    }

}
