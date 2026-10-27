package web.tosunsaeng.identity.domain.user.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.bson.Document;

import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.dto.response.ConsentPolicyStatusResponse;
import web.tosunsaeng.identity.domain.user.exception.UserException;

import static org.assertj.core.api.Assertions.*;

class MarketingPushConsentTests {
	private static final Instant NOW = Instant.parse("2026-10-27T03:00:00Z");
	private static final String VERSION = "marketing-push-v1";

	@Test
	void explicitConsentIsVersionedAndIdempotentAndNullPreservesIt() {
		UserConsents initial = UserConsents.unconsented();
		assertThat(initial.isMarketingPushConsented()).isFalse();
		assertThat(initial.renewMarketingPush(null, null, NOW)).isSameAs(initial);
		assertThat(initial.renewMarketingPush(false, null, NOW)).isSameAs(initial);
		UserConsents optedIn = initial.renewMarketingPush(true, VERSION, NOW);
		assertThat(initial.isMarketingPushConsented()).isFalse();
		assertThat(optedIn.getMarketingPushConsentedAt()).isEqualTo(NOW);
		assertThat(optedIn.getMarketingPushConsentVersion()).isEqualTo(VERSION);
		assertThat(optedIn.renewMarketingPush(true, VERSION, NOW.plusSeconds(1))).isSameAs(optedIn);
		assertThat(optedIn.renewMarketingPush(null, null, NOW.plusSeconds(1))).isSameAs(optedIn);
	}

	@Test
	void requiredAndQualityRenewalsNeverEraseMarketingChoice() {
		UserConsents initial = UserConsents.unconsented().renewMarketingPush(true, VERSION, NOW);
		UserConsents required = initial.renewRequiredConsents("privacy-v2", "term-v2", NOW.plusSeconds(1));
		UserConsents quality = required.renew("privacy-v3", "term-v3", true, "quality-v1", NOW.plusSeconds(2));
		assertThat(required.isMarketingPushConsented()).isTrue();
		assertThat(quality.isMarketingPushConsented()).isTrue();
		assertThat(quality.getMarketingPushConsentedAt()).isEqualTo(NOW);
	}

	@Test
	void withdrawalRetainsEvidenceButIsNeverEffectiveConsent() {
		UserConsents consent = UserConsents.unconsented().renewMarketingPush(true, VERSION, NOW);
		UserConsents withdrawn = consent.renewMarketingPush(false, null, NOW.plusSeconds(5));
		assertThat(withdrawn.isMarketingPushConsented()).isFalse();
		assertThat(withdrawn.getMarketingPushConsentedAt()).isEqualTo(NOW);
		assertThat(withdrawn.getMarketingPushRevokedAt()).isEqualTo(NOW.plusSeconds(5));
		assertThat(withdrawn.renewMarketingPush(false, null, NOW.plusSeconds(6))).isSameAs(withdrawn);
		var status = ConsentPolicyStatusResponse.optionalOf(VERSION, withdrawn.isMarketingPushConsented(),
				withdrawn.getMarketingPushConsentVersion(), withdrawn.getMarketingPushConsentedAt());
		assertThat(status.consented()).isFalse();
		assertThat(status.requiresConsent()).isFalse();
		UserConsents renewed = withdrawn.renewMarketingPush(true, "marketing-push-v2", NOW.plusSeconds(7));
		assertThat(renewed.getMarketingPushConsentedAt()).isEqualTo(NOW.plusSeconds(7));
		assertThat(ConsentPolicyStatusResponse.optionalOf("marketing-push-v3", true,
				renewed.getMarketingPushConsentVersion(), renewed.getMarketingPushConsentedAt()).consented()).isFalse();
	}

	@Test
	void onlyOptInRequiresCurrentPolicyVersion() {
		ConsentPolicy policy = new ConsentPolicy("privacy-v1", "term-v1", "quality-v1", VERSION);
		policy.validateMarketingPush(null, null);
		policy.validateMarketingPush(false, null);
		policy.validateMarketingPush(true, " marketing-push-v1 ");
		for (String version : new String[] {null, "", "marketing-push-v0"}) {
			assertThatThrownBy(() -> policy.validateMarketingPush(true, version)).isInstanceOf(UserException.class);
		}
	}

	@Test
	void mongoMappingDefaultsLegacyDocumentsToFalseAndRoundTripsNewFields() throws Exception {
		MongoMappingContext context = new MongoMappingContext();
		var conversions = org.springframework.data.mongodb.core.convert.MongoCustomConversions.create(adapter -> { });
		context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		context.afterPropertiesSet();
		MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
		converter.setCustomConversions(conversions);
		converter.afterPropertiesSet();
		assertThat(converter.read(UserConsents.class, new Document()).isMarketingPushConsented()).isFalse();
		UserConsents source = UserConsents.unconsented().renewMarketingPush(true, VERSION, NOW)
				.renewMarketingPush(false, null, NOW.plusSeconds(5));
		Document document = new Document();
		converter.write(source, document);
		UserConsents restored = converter.read(UserConsents.class, document);
		assertThat(restored.isMarketingPushConsented()).isFalse();
		assertThat(restored.getMarketingPushConsentVersion()).isEqualTo(VERSION);
		assertThat(restored.getMarketingPushConsentedAt()).isEqualTo(NOW);
		assertThat(restored.getMarketingPushRevokedAt()).isEqualTo(NOW.plusSeconds(5));
	}
}
