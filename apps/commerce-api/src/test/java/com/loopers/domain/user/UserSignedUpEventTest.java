package com.loopers.domain.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserSignedUpEventTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원가입 시 도메인 이벤트를 통해 포인트가 자동 생성된다.")
    @Test
    void pointIsCreatedAfterSignUp() {
        // act
        userService.signUp("eventuser", "event@mail.com", "1990-01-01", Gender.MALE);

        // assert
        Point point = pointService.getPoint("eventuser");
        assertThat(point).isNotNull();
        assertThat(point.getBalanceValue()).isEqualTo(0L);
    }
}
