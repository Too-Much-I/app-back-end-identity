package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import web.tosunsaeng.identity.global.workload.WorkloadIdentityPurpose;

public interface WorkloadIdentityCredentialProvider {

	WorkloadIdentityCredential issue(WorkloadIdentityPurpose purpose);
}
