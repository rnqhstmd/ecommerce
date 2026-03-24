package com.loopers.interfaces.api.auth;

import com.loopers.application.auth.AuthInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthV1Dto {

    public record SignupRequest(
            @NotBlank(message = "ID는 필수입니다.")
            @Pattern(regexp = "^[a-zA-Z0-9]{1,10}$", message = "ID는 영문 및 숫자 10자 이내여야 합니다.")
            String userId,

            @NotBlank(message = "이메일은 필수입니다.")
            @Pattern(regexp = "^[^@]+@[^@]+\\.[^@]+$", message = "이메일은 xx@yy.zz 형식이어야 합니다.")
            String email,

            @NotBlank(message = "생년월일은 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다.")
            String birthDate,

            @NotBlank(message = "성별은 필수입니다.")
            String gender,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "ID는 필수입니다.")
            String userId,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "리프레시 토큰은 필수입니다.")
            String refreshToken
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String userId
    ) {
        public static AuthResponse from(AuthInfo info) {
            return new AuthResponse(info.accessToken(), info.refreshToken(), info.userId());
        }
    }
}
