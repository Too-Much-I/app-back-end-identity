package web.tosunsaeng.identity.domain.support;

import java.io.IOException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.StreamReadFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import web.tosunsaeng.identity.global.response.BaseResponse;

@RestController
@RequestMapping("/api/v1/support/inquiries")
public class SupportController {
    public record Receipt(String inquiryId, String status) { }
    private final ObjectProvider<SupportService> services;
    private final SupportActorResolver actors;
    private final SupportClientAddress addresses;
    private final ObjectReader reader;
    private final Validator validator;
    public SupportController(ObjectProvider<SupportService> services, SupportActorResolver actors,
            SupportProperties props, ObjectMapper mapper, Validator validator) {
        this.services = services; this.actors = actors; this.addresses = new SupportClientAddress(props.trustedProxies());
        this.validator = validator;
        ObjectMapper strict = mapper.copy();
        strict.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        strict.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        strict.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        strict.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature());
        reader = strict.readerFor(SupportRequest.class);
    }
    @Operation(summary="문의 접수", description="AUTH/GENERAL은 로그인 선택, REFUND는 인증된 사용자 ID 필수. userId는 본문이 아닌 서버 인증으로 결정합니다. 인증 헤더가 있으면 검증하며 잘못된 인증을 익명으로 전환하지 않습니다. 동일 키/본문 재전송은 기존 접수 결과를 반환합니다.",
        requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true, content=@Content(schema=@Schema(implementation=SupportRequest.class))),
        responses={@ApiResponse(responseCode="201", description="DB 접수 완료, Slack 전송은 비동기"),
            @ApiResponse(responseCode="200", description="동일 문의 재전송"), @ApiResponse(responseCode="400", description="요청/식별자 오류"),
            @ApiResponse(responseCode="401", description="인증 실패"), @ApiResponse(responseCode="403", description="비활성 계정"),
            @ApiResponse(responseCode="409", description="동일 키 다른 내용"), @ApiResponse(responseCode="413", description="16KiB 초과"),
            @ApiResponse(responseCode="429", description="요청 제한"), @ApiResponse(responseCode="503", description="비활성 또는 저장 장애")})
    @PostMapping(consumes="application/json", produces="application/json")
    public ResponseEntity<BaseResponse<Receipt>> submit(
            @Parameter(required=true, description="문의별 소문자 UUID v4. 재시도 시 동일 값 유지")
            @RequestHeader(value="Idempotency-Key", required=false) String key,
            Authentication authentication, HttpServletRequest http) {
        SupportService service = services.getIfAvailable();
        if (service == null) throw SupportError.SUPPORT_INQUIRY_UNAVAILABLE.exception();
        String ip = addresses.resolve(http.getRemoteAddr(), http.getHeader("X-Forwarded-For"));
        service.burst(ip);
        var actor = actors.resolve(authentication);
        SupportRequest request;
        try {
            byte[] body = http.getInputStream().readNBytes(16 * 1024 + 1);
            if (body.length > 16 * 1024) throw SupportError.SUPPORT_INQUIRY_TOO_LARGE.exception();
            request = reader.readValue(body);
        } catch (IOException | IllegalArgumentException e) { throw SupportError.INVALID_REQUEST.exception(); }
        if (request == null || !validator.validate(request).isEmpty()) throw SupportError.INVALID_REQUEST.exception();
        actor.requireFor(request.category());
        var result = service.submit(actor, ip, key, request);
        return ResponseEntity.status(result.replay() ? 200 : 201).header("Cache-Control", "no-store")
                .body(BaseResponse.success(new Receipt(result.inquiryId(), "RECEIVED")));
    }
}
