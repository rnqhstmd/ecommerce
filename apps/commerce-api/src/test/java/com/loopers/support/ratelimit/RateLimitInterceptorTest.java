package com.loopers.support.ratelimit;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(true, 60, 60);
        interceptor = new RateLimitInterceptor(redisTemplate, properties);
    }

    @DisplayName("rate limit이 비활성화되면 요청을 허용한다.")
    @Test
    void preHandle_disabled_allowsRequest() {
        // arrange
        RateLimitProperties disabledProperties = new RateLimitProperties(false, 60, 60);
        RateLimitInterceptor disabledInterceptor = new RateLimitInterceptor(redisTemplate, disabledProperties);

        // act
        boolean result = disabledInterceptor.preHandle(request, response, new Object());

        // assert
        assertThat(result).isTrue();
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @DisplayName("요청 횟수가 제한 이내이면 허용한다.")
    @Test
    void preHandle_withinLimit_allowsRequest() {
        // arrange
        given(request.getHeader("X-USER-ID")).willReturn("user1");
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rate:limit:user:user1")), eq("60")))
                .willReturn(1L);

        // act
        boolean result = interceptor.preHandle(request, response, new Object());

        // assert
        assertThat(result).isTrue();
    }

    @DisplayName("요청 횟수가 제한을 초과하면 TOO_MANY_REQUESTS 예외를 던진다.")
    @Test
    void preHandle_exceedsLimit_throwsTooManyRequests() {
        // arrange
        given(request.getHeader("X-USER-ID")).willReturn("user1");
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rate:limit:user:user1")), eq("60")))
                .willReturn(61L);

        // act & assert
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.TOO_MANY_REQUESTS));
    }

    @DisplayName("X-USER-ID가 있으면 user 기반 키를 생성한다.")
    @Test
    void resolveIdentifier_withUserId_generatesUserKey() {
        // arrange
        given(request.getHeader("X-USER-ID")).willReturn("testuser");
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rate:limit:user:testuser")), eq("60")))
                .willReturn(1L);

        // act
        interceptor.preHandle(request, response, new Object());

        // assert
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("rate:limit:user:testuser")), eq("60"));
    }

    @DisplayName("X-USER-ID가 없으면 remoteAddr 기반 IP 키를 생성한다.")
    @Test
    void resolveIdentifier_withoutUserId_generatesIpKeyFromRemoteAddr() {
        // arrange
        given(request.getHeader("X-USER-ID")).willReturn(null);
        given(request.getRemoteAddr()).willReturn("192.168.1.1");
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rate:limit:ip:192.168.1.1")), eq("60")))
                .willReturn(1L);

        // act
        interceptor.preHandle(request, response, new Object());

        // assert
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("rate:limit:ip:192.168.1.1")), eq("60"));
    }
}
