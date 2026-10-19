package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;

public final class AbandonedFirebaseCleanupException extends RuntimeException {

	private final AbandonedFirebaseEnrollmentFailureCode failureCode;

	public AbandonedFirebaseCleanupException(
			AbandonedFirebaseEnrollmentFailureCode failureCode
	) {
		super("Abandoned Firebase cleanup failed.");
		this.failureCode = Objects.requireNonNull(failureCode);
	}

	public AbandonedFirebaseEnrollmentFailureCode failureCode() {
		return failureCode;
	}
}
