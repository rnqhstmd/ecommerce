package com.loopers.support.lock;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockService distributedLockService;

    @BeforeEach
    void setUp() {
        distributedLockService = new DistributedLockService(redissonClient);
    }

    @DisplayName("락 획득에 성공하면 supplier의 결과를 반환한다.")
    @Test
    void executeWithLock_success() throws InterruptedException {
        // arrange
        String key = "test:lock:1";
        given(redissonClient.getLock(key)).willReturn(rLock);
        given(rLock.tryLock(3L, 5L, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // act
        String result = distributedLockService.executeWithLock(
                key, 3L, 5L, TimeUnit.SECONDS, () -> "success"
        );

        // assert
        assertThat(result).isEqualTo("success");
        verify(rLock).unlock();
    }

    @DisplayName("락 획득에 성공하면 Runnable action을 실행한다.")
    @Test
    void executeWithLock_runnable_success() throws InterruptedException {
        // arrange
        String key = "test:lock:2";
        given(redissonClient.getLock(key)).willReturn(rLock);
        given(rLock.tryLock(3L, 5L, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        Runnable action = mock(Runnable.class);

        // act
        distributedLockService.executeWithLock(key, 3L, 5L, TimeUnit.SECONDS, action);

        // assert
        verify(action).run();
        verify(rLock).unlock();
    }

    @DisplayName("락 획득에 실패하면 CONFLICT 예외를 던진다.")
    @Test
    void executeWithLock_failsToAcquire_throwsConflict() throws InterruptedException {
        // arrange
        String key = "test:lock:fail";
        given(redissonClient.getLock(key)).willReturn(rLock);
        given(rLock.tryLock(3L, 5L, TimeUnit.SECONDS)).willReturn(false);

        // act & assert
        assertThatThrownBy(() ->
                distributedLockService.executeWithLock(
                        key, 3L, 5L, TimeUnit.SECONDS, () -> "result"
                )
        )
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.CONFLICT));
    }

    @DisplayName("락 획득 중 InterruptedException 발생 시 INTERNAL_ERROR 예외를 던진다.")
    @Test
    void executeWithLock_interrupted_throwsInternalError() throws InterruptedException {
        // arrange
        String key = "test:lock:interrupt";
        given(redissonClient.getLock(key)).willReturn(rLock);
        given(rLock.tryLock(3L, 5L, TimeUnit.SECONDS)).willThrow(new InterruptedException());

        // act & assert
        assertThatThrownBy(() ->
                distributedLockService.executeWithLock(
                        key, 3L, 5L, TimeUnit.SECONDS, () -> "result"
                )
        )
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR));

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // 스레드 인터럽트 상태 초기화
        Thread.interrupted();
    }

    @DisplayName("락 획득 후 현재 스레드가 보유하지 않으면 unlock하지 않는다.")
    @Test
    void executeWithLock_notHeldByCurrentThread_doesNotUnlock() throws InterruptedException {
        // arrange
        String key = "test:lock:nounlock";
        given(redissonClient.getLock(key)).willReturn(rLock);
        given(rLock.tryLock(3L, 5L, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(false);

        // act
        distributedLockService.executeWithLock(key, 3L, 5L, TimeUnit.SECONDS, () -> "ok");

        // assert
        verify(rLock, never()).unlock();
    }
}
