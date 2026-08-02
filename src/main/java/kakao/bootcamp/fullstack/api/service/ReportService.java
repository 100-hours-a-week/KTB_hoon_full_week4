package kakao.bootcamp.fullstack.api.service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.report.Report;
import kakao.bootcamp.fullstack.api.domain.report.ReportErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PostReportReqDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.report.ReportRepository;
import kakao.bootcamp.fullstack.api.service.report.ReportTargetHandler;
import kakao.bootcamp.fullstack.global.exception.ConflictException;
import kakao.bootcamp.fullstack.global.exception.InternalServerException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final List<ReportTargetHandler> handlers;

    private Map<TargetType, ReportTargetHandler> handlerMap;

    @PostConstruct
    void registerAndVerifyHandlers() {
        handlerMap =
                handlers.stream()
                        .collect(
                                Collectors.toMap(
                                        ReportTargetHandler::getTargetType,
                                        Function.identity(),
                                        (existing, duplicate) -> {
                                            throw new IllegalStateException(
                                                    "Duplicate ReportTargetHandler for: "
                                                            + existing.getTargetType());
                                        }));
        // 갭(핸들러 없는 타입)도 기동 시점에 실패시킨다.
        for (TargetType type : TargetType.values()) {
            if (!handlerMap.containsKey(type)) {
                throw new IllegalStateException("No ReportTargetHandler registered for: " + type);
            }
        }
    }

    @Transactional
    public void report(Long memberId, PostReportReqDto request) {
        checkMemberExists(memberId);
        checkNotAlreadyReported(request.targetId(), request.targetType(), memberId);
        Report report =
                Report.create(
                        request.targetId(), request.targetType(), memberId, request.reportReason());
        reportRepository.save(report);
        resolveHandler(request.targetType()).handleReported(request.targetId());
    }

    private ReportTargetHandler resolveHandler(TargetType targetType) {
        ReportTargetHandler handler = handlerMap.get(targetType);
        if (handler == null) {
            throw new InternalServerException(
                    CommonErrorCode.HANDLER_NOT_FOUND,
                    "No ReportTargetHandler registered for: " + targetType);
        }
        return handler;
    }

    private void checkMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new UnauthorizedException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void checkNotAlreadyReported(Long targetId, TargetType targetType, Long memberId) {
        if (reportRepository.existsByTargetAndMember(targetId, targetType, memberId)) {
            throw new ConflictException(ReportErrorCode.ALREADY_REPORTED);
        }
    }
}
