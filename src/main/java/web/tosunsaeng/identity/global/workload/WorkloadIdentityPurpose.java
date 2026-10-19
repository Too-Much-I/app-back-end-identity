package web.tosunsaeng.identity.global.workload;

public enum WorkloadIdentityPurpose {
	USER_WITHDRAWN("learning-core-user-withdrawn"),
	USER_MERGED("learning-core-user-merged");

	private final String audience;

	WorkloadIdentityPurpose(String audience) {
		this.audience = audience;
	}

	public String audience() {
		return audience;
	}
}
