package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.*;
import com.google.firebase.auth.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static web.tosunsaeng.identity.domain.auth.accountrecovery.PhoneOnlyCleanupDeletion.Outcome.*;

class PhoneOnlyCleanupDeletionTests {
    final Instant now = Instant.parse("2026-10-15T00:00:00Z");
    final AbstractFirebaseAuth firebase = mock(AbstractFirebaseAuth.class);
    final PhoneOnlyCleanupFence fence = mock(PhoneOnlyCleanupFence.class);
    final PhoneOnlyCleanupAudit audit = mock(PhoneOnlyCleanupAudit.class);
    final PhoneOnlyCleanupDeletion worker = new PhoneOnlyCleanupDeletion(firebase, fence, audit, "project",
            Clock.fixed(now, ZoneOffset.UTC));

    UserRecord user(boolean disabled, String... providers) {
        UserRecord user = mock(UserRecord.class);
        when(user.getUid()).thenReturn("uid");
        when(user.getPhoneNumber()).thenReturn("+16505550123");
        when(user.isDisabled()).thenReturn(disabled);
        when(user.getUserMetadata()).thenReturn(new UserMetadata(now.minusSeconds(3600).toEpochMilli(),
                now.minusSeconds(3600).toEpochMilli(), 0));
        UserInfo[] providerData = Arrays.stream(providers).map(id -> {
            UserInfo provider = mock(UserInfo.class); when(provider.getProviderId()).thenReturn(id); return provider;
        }).toArray(UserInfo[]::new);
        when(user.getProviderData()).thenReturn(providerData);
        return user;
    }

    FirebaseAuthException absent() {
        var error = mock(FirebaseAuthException.class);
        when(error.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
        return error;
    }

    @BeforeEach void setup() {
        when(audit.state(any())).thenReturn(new PhoneOnlyCleanupPolicy.ProductionState(true, false, false, false, false));
        when(fence.claim(eq("project"), eq("uid"), any())).thenAnswer(i -> ((java.util.function.Supplier<Boolean>)i.getArgument(2)).get());
    }

    @Test void disablesRevokesRechecksDeletesAndConfirmsAbsence() throws Exception {
        var original = user(false, "phone");
        var disabled = user(true, "phone"); var gone = absent();
        when(firebase.getUser("uid")).thenReturn(original, original, disabled).thenThrow(gone);
        assertThat(worker.process("uid")).isEqualTo(DELETED);
        var order = inOrder(firebase);
        order.verify(firebase, times(2)).getUser("uid");
        order.verify(firebase).updateUser(any());
        order.verify(firebase).revokeRefreshTokens("uid");
        order.verify(firebase).getUser("uid");
        order.verify(firebase).deleteUser("uid");
        order.verify(firebase).getUser("uid");
        verify(fence).mark("project", "uid", "DELETED", true);
    }

    @Test void snsOnlyAndSnsWithPhoneNeverMutated() throws Exception {
        for (var providers : List.of(new String[]{"google.com"}, new String[]{"phone", "google.com"})) {
            doReturn(user(false, providers)).when(firebase).getUser("uid");
            assertThat(worker.process("uid")).isEqualTo(SKIPPED);
        }
        verify(firebase, never()).updateUser(any()); verify(firebase, never()).deleteUser(anyString());
        verifyNoInteractions(fence);
    }

    @Test void ownerAppearingAfterClaimPreventsDisable() throws Exception {
        doReturn(user(false, "phone")).when(firebase).getUser("uid");
        when(audit.state(any())).thenReturn(new PhoneOnlyCleanupPolicy.ProductionState(true, false, false, false, false),
                new PhoneOnlyCleanupPolicy.ProductionState(true, false, false, false, false),
                new PhoneOnlyCleanupPolicy.ProductionState(true, true, false, false, false));
        assertThat(worker.process("uid")).isEqualTo(BLOCKED);
        verify(firebase, never()).updateUser(any()); verify(firebase, never()).deleteUser(anyString());
    }

    @Test void providerAppearingAfterDisableNeverDeletedOrAutomaticallyReenabled() throws Exception {
        var original = user(false, "phone");
        var changed = user(true, "phone", "google.com");
        when(firebase.getUser("uid")).thenReturn(original, original, changed);
        assertThat(worker.process("uid")).isEqualTo(BLOCKED);
        verify(firebase, times(1)).updateUser(any()); verify(firebase, never()).deleteUser(anyString());
        verify(fence).mark("project", "uid", "CHANGED_AFTER_DISABLE", false);
    }

    @Test void failedDisableDoesNotContinueToDeletion() throws Exception {
        doReturn(user(false, "phone")).when(firebase).getUser("uid");
        when(firebase.updateUser(any())).thenThrow(new IllegalStateException("test"));
        assertThat(worker.process("uid")).isEqualTo(BLOCKED);
        verify(firebase, never()).deleteUser(anyString());
        verify(fence).mark("project", "uid", "RESULT_UNKNOWN", false);
    }

    @Test void unknownDeletionResultStaysBlocked() throws Exception {
        var original = user(false, "phone");
        var disabled = user(true, "phone");
        when(firebase.getUser("uid")).thenReturn(original, original, disabled);
        doThrow(new IllegalStateException("test")).when(firebase).deleteUser("uid");
        assertThat(worker.process("uid")).isEqualTo(BLOCKED);
        verify(fence).mark("project", "uid", "RESULT_UNKNOWN", false);
    }

    @Test void existingClaimSkipsAllMutations() throws Exception {
        doReturn(user(false, "phone")).when(firebase).getUser("uid");
        when(fence.claim(anyString(), anyString(), any())).thenReturn(false);
        assertThat(worker.process("uid")).isEqualTo(SKIPPED);
        verify(firebase, never()).updateUser(any()); verify(firebase, never()).deleteUser(anyString());
    }

    @Test void alreadyAbsentIsIdempotent() throws Exception {
        doThrow(absent()).when(firebase).getUser("uid");
        assertThat(worker.process("uid")).isEqualTo(ABSENT);
        verifyNoInteractions(fence);
    }
}
