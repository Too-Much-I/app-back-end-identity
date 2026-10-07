package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.federation.application.*;

class AccountRecoveryServiceTests {
	static final Instant NOW = Instant.parse("2026-10-03T00:00:00Z");
	static final String ID = "11111111-1111-4111-8111-111111111111";
	static final String RING = "v1:" + Base64.getEncoder().encodeToString(new byte[32]);
	RecoveryStore store = mock(RecoveryStore.class);
	FirebaseAuthenticationVerifier verifier = mock(FirebaseAuthenticationVerifier.class);
	RecoveryAccountResolver resolver = mock(RecoveryAccountResolver.class);
	RecoveryProperties properties = new RecoveryProperties(true, RING, null, null, null, 0, 0, 0);
	AccountRecoveryService service = new AccountRecoveryService(store, new RecoveryHasher(RING), verifier, resolver, properties, Clock.fixed(NOW, ZoneOffset.UTC));
	VerifiedFirebasePrincipal proof(FirebaseAuthenticationMethod method, Instant authTime, Instant issuedAt) {
		return new VerifiedFirebasePrincipal("project", "uid", method, authTime, issuedAt, NOW.plusSeconds(300),
				false, true, "+16505550123", Set.of(FirebaseAuthenticationMethod.PHONE), List.of());
	}
	@BeforeEach void setup() {
		when(verifier.verify(anyString(), eq(FirebaseVerificationPurpose.ACCOUNT_RECOVERY)))
				.thenReturn(proof(FirebaseAuthenticationMethod.PHONE, NOW.minusSeconds(10), NOW));
		when(store.lookup(anyString(), anyList(), any(), any(), any(), any())).thenAnswer(i -> ((Supplier<?>)i.getArgument(5)).get());
		when(resolver.resolve("+16505550123")).thenReturn(RecoveryResult.notFound());
	}
	@Test void prepareDoesNotRevealAccountOrCallFirebase() {
		var result = service.prepare("127.0.0.1");
		assertThat(UUID.fromString(result.recoveryId())).isNotNull();
		assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(600));
		verify(store).admit(anyList(), eq(60), eq(15), eq(NOW));
		verifyNoInteractions(verifier, resolver);
		verify(store).prepare(result.recoveryId(), NOW, result.expiresAt());
	}
	@Test void lookupUsesPhoneOnlyAndNeverIssuesIdentityTokens() {
		assertThat(service.lookup(ID, "test-proof", "127.0.0.1").status()).isEqualTo(RecoveryResult.Status.NOT_FOUND);
		verify(verifier).verify("test-proof", FirebaseVerificationPurpose.ACCOUNT_RECOVERY);
		verify(store, times(3)).admit(anyList(), anyInt(), anyInt(), eq(NOW));
	}
	@Test void tokenRefreshKeepsProofIdentityButDifferentSmsAuthDoesNot() {
		service.lookup(ID, "proof-1", "127.0.0.1");
		when(verifier.verify(anyString(), any())).thenReturn(proof(FirebaseAuthenticationMethod.PHONE, NOW.minusSeconds(10), NOW.plusSeconds(1)));
		service.lookup(ID, "proof-2", "127.0.0.1");
		when(verifier.verify(anyString(), any())).thenReturn(proof(FirebaseAuthenticationMethod.PHONE, NOW.minusSeconds(9), NOW));
		service.lookup(ID, "proof-3", "127.0.0.1");
		var hashes = org.mockito.ArgumentCaptor.forClass(List.class);
		verify(store, times(3)).lookup(eq(ID), hashes.capture(), any(), any(), any(), any());
		assertThat(hashes.getAllValues().get(0)).isEqualTo(hashes.getAllValues().get(1)).isNotEqualTo(hashes.getAllValues().get(2));
		assertThat(hashes.getValue().toString()).doesNotContain("uid", "+16505550123", "proof-3");
	}
	@Test void nonPhoneAndStaleProofAreRejectedBeforeResolving() {
		when(verifier.verify(anyString(), any())).thenReturn(proof(FirebaseAuthenticationMethod.GOOGLE, NOW, NOW));
		denied(AuthErrorStatus.INVALID_RECOVERY_PROOF);
		when(verifier.verify(anyString(), any())).thenReturn(proof(FirebaseAuthenticationMethod.PHONE, NOW.minusSeconds(601), NOW));
		denied(AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED);
		verifyNoInteractions(resolver);
	}
	@Test void errorsDoNotExposeProviderDetailsAndStoreOutageFailsClosed() {
		when(verifier.verify(anyString(), any())).thenThrow(new AuthException(AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN));
		denied(AuthErrorStatus.INVALID_RECOVERY_PROOF);
		doThrow(new org.springframework.dao.DataAccessResourceFailureException("private backend message"))
				.when(store).admit(anyList(), anyInt(), anyInt(), any());
		denied(AuthErrorStatus.RECOVERY_UNAVAILABLE);
	}
	@Test void malformedRequestCannotInvokeFirebase() {
		assertThatThrownBy(() -> service.lookup("bad", "proof", "127.0.0.1")).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_RECOVERY_REQUEST));
		verifyNoInteractions(verifier);
	}
	void denied(AuthErrorStatus expected) {
		assertThatThrownBy(() -> service.lookup(ID, "test-proof", "127.0.0.1")).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(expected));
	}
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(ints = {301, 599, 600})
	void acceptsPhoneAuthenticationThroughTenMinutes(int ageSeconds) {
		when(verifier.verify(anyString(), any())).thenReturn(proof(FirebaseAuthenticationMethod.PHONE, NOW.minusSeconds(ageSeconds), NOW));
		assertThat(service.lookup(ID, "test-proof", "127.0.0.1").status()).isEqualTo(RecoveryResult.Status.NOT_FOUND);
		verify(store).lookup(eq(ID), anyList(), any(), eq(NOW), eq(Duration.ofMinutes(5)), any());
	}
	@Test void customRecentAuthWindowStillApplies() {
		var configured = new RecoveryProperties(true, RING, null, Duration.ofMinutes(2), null, 0, 0, 0);
		var customService = new AccountRecoveryService(store, new RecoveryHasher(RING), verifier, resolver, configured, Clock.fixed(NOW, ZoneOffset.UTC));
		when(verifier.verify(anyString(), any())).thenReturn(proof(FirebaseAuthenticationMethod.PHONE, NOW.minusSeconds(121), NOW));
		assertThatThrownBy(() -> customService.lookup(ID, "test-proof", "127.0.0.1"))
				.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED));
		verifyNoInteractions(resolver);
	}
}
