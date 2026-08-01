package kakao.bootcamp.fullstack.api.controller;

import jakarta.validation.Valid;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResult;
import kakao.bootcamp.fullstack.api.service.AuthService;
import kakao.bootcamp.fullstack.global.constants.AuthCookieConstants;
import kakao.bootcamp.fullstack.global.exception.code.SuccessCode;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import kakao.bootcamp.fullstack.global.utils.RefreshTokenCookieFactory;
import kakao.bootcamp.fullstack.global.utils.TokenExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResDto>> login(@Valid @RequestBody LoginReqDto request) {
        LoginResult result = authService.login(request);
        ResponseCookie refreshTokenCookie =
                RefreshTokenCookieFactory.create(
                        result.refreshToken(), result.refreshTokenMaxAgeSeconds());
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(
                        ApiResponse.success(
                                SuccessCode.SUCCESS, new LoginResDto(result.accessToken())));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResDto>> reissue(
            @CookieValue(value = AuthCookieConstants.REFRESH_TOKEN_COOKIE, required = false)
                    String refreshToken) {
        LoginResult result = authService.reissue(refreshToken);
        ResponseCookie refreshTokenCookie =
                RefreshTokenCookieFactory.create(
                        result.refreshToken(), result.refreshTokenMaxAgeSeconds());
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(
                        ApiResponse.success(
                                SuccessCode.SUCCESS, new LoginResDto(result.accessToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @CookieValue(value = AuthCookieConstants.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
            ) {
        String accessToken = TokenExtractor.extractBearerToken(authorizationHeader);
        authService.logout(accessToken, refreshToken);
        ResponseCookie logoutCookie = RefreshTokenCookieFactory.delete();
        return ResponseEntity.status(SuccessCode.SUCCESS.getHttpStatus())
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString())
                .body(ApiResponse.success(SuccessCode.SUCCESS));
    }
}
