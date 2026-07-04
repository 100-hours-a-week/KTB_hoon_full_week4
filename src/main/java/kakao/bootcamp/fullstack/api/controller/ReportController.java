package kakao.bootcamp.fullstack.api.controller;

import jakarta.validation.Valid;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.api.dto.request.PostReportReqDto;
import kakao.bootcamp.fullstack.api.service.ReportService;
import kakao.bootcamp.fullstack.global.exception.code.SuccessCode;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.LoginMember;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/report")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReport(
            @LoginMember AuthMember authMember,
            @Valid @RequestBody PostReportReqDto request){
        reportService.report(authMember.memberId(), request);
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.CREATED));
    }
}
