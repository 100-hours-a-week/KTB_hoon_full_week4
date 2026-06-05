package kakao.bootcamp.fullstack.api.controller;


import jakarta.validation.Valid;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResDto;
import kakao.bootcamp.fullstack.api.service.AuthService;
import kakao.bootcamp.fullstack.global.constants.JwtConstants;
import kakao.bootcamp.fullstack.global.exception.code.SuccessCode;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResDto>> login(@Valid @RequestBody LoginReqDto request){
        LoginResDto response = authService.login(request);
        return ResponseEntity
                .status(SuccessCode.LOGIN_SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestAttribute(JwtConstants.ACCESS_TOKEN_ATTRIBUTE) String accessToken
    ) {
        authService.logout(accessToken);
        return ResponseEntity
                .status(SuccessCode.LOGOUT_SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.LOGOUT_SUCCESS));
    }
}
