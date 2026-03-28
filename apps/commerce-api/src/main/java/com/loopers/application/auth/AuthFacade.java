package com.loopers.application.auth;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.support.auth.JwtTokenProvider;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AuthInfo signup(String userId, String email, String birthDate, Gender gender, String password) {
        String encodedPassword = passwordEncoder.encode(password);

        User user = userService.signUpWithPassword(userId, email, birthDate, gender, encodedPassword);

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserIdValue(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserIdValue());

        saveRefreshToken(user.getUserIdValue(), refreshToken);

        return new AuthInfo(accessToken, refreshToken, user.getUserIdValue());
    }

    public AuthInfo login(String userId, String password) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."));

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserIdValue(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserIdValue());

        saveRefreshToken(user.getUserIdValue(), refreshToken);

        return new AuthInfo(accessToken, refreshToken, user.getUserIdValue());
    }

    public AuthInfo refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(refreshToken);
        } catch (Exception e) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        String userId = claims.getSubject();
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserIdValue(), user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserIdValue());

        saveRefreshToken(userId, newRefreshToken);

        return new AuthInfo(newAccessToken, newRefreshToken, userId);
    }

    private void saveRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                jwtTokenProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );
    }
}
