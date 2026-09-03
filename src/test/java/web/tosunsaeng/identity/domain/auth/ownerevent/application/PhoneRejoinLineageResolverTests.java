package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneRejoinLineage;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneRejoinLineageStatus;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class PhoneRejoinLineageResolverTests {
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
	private static final String SOURCE = "00000000-0000-4000-8000-000000000001";
	private static final String TARGET = "00000000-0000-4000-8000-000000000002";
	private static final String PHONE_ID = "00000000-0000-4000-8000-000000000003";
	private static final String WITHDRAWAL_ID = "00000000-0000-4000-8000-000000000004";
	private static final String SCOPE = "FREE_EXAM_ONCE";

	@Test
	void consumesOnlyExactAvailablePredecessorAndCreatesBillingEvent() {
		Fixture fixture = fixture();
		PhoneFingerprintAlias alias = mock(PhoneFingerprintAlias.class);
		when(alias.getPhoneIdentityId()).thenReturn(PHONE_ID);
		when(fixture.aliases.findAllByFingerprintsAndStatus(any(), any()))
				.thenReturn(List.of(alias));
		PhoneRejoinLineage lineage = PhoneRejoinLineage.available(
				WITHDRAWAL_ID, SOURCE, PHONE_ID, SCOPE, 2, NOW.minusSeconds(60));
		when(fixture.lineages.findAllBySourcePhoneIdentityIdInAndConsumerScopeIdAndStatus(
				any(), any(), any())).thenReturn(List.of(lineage));
		User sourceUser = mock(User.class);
		when(sourceUser.getStatus()).thenReturn(UserStatus.WITHDRAWN);
		when(fixture.users.findById(SOURCE)).thenReturn(Optional.of(sourceUser));
		User targetUser = mock(User.class);
		when(targetUser.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(targetUser.isMember()).thenReturn(true);
		when(fixture.users.findById(TARGET)).thenReturn(Optional.of(targetUser));
		UserWithdrawalLifecycle lifecycle = mock(UserWithdrawalLifecycle.class);
		when(lifecycle.getStatus()).thenReturn(UserWithdrawalCleanupStatus.CLEANED);
		when(lifecycle.getUserId()).thenReturn(SOURCE);
		when(fixture.lifecycles.findById(WITHDRAWAL_ID)).thenReturn(Optional.of(lifecycle));
		PhoneEligibilityBindingRevision sourceRevision = revision(2, false);
		PhoneEligibilityBindingRevision targetRevision = revision(1, true);
		when(fixture.revisions.findByUserIdAndConsumerScopeId(SOURCE, SCOPE))
				.thenReturn(Optional.of(sourceRevision));
		when(fixture.revisions.findByUserIdAndConsumerScopeId(TARGET, SCOPE))
				.thenReturn(Optional.of(targetRevision));
		OwnerEventCore event = OwnerEventCore.trialOwnerRebindApproved(
				SOURCE, TARGET, SCOPE, 2, 1, lineage.getLineageId(), NOW);
		when(fixture.capture.captureTrialOwnerRebind(
				SOURCE, TARGET, SCOPE, 2, 1, lineage.getLineageId(), NOW)).thenReturn(event);

		assertThat(fixture.resolver.resolve(fingerprints(), SCOPE, TARGET, NOW)).isSameAs(event);
		assertThat(lineage.getStatus()).isEqualTo(PhoneRejoinLineageStatus.CONSUMED);
		assertThat(lineage.getTargetUserId()).isEqualTo(TARGET);
		verify(fixture.lineages).save(lineage);
	}

	@Test
	void multipleAvailablePredecessorsFailClosedWithoutBlockingSignup() {
		Fixture fixture = fixture();
		PhoneFingerprintAlias firstAlias = mock(PhoneFingerprintAlias.class);
		PhoneFingerprintAlias secondAlias = mock(PhoneFingerprintAlias.class);
		when(firstAlias.getPhoneIdentityId()).thenReturn(PHONE_ID);
		when(secondAlias.getPhoneIdentityId()).thenReturn("00000000-0000-4000-8000-000000000005");
		when(fixture.aliases.findAllByFingerprintsAndStatus(any(), any()))
				.thenReturn(List.of(firstAlias, secondAlias));
		PhoneRejoinLineage first = PhoneRejoinLineage.available(
				WITHDRAWAL_ID, SOURCE, PHONE_ID, SCOPE, 2, NOW.minusSeconds(60));
		PhoneRejoinLineage second = PhoneRejoinLineage.available(
				"00000000-0000-4000-8000-000000000006",
				"00000000-0000-4000-8000-000000000007",
				"00000000-0000-4000-8000-000000000005", SCOPE, 2, NOW.minusSeconds(30));
		when(fixture.lineages.findAllBySourcePhoneIdentityIdInAndConsumerScopeIdAndStatus(
				any(), any(), any())).thenReturn(List.of(first, second));

		assertThat(fixture.resolver.resolve(fingerprints(), SCOPE, TARGET, NOW)).isNull();
		assertThat(first.getStatus()).isEqualTo(PhoneRejoinLineageStatus.RECONCILIATION_REQUIRED);
		assertThat(second.getStatus()).isEqualTo(PhoneRejoinLineageStatus.RECONCILIATION_REQUIRED);
		verify(fixture.capture, never()).captureTrialOwnerRebind(
				any(), any(), any(), anyLong(), anyLong(), any(), any());
	}

	private Fixture fixture() {
		OwnerEventProperties properties = new OwnerEventProperties();
		properties.setTrialRebindCaptureEnabled(true);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		PhoneRejoinLineageRepository lineages = mock(PhoneRejoinLineageRepository.class);
		PhoneEligibilityBindingRevisionRepository revisions = mock(PhoneEligibilityBindingRevisionRepository.class);
		UserRepository users = mock(UserRepository.class);
		UserWithdrawalLifecycleRepository lifecycles = mock(UserWithdrawalLifecycleRepository.class);
		OwnerEventCaptureService capture = mock(OwnerEventCaptureService.class);
		return new Fixture(new PhoneRejoinLineageResolver(properties, aliases, lineages,
				revisions, users, lifecycles, capture), aliases, lineages, revisions,
				users, lifecycles, capture);
	}

	private PhoneEligibilityBindingRevision revision(long value, boolean active) {
		PhoneEligibilityBindingRevision revision = mock(PhoneEligibilityBindingRevision.class);
		when(revision.getRevision()).thenReturn(value);
		when(revision.isActive()).thenReturn(active);
		return revision;
	}

	private PhoneFingerprintSet fingerprints() {
		PhoneFingerprint fingerprint = new PhoneFingerprint("v1", "A".repeat(43));
		return new PhoneFingerprintSet(fingerprint, List.of(fingerprint));
	}

	private record Fixture(
			PhoneRejoinLineageResolver resolver,
			PhoneFingerprintAliasRepository aliases,
			PhoneRejoinLineageRepository lineages,
			PhoneEligibilityBindingRevisionRepository revisions,
			UserRepository users,
			UserWithdrawalLifecycleRepository lifecycles,
			OwnerEventCaptureService capture
	) {}
}
