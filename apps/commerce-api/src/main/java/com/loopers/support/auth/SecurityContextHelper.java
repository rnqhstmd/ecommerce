package com.loopers.support.auth;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        return (String) authentication.getPrincipal();
    }

    public static String getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof String userId
                && !"anonymousUser".equals(userId)) {
            return userId;
        }
        return null;
    }
}
