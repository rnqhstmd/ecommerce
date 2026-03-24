package com.loopers.interfaces.api.auth;

import com.loopers.application.auth.AuthFacade;
import com.loopers.application.auth.AuthInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthV1Controller {

    private final AuthFacade authFacade;

    @PostMapping("/signup")
    public ApiResponse<AuthV1Dto.AuthResponse> signup(
            @RequestBody @Valid AuthV1Dto.SignupRequest request
    ) {
        AuthInfo info = authFacade.signup(
                request.userId(), request.email(), request.birthDate(),
                request.gender(), request.password()
        );
        return ApiResponse.success(AuthV1Dto.AuthResponse.from(info));
    }

    @PostMapping("/login")
    public ApiResponse<AuthV1Dto.AuthResponse> login(
            @RequestBody @Valid AuthV1Dto.LoginRequest request
    ) {
        AuthInfo info = authFacade.login(request.userId(), request.password());
        return ApiResponse.success(AuthV1Dto.AuthResponse.from(info));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthV1Dto.AuthResponse> refresh(
            @RequestBody @Valid AuthV1Dto.RefreshRequest request
    ) {
        AuthInfo info = authFacade.refresh(request.refreshToken());
        return ApiResponse.success(AuthV1Dto.AuthResponse.from(info));
    }
}
