package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

public interface WorkloadIdentityCredentialProvider {

	WorkloadIdentityCredential issue(String audience);
}
