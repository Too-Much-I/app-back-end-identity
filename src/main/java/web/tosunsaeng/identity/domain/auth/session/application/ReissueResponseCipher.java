package web.tosunsaeng.identity.domain.auth.session.application;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.domain.RefreshReissueResponse;

public interface ReissueResponseCipher {
	RefreshReissueResponse encrypt(RefreshSession source, ReissueResult result);
	ReissueResult decrypt(RefreshSession source, RefreshReissueResponse document);
}
