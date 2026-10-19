package web.tosunsaeng.identity.domain.support;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import web.tosunsaeng.identity.global.security.jwt.TestRsaKeyConfiguration;
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.*;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.*;

@web.tosunsaeng.identity.domain.auth.mergeprogress.MockMergeProgressInfrastructure
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRsaKeyConfiguration.class)
class SupportWebTests {
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	private web.tosunsaeng.identity.domain.auth.registration.application.GuestRecoveryTransactionService guestRecoveryTransactionService;

    static final String PATH = "/api/v1/support/inquiries";
    static final String KEY = "00000000-0000-4000-8000-000000000001";
    static final String USER = "00000000-0000-4000-8000-000000000002";
    static final String BODY = "{\"category\":\"AUTH\",\"message\":\"synthetic inquiry message\"}";
    @Autowired MockMvc mvc;
    @MockitoBean SupportService service;
    @MockitoBean ProviderChangeGuard guard;
    @MockitoBean UserRepository users;
    @MockitoBean RefreshSessionRepository sessions;
    @MockitoBean UserWithdrawnOutboxRepository withdrawals;
    @BeforeEach void setup() {
        when(service.submit(any(), anyString(), any(), any())).thenReturn(new SupportService.Result("receipt", false));
    }
    @Test void anonymousReceiptAndReplay() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").header("Idempotency-Key", KEY).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.result.inquiryId").value("receipt"));
        verify(service).submit(eq(new SupportService.Actor(null, "ANONYMOUS")), anyString(), eq(KEY), any());
        when(service.submit(any(), anyString(), any(), any())).thenReturn(new SupportService.Result("receipt", true));
        mvc.perform(post(PATH).contentType("application/json").header("Idempotency-Key", KEY).content(BODY)).andExpect(status().isOk());
    }
    @Test void refundCategoryIsAcceptedAndPassedToService() throws Exception {
        User user = mock(User.class); when(user.getUserId()).thenReturn(USER);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE); when(user.getAccountType()).thenReturn(UserAccountType.MEMBER);
        when(users.findById(USER)).thenReturn(Optional.of(user));
        mvc.perform(post(PATH).with(jwt().jwt(j -> j.subject(USER)))
                        .contentType("application/json").header("Idempotency-Key", KEY)
                        .content(BODY.replace("AUTH", "REFUND")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.result.status").value("RECEIVED"));
        verify(service).submit(eq(new SupportService.Actor(USER, "MEMBER")), anyString(), eq(KEY),
                argThat(request -> request.category() == SupportRequest.Category.REFUND));
    }
    @Test void anonymousRefundRequiresAuthenticationAndCannotSupplyUserId() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").header("Idempotency-Key", KEY)
                        .content(BODY.replace("AUTH", "REFUND")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SUPPORT_REFUND_AUTH_REQUIRED"));
        mvc.perform(post(PATH).contentType("application/json").header("Idempotency-Key", KEY)
                        .content(BODY.replace("AUTH", "REFUND").replace("}", ",\"userId\":\"" + USER + "\"}")))
                .andExpect(status().isBadRequest());
        verify(service, never()).submit(any(), any(), any(), any());
    }
    @Test void invalidBearerIsNeverDowngradedToAnonymous() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").header("Authorization", "Bearer invalid").content(BODY))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(service);
    }
    @Test void serverAccountTypeOverridesClientClaim() throws Exception {
        User user = mock(User.class); when(user.getUserId()).thenReturn(USER);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE); when(user.getAccountType()).thenReturn(UserAccountType.GUEST);
        when(users.findById(USER)).thenReturn(Optional.of(user));
        mvc.perform(post(PATH).with(jwt().jwt(j -> j.subject(USER).claim("account_type", "MEMBER")))
                .contentType("application/json").header("Idempotency-Key", KEY).content(BODY)).andExpect(status().isCreated());
        verify(service).submit(eq(new SupportService.Actor(USER, "GUEST")), anyString(), eq(KEY), any());
    }
    @Test void mergedAndSuspendedAccountsAreRejected() throws Exception {
        when(users.existsByUserIdAndStatus(USER, UserStatus.MERGED)).thenReturn(true);
        mvc.perform(post(PATH).with(jwt().jwt(j -> j.subject(USER))).contentType("application/json").content(BODY))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_MERGED_TOKEN_REJECTED"));
        when(users.existsByUserIdAndStatus(USER, UserStatus.MERGED)).thenReturn(false);
        User user = mock(User.class); when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(users.findById(USER)).thenReturn(Optional.of(user));
        mvc.perform(post(PATH).with(jwt().jwt(j -> j.subject(USER))).contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
        verify(service, never()).submit(any(), any(), any(), any());
    }
    @Test void rejectsUnknownDuplicateTrailingAndNumericFieldsWithoutEcho() throws Exception {
        for (String body : new String[]{BODY.replace("}", ",\"userId\":\"private-value\"}"),
                BODY.replace("}", ",\"message\":\"private-value\"}"), BODY + " {}",
                "{\"category\":0,\"message\":\"private-value\"}", "{\"category\":\"AUTH\",\"message\":\"x\"}"}) {
            mvc.perform(post(PATH).contentType("application/json").header("Idempotency-Key", KEY).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-value"))));
        }
    }
    @Test void boundsBodyEvenWithoutContentLength() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").content("x".repeat(16385)))
                .andExpect(status().isPayloadTooLarge());
    }
    @Test void quotaHasRetryAfterAndOtherEndpointsRemainProtected() throws Exception {
        doThrow(SupportError.SUPPORT_INQUIRY_RATE_LIMITED.exception()).when(service).burst(anyString());
        mvc.perform(post(PATH).contentType("application/json").content(BODY)).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "86400"));
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/support/inquiries/receipt")).andExpect(status().isUnauthorized());
    }
}
