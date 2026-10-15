package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.*;
import com.google.firebase.auth.*;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static web.tosunsaeng.identity.domain.auth.accountrecovery.PhoneOnlyCleanupPolicy.Decision.*;

class PhoneOnlyCleanupAuditTests {
    final Instant now = Instant.parse("2026-10-15T00:00:00Z");
    final AbstractFirebaseAuth firebase = mock(AbstractFirebaseAuth.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final PhoneFingerprintHasher hasher = mock(PhoneFingerprintHasher.class);
    final PhoneOnlyCleanupAudit audit = new PhoneOnlyCleanupAudit(firebase, mongo, hasher, Clock.fixed(now, ZoneOffset.UTC));
    final ListUsersPage page = mock(ListUsersPage.class);
    final ExportedUserRecord listed = mock(ExportedUserRecord.class);
    final UserRecord fresh = mock(UserRecord.class);

    @BeforeEach void setup() throws Exception {
        when(firebase.listUsers(null, 100)).thenReturn(page);
        when(page.getValues()).thenReturn(List.of(listed));
        when(listed.getUid()).thenReturn("test-uid");
        when(firebase.getUser("test-uid")).thenReturn(fresh);
        when(fresh.getUid()).thenReturn("test-uid");
        when(fresh.getPhoneNumber()).thenReturn("+16505550123");
        var provider = mock(UserInfo.class);
        when(provider.getProviderId()).thenReturn("phone");
        when(fresh.getProviderData()).thenReturn(new UserInfo[]{provider});
        when(fresh.getUserMetadata()).thenReturn(new UserMetadata(now.minusSeconds(3600).toEpochMilli(),
                now.minusSeconds(3600).toEpochMilli(), 0));
        var fp = new PhoneFingerprint("v1", "A".repeat(43));
        when(hasher.fingerprint(anyString())).thenReturn(new PhoneFingerprintSet(fp, List.of(fp)));
    }

    @Test void cleanCandidateIsCountedButNeverDeleted() throws Exception {
        var result = audit.inspectPage();
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.sweepComplete()).isTrue();
        assertThat(result.counts()).containsEntry(CANDIDATE, 1);
        verify(firebase).listUsers(null, 100);
        verify(firebase).getUser("test-uid");
        verifyNoMoreInteractions(firebase);
    }

    @Test void productionReferenceProtectsRegardlessOfStatus() throws Exception {
        when(mongo.exists(any(), eq("firebase_identities"))).thenReturn(true);
        assertThat(audit.inspectPage().counts()).containsEntry(PROTECTED, 1);
        verify(firebase, never()).deleteUser(anyString());
    }

    @Test void databaseFailureIsUnknownNotCandidate() throws Exception {
        when(mongo.exists(any(), eq("firebase_identities"))).thenThrow(new IllegalStateException("test"));
        assertThat(audit.inspectPage().counts()).containsEntry(UNKNOWN, 1);
        verify(firebase, never()).deleteUser(anyString());
    }

    @Test void freshProviderStateIsUsedInsteadOfListSnapshot() throws Exception {
        var google = mock(UserInfo.class);
        when(google.getProviderId()).thenReturn("google.com");
        when(fresh.getProviderData()).thenReturn(new UserInfo[]{google});
        assertThat(audit.inspectPage().counts()).containsEntry(NOT_PHONE_ONLY, 1);
        verifyNoInteractions(mongo, hasher);
    }

    @Test void tokenRefreshProtectsAnOldAccount() throws Exception {
        when(fresh.getUserMetadata()).thenReturn(new UserMetadata(now.minusSeconds(3600).toEpochMilli(),
                now.minusSeconds(3600).toEpochMilli(), now.minusSeconds(10).toEpochMilli()));
        assertThat(audit.inspectPage().counts()).containsEntry(RECENT, 1);
        verifyNoInteractions(mongo, hasher);
    }

    @Test void missingRemoteUserIsUnknownAndScanCanContinue() throws Exception {
        when(firebase.getUser("test-uid")).thenThrow(new IllegalStateException("test"));
        assertThat(audit.inspectPage().counts()).containsEntry(UNKNOWN, 1);
        verifyNoInteractions(mongo, hasher);
    }
}
