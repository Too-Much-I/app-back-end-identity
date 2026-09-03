package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventWireMapper;

class OwnerEventCoreTests {
	private static final String SOURCE = "00000000-0000-4000-8000-000000000001";
	private static final String TARGET = "00000000-0000-4000-8000-000000000002";
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

	@Test
	void userMergedCreatesExactlyTwoConsumersAndKeepsWireV1() {
		OwnerEventCore event = OwnerEventCore.userMerged(SOURCE, TARGET, NOW);
		String json = new String(new OwnerEventWireMapper(
				new ObjectMapper().findAndRegisterModules()).serialize(event), StandardCharsets.UTF_8);

		assertThat(event.getRequiredConsumers()).isEqualTo(Set.of(
				OwnerEventConsumer.BILLING, OwnerEventConsumer.LEARNING_CORE));
		assertThat(json).contains("\"schemaVersion\":1", "\"sourceUserId\":\"" + SOURCE + "\"")
				.doesNotContain("eventType", "producer", "consumerScopeId", "phoneRejoinLineageId");
	}

	@Test
	void trialRebindIsBillingOnlyAndDoesNotExposeInternalLineage() {
		OwnerEventCore event = OwnerEventCore.trialOwnerRebindApproved(
				SOURCE, TARGET, "FREE_EXAM_ONCE", 2, 1,
				"00000000-0000-4000-8000-000000000003", NOW);
		String json = new String(new OwnerEventWireMapper(
				new ObjectMapper().findAndRegisterModules()).serialize(event), StandardCharsets.UTF_8);

		assertThat(event.getRequiredConsumers()).containsExactly(OwnerEventConsumer.BILLING);
		assertThat(json).contains(
				"\"eventType\":\"TrialOwnerRebindApproved\"",
				"\"lifecycleReason\":\"PHONE_REJOIN\"",
				"\"sourceBindingRevision\":2",
				"\"targetBindingRevision\":1")
				.doesNotContain("phoneRejoinLineageId");
	}

	@Test
	void rejectsSameSourceAndTarget() {
		assertThatThrownBy(() -> OwnerEventCore.userMerged(SOURCE, SOURCE, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
