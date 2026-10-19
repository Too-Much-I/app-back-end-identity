package web.tosunsaeng.identity.domain.auth.session.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import web.tosunsaeng.identity.domain.auth.session.domain.RefreshReissueResponse;

public interface RefreshReissueResponseRepository extends MongoRepository<RefreshReissueResponse, String> { }
