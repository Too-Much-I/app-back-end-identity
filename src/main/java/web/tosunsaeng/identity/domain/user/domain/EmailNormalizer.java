package web.tosunsaeng.identity.domain.user.domain;

import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class EmailNormalizer {

	public String normalize(String email) {
		return Objects.requireNonNull(email, "email must not be null")
				.trim()
				.toLowerCase(Locale.ROOT);
	}
}
