package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import java.util.Objects;

public final class PhoneNumberNormalizer {

	private static final int MIN_E164_DIGITS = 8;
	private static final int MAX_E164_DIGITS = 15;

	public String normalize(String verifiedPhoneNumber) {
		String input = Objects.requireNonNull(
				verifiedPhoneNumber,
				"verifiedPhoneNumber must not be null"
		).trim();
		if (input.isEmpty() || input.charAt(0) != '+') {
			throw invalid();
		}

		StringBuilder digits = new StringBuilder(MAX_E164_DIGITS);
		for (int index = 1; index < input.length(); index++) {
			char character = input.charAt(index);
			if (Character.isDigit(character)) {
				if (character > '9') {
					throw invalid();
				}
				digits.append(character);
				continue;
			}
			if (!isFormattingCharacter(character)) {
				throw invalid();
			}
		}
		if (digits.length() < MIN_E164_DIGITS
				|| digits.length() > MAX_E164_DIGITS
				|| digits.charAt(0) == '0') {
			throw invalid();
		}
		return "+" + digits;
	}

	private static boolean isFormattingCharacter(char value) {
		return value == ' '
				|| value == '-'
				|| value == '('
				|| value == ')'
				|| value == '.';
	}

	private static IllegalArgumentException invalid() {
		return new IllegalArgumentException("Verified phone number is not valid E.164 input.");
	}
}
