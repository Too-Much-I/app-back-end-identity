package web.tosunsaeng.identity.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import web.tosunsaeng.identity.config.SecurityConfig;

@WebMvcTest(JwksController.class)
@Import({SecurityConfig.class, JwksControllerTests.TestJwkConfiguration.class})
class JwksControllerTests {

	private static final String KEY_ID = "identity-test-rsa-key";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsUnauthenticatedStandardPublicJwksWithoutPrivateParameters() throws Exception {
		MvcResult result = mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.keys").isArray())
				.andExpect(jsonPath("$.keys.length()").value(1))
				.andExpect(jsonPath("$.keys[0].kty").value("RSA"))
				.andExpect(jsonPath("$.keys[0].use").value("sig"))
				.andExpect(jsonPath("$.keys[0].alg").value("RS256"))
				.andExpect(jsonPath("$.keys[0].kid").value(KEY_ID))
				.andExpect(jsonPath("$.keys[0].n").isNotEmpty())
				.andExpect(jsonPath("$.keys[0].e").value("AQAB"))
				.andExpect(jsonPath("$.keys[0].d").doesNotExist())
				.andExpect(jsonPath("$.keys[0].p").doesNotExist())
				.andExpect(jsonPath("$.keys[0].q").doesNotExist())
				.andExpect(jsonPath("$.keys[0].dp").doesNotExist())
				.andExpect(jsonPath("$.keys[0].dq").doesNotExist())
				.andExpect(jsonPath("$.keys[0].qi").doesNotExist())
				.andExpect(jsonPath("$.keys[0].oth").doesNotExist())
				.andExpect(jsonPath("$.isSuccess").doesNotExist())
				.andExpect(jsonPath("$.code").doesNotExist())
				.andExpect(jsonPath("$.message").doesNotExist())
				.andExpect(jsonPath("$.result").doesNotExist())
				.andReturn();

		assertThat(result.getResponse().getContentType()).startsWith("application/json");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestJwkConfiguration {

		@Bean
		RSAKey testRsaKey() {
			KeyPair keyPair = generateRsaKeyPair();
			return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
					.privateKey((RSAPrivateKey) keyPair.getPrivate())
					.keyID(KEY_ID)
					.algorithm(JWSAlgorithm.RS256)
					.keyUse(KeyUse.SIGNATURE)
					.build();
		}

		private KeyPair generateRsaKeyPair() {
			try {
				KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
				generator.initialize(2048);
				return generator.generateKeyPair();
			} catch (NoSuchAlgorithmException exception) {
				throw new IllegalStateException("RSA is not available in the test runtime.");
			}
		}
	}
}
