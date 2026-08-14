package web.tosunsaeng.identity.domain.auth.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.phone.PhoneFingerprint;

public class PhoneFingerprintAliasRepositoryImpl
		implements PhoneFingerprintAliasRepositoryCustom {

	private final MongoOperations mongoOperations;

	public PhoneFingerprintAliasRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(
				mongoOperations,
				"mongoOperations must not be null"
		);
	}

	@Override
	public List<PhoneFingerprintAlias> findAllActiveByFingerprints(
			Collection<PhoneFingerprint> fingerprints
	) {
		List<PhoneFingerprint> required = List.copyOf(
				Objects.requireNonNull(fingerprints, "fingerprints must not be null")
		);
		if (required.isEmpty()) {
			return List.of();
		}
		Criteria[] candidates = required.stream()
				.map(fingerprint -> Criteria.where("fingerprintKeyVersion")
						.is(fingerprint.keyVersion())
						.and("phoneFingerprint")
						.is(fingerprint.value()))
				.toArray(Criteria[]::new);
		Query query = Query.query(Criteria.where("status")
				.is(PhoneFingerprintAliasStatus.ACTIVE)
				.andOperator(new Criteria().orOperator(candidates)));
		return mongoOperations.find(query, PhoneFingerprintAlias.class);
	}

	@Override
	public long releaseAllActiveByPhoneIdentityId(
			String phoneIdentityId,
			Instant releasedAt
	) {
		Query query = Query.query(Criteria.where("phoneIdentityId")
				.is(Objects.requireNonNull(
						phoneIdentityId,
						"phoneIdentityId must not be null"
				))
				.and("status")
				.is(PhoneFingerprintAliasStatus.ACTIVE));
		Update update = new Update()
				.set("status", PhoneFingerprintAliasStatus.RELEASED)
				.set("releasedAt", Objects.requireNonNull(
						releasedAt,
						"releasedAt must not be null"
				));
		UpdateResult result = mongoOperations.updateMulti(
				query,
				update,
				PhoneFingerprintAlias.class
		);
		return result.getModifiedCount();
	}
}
