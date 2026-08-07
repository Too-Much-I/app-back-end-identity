package web.tosunsaeng.identity.domain.user.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

@Schema(description = "회원 탈퇴 결과")
public record WithdrawResponse(
		@Schema(description = "탈퇴 처리된 사용자 상태", example = "WITHDRAWN")
		UserStatus status,
		@Schema(description = "서버가 기록한 회원 탈퇴 시각", format = "date-time")
		Instant withdrawnAt
) {

	public static WithdrawResponse from(User withdrawnUser) {
		if (withdrawnUser.getStatus() != UserStatus.WITHDRAWN) {
			throw new IllegalArgumentException("User must be withdrawn.");
		}
		return new WithdrawResponse(
				UserStatus.WITHDRAWN,
				withdrawnUser.getWithdrawnAt() != null
						? withdrawnUser.getWithdrawnAt()
						: withdrawnUser.getUpdatedAt()
		);
	}
}
