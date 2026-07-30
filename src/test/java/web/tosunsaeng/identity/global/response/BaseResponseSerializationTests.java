package web.tosunsaeng.identity.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.global.exception.CommonErrorStatus;

class BaseResponseSerializationTests {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void serializesSuccessResponse() throws Exception {
		BaseResponse<Map<String, String>> response = BaseResponse.success(Map.of("status", "ready"));

		JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(json.get("isSuccess").asBoolean()).isTrue();
		assertThat(json.get("code").asText()).isEqualTo("SUCCESS");
		assertThat(json.get("message").asText()).isEqualTo("요청에 성공했습니다.");
		assertThat(json.get("result").get("status").asText()).isEqualTo("ready");
	}

	@Test
	void serializesFailureResponse() throws Exception {
		BaseResponse<Void> response = BaseResponse.failure(CommonErrorStatus.NOT_FOUND);

		JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(json.get("isSuccess").asBoolean()).isFalse();
		assertThat(json.get("code").asText()).isEqualTo("NOT_FOUND");
		assertThat(json.get("message").asText()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
		assertThat(json.get("result").isNull()).isTrue();
	}
}
