package web.tosunsaeng.identity.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTests {

	@Test
	void providesBcryptPasswordEncoder() {
		PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();

		assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
	}
}
