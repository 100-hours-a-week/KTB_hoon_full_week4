package kakao.bootcamp.fullstack.api.controller;

import jakarta.validation.Valid;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;
import kakao.bootcamp.fullstack.api.dto.response.MemberProfileResDto;
import kakao.bootcamp.fullstack.api.service.MemberService;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.LoginMember;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kakao.bootcamp.fullstack.global.exception.code.SuccessCode.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupReqDto request) {
        memberService.signup(request);
        return ResponseEntity.status(CREATED.getHttpStatus())
                .body(ApiResponse.success(CREATED));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MemberProfileResDto>> getProfile(@LoginMember AuthMember authMember) {
        MemberProfileResDto response = memberService.getMemberProfile(authMember.memberId());
        return ResponseEntity.status(SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SUCCESS, response));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @LoginMember AuthMember authMember,
            @Valid @RequestBody ProfileUpdateReqDto request) {
        memberService.updateMemberProfile(authMember.memberId(), request);
        return ResponseEntity.status(SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SUCCESS));
    }

    @PatchMapping("/profile/pw")
    public ResponseEntity<ApiResponse<Void>> updateProfilePw(
            @LoginMember AuthMember authMember, @Valid @RequestBody PasswordUpdateReqDto request){
        memberService.updatePassword(authMember.memberId(), request);
        return ResponseEntity.status(SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SUCCESS));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> withdrawal(@LoginMember AuthMember authMember) {
        memberService.deleteMember(authMember.memberId());
        return ResponseEntity.status(SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SUCCESS));
    }
}
