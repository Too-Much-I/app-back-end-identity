package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;

class PhoneIdentityTransactionServiceTests {

	private static final String USER_A = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String USER_B = "85a8c19e-5ab4-4945-bd1e-3a20ac269d9b";
	private static final Instant NOW = Instant.parse("2026-08-14T01:02:03Z");
	private static final PhoneFingerprint OLD = new PhoneFingerprint("v1", "A".repeat(43));
	private static final PhoneFingerprint ACTIVE = new PhoneFingerprint("v2", "B".repeat(43));

	@Test
	void createsIdentityAndEveryRetainedAlias() {
		PhoneIdentityRepository identities = mock(PhoneIdentityRepository.class);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		when(identities.findByUserIdAndStatus(USER_A, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(identities.save(any(PhoneIdentity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		PhoneIdentityLinkResult result = service(identities, aliases).linkOrReplace(
				USER_A,
				new PhoneFingerprintSet(ACTIVE, List.of(OLD, ACTIVE)),
				NOW
		);

		assertThat(result.outcome()).isEqualTo(PhoneIdentityLinkOutcome.CREATED);
		verify(aliases).saveAll(org.mockito.ArgumentMatchers.argThat(iterable -> {
			List<PhoneFingerprintAlias> saved = toList(iterable);
			return saved.size() == 2
					&& saved.stream().allMatch(alias -> alias.getUserId().equals(USER_A));
		}));
	}

	@Test
	void sameUserAndSameRetainedAliasesAreIdempotent() {
		PhoneIdentityRepository identities = mock(PhoneIdentityRepository.class);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		PhoneIdentity identity = PhoneIdentity.create(USER_A, ACTIVE, NOW);
		PhoneFingerprintAlias alias = alias(identity, ACTIVE);
		when(aliases.findAllActiveByFingerprints(List.of(ACTIVE))).thenReturn(List.of(alias));
		when(identities.findByUserIdAndStatus(USER_A, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.of(identity));
		when(aliases.findAllByPhoneIdentityIdAndStatus(
				identity.getPhoneIdentityId(),
				PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(alias));

		PhoneIdentityLinkResult result = service(identities, aliases).linkOrReplace(
				USER_A,
				new PhoneFingerprintSet(ACTIVE, List.of(ACTIVE)),
				NOW
		);

		assertThat(result.outcome()).isEqualTo(PhoneIdentityLinkOutcome.IDEMPOTENT);
		verify(identities, never()).save(any(PhoneIdentity.class));
		verify(aliases, never()).saveAll(any());
	}

	@Test
	void rotationBackfillsNewAliasAndMovesCurrentFingerprint() {
		PhoneIdentityRepository identities = mock(PhoneIdentityRepository.class);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		PhoneIdentity identity = PhoneIdentity.create(USER_A, OLD, NOW.minusSeconds(10));
		PhoneFingerprintAlias oldAlias = PhoneFingerprintAlias.create(
				identity.getPhoneIdentityId(),
				USER_A,
				OLD,
				NOW.minusSeconds(10)
		);
		when(aliases.findAllActiveByFingerprints(List.of(OLD, ACTIVE)))
				.thenReturn(List.of(oldAlias));
		when(identities.findByUserIdAndStatus(USER_A, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.of(identity));
		when(aliases.findAllByPhoneIdentityIdAndStatus(
				identity.getPhoneIdentityId(),
				PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(oldAlias));

		PhoneIdentityLinkResult result = service(identities, aliases).linkOrReplace(
				USER_A,
				new PhoneFingerprintSet(ACTIVE, List.of(OLD, ACTIVE)),
				NOW
		);

		assertThat(result.outcome()).isEqualTo(PhoneIdentityLinkOutcome.ROTATED);
		assertThat(identity.hasCurrentFingerprint(ACTIVE)).isTrue();
		verify(aliases).saveAll(org.mockito.ArgumentMatchers.argThat(iterable -> {
			List<PhoneFingerprintAlias> saved = toList(iterable);
			return saved.size() == 1 && saved.getFirst().matches(ACTIVE);
		}));
		verify(identities).save(identity);
	}

	@Test
	void anotherOwnerIsRejectedWithoutMutationOrAutomaticMerge() {
		PhoneIdentityRepository identities = mock(PhoneIdentityRepository.class);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		PhoneIdentity otherIdentity = PhoneIdentity.create(USER_B, ACTIVE, NOW);
		when(aliases.findAllActiveByFingerprints(List.of(ACTIVE)))
				.thenReturn(List.of(alias(otherIdentity, ACTIVE)));

		assertThatThrownBy(() -> service(identities, aliases).linkOrReplace(
				USER_A,
				new PhoneFingerprintSet(ACTIVE, List.of(ACTIVE)),
				NOW
		))
				.isInstanceOfSatisfying(AuthException.class, exception -> assertThat(
						exception.getErrorCode()
				).isEqualTo(AuthErrorStatus.PHONE_ALREADY_LINKED));
		verify(identities, never()).save(any(PhoneIdentity.class));
		verify(aliases, never()).saveAll(any());
		verify(aliases, never()).releaseAllActiveByPhoneIdentityId(any(), any());
	}

	@Test
	void replacementReleasesOldOwnershipBeforeCreatingNewIdentity() {
		PhoneIdentityRepository identities = mock(PhoneIdentityRepository.class);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		PhoneIdentity oldIdentity = PhoneIdentity.create(USER_A, OLD, NOW.minusSeconds(10));
		when(aliases.findAllActiveByFingerprints(List.of(ACTIVE))).thenReturn(List.of());
		when(identities.findByUserIdAndStatus(USER_A, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.of(oldIdentity));
		when(aliases.releaseAllActiveByPhoneIdentityId(oldIdentity.getPhoneIdentityId(), NOW))
				.thenReturn(1L);
		when(identities.save(any(PhoneIdentity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		PhoneIdentityLinkResult result = service(identities, aliases).linkOrReplace(
				USER_A,
				new PhoneFingerprintSet(ACTIVE, List.of(ACTIVE)),
				NOW
		);

		assertThat(result.outcome()).isEqualTo(PhoneIdentityLinkOutcome.REPLACED);
		assertThat(oldIdentity.getStatus()).isEqualTo(PhoneIdentityStatus.RELEASED);
		verify(aliases).releaseAllActiveByPhoneIdentityId(
				oldIdentity.getPhoneIdentityId(),
				NOW
		);
		verify(aliases).saveAll(any());
	}

	@Test
	void replacementPublishesRevokedThenVerifiedRevisionsInSameOperation() {
		PhoneIdentityRepository identities = mock(PhoneIdentityRepository.class);
		PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
		PhoneEligibilityBindingRevisionRepository revisions = mock(
				PhoneEligibilityBindingRevisionRepository.class);
		PhoneEligibilityBindingOutboxRepository outbox = mock(
				PhoneEligibilityBindingOutboxRepository.class);
		PhoneIdentity oldIdentity = PhoneIdentity.create(USER_A, OLD, NOW.minusSeconds(10));
		when(aliases.findAllActiveByFingerprints(List.of(ACTIVE))).thenReturn(List.of());
		when(identities.findByUserIdAndStatus(USER_A, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.of(oldIdentity));
		when(aliases.releaseAllActiveByPhoneIdentityId(oldIdentity.getPhoneIdentityId(), NOW))
				.thenReturn(1L);
		when(identities.save(any(PhoneIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		PhoneEligibilityBindingRevision active = mock(PhoneEligibilityBindingRevision.class);
		when(active.isActive()).thenReturn(true);
		when(active.getRevision()).thenReturn(1L);
		PhoneEligibilityBindingRevision revoked = mock(PhoneEligibilityBindingRevision.class);
		when(revoked.getRevision()).thenReturn(2L);
		PhoneEligibilityBindingRevision verified = mock(PhoneEligibilityBindingRevision.class);
		when(verified.getRevision()).thenReturn(3L);
		when(revisions.findByUserIdAndConsumerScopeId(USER_A, "opaque-scope-v1"))
				.thenReturn(Optional.of(active));
		when(revisions.advanceRevoked(USER_A, "opaque-scope-v1", 1L, NOW))
				.thenReturn(Optional.of(revoked));
		when(revisions.advanceVerified(USER_A, "opaque-scope-v1", NOW)).thenReturn(verified);

		new PhoneIdentityTransactionService(identities, aliases, revisions, outbox).linkOrReplace(
				USER_A,
				new PhoneFingerprintSet(ACTIVE, List.of(ACTIVE)),
				"opaque-scope-v1",
				List.of(new PhoneEligibilityFingerprintCandidate("v1", "C".repeat(43))),
				NOW
		);

		verify(outbox, org.mockito.Mockito.times(2)).save(any(PhoneEligibilityBindingOutbox.class));
	}

	private PhoneIdentityTransactionService service(
			PhoneIdentityRepository identities,
			PhoneFingerprintAliasRepository aliases
	) {
		return new PhoneIdentityTransactionService(identities, aliases);
	}

	private PhoneFingerprintAlias alias(
			PhoneIdentity identity,
			PhoneFingerprint fingerprint
	) {
		return PhoneFingerprintAlias.create(
				identity.getPhoneIdentityId(),
				identity.getUserId(),
				fingerprint,
				NOW
		);
	}

	private List<PhoneFingerprintAlias> toList(
			Iterable<PhoneFingerprintAlias> iterable
	) {
		java.util.ArrayList<PhoneFingerprintAlias> result = new java.util.ArrayList<>();
		iterable.forEach(result::add);
		return result;
	}
}
