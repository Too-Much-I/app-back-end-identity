package web.tosunsaeng.identity.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.common.response.BaseResponse;
import web.tosunsaeng.identity.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.user.service.UserProfileService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserProfileService userProfileService;

	public UserController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@Operation(
			summary = "내 프로필 조회",
			description = "검증된 Access Token의 subject에 해당하는 사용자 프로필을 조회합니다."
	)
	@GetMapping("/me")
	public BaseResponse<UserProfileResponse> getMe() {
		return BaseResponse.success(userProfileService.getCurrentUserProfile());
	}
}
