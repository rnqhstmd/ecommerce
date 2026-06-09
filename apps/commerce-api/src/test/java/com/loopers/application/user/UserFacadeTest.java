package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFacadeTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserFacade userFacade;

    @DisplayName("회원가입하면 UserService.signUp을 command의 각 필드로 호출하고 UserInfo를 반환한다.")
    @Test
    void signUp_delegatesToUserService() {
        // given
        UserCommand command = new UserCommand("testuser", "test@example.com", "2000-01-01", Gender.MALE);
        User user = User.create("testuser", "test@example.com", "2000-01-01", Gender.MALE);
        when(userService.signUp("testuser", "test@example.com", "2000-01-01", Gender.MALE))
                .thenReturn(user);

        // when
        UserInfo result = userFacade.signUp(command);

        // then
        verify(userService, times(1)).signUp("testuser", "test@example.com", "2000-01-01", Gender.MALE);
        assertThat(result.userId()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.gender()).isEqualTo(Gender.MALE);
    }

    @DisplayName("사용자 정보를 조회하면 UserService.getUserByUserId를 호출하고 UserInfo를 반환한다.")
    @Test
    void getUserInfo_delegatesToUserService() {
        // given
        User user = User.create("testuser", "test@example.com", "2000-01-01", Gender.FEMALE);
        when(userService.getUserByUserId("testuser")).thenReturn(user);

        // when
        UserInfo result = userFacade.getUserInfo("testuser");

        // then
        verify(userService, times(1)).getUserByUserId("testuser");
        assertThat(result.userId()).isEqualTo("testuser");
        assertThat(result.gender()).isEqualTo(Gender.FEMALE);
    }
}
