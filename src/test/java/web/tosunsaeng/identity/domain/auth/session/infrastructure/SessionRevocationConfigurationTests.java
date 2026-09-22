package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;

import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.refresh.*;

class SessionRevocationConfigurationTests {
	@Test void schedulerPreservesSafeDiagnosticWithoutRawException() {
		var worker = mock(FirebaseSessionRevocationWorker.class);
		var source = new org.springframework.dao.QueryTimeoutException("test-sensitive-sentinel");
		var error = new web.tosunsaeng.identity.domain.auth.common.exception.AuthException(
				web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE,
				web.tosunsaeng.identity.global.observability.FailureDiagnostic.from(source,
						web.tosunsaeng.identity.global.observability.FailureDiagnostic.Operation.SESSION_TRANSACTION));
		org.mockito.Mockito.doThrow(error).when(worker).runBatch();
		try (var logs = web.tosunsaeng.identity.support.LogCapture.forClass(SessionRevocationConfiguration.Scheduler.class)) {
			new SessionRevocationConfiguration.Scheduler(worker).run();
			assertThat(logs.events()).singleElement().satisfies(event -> {
				assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
				assertThat(web.tosunsaeng.identity.support.LogCapture.value(event, "failureKind")).isEqualTo("DB_TIMEOUT");
				assertThat(web.tosunsaeng.identity.support.LogCapture.value(event, "operation")).isEqualTo("SESSION_TRANSACTION");
				assertThat(event.getThrowableProxy()).isNull();
				assertThat(web.tosunsaeng.identity.support.LogCapture.rendered(event)).doesNotContain("test-sensitive-sentinel");
			});
		}
	}
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(SessionRevocationConfiguration.class);

	@Test void defaultConfigurationDoesNotInstantiateExternalClientOrFence() {
		runner.run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(SessionRevocationProperties.class)
					.doesNotHaveBean(SessionSecurityService.class).doesNotHaveBean(LogoutAllCoordinator.class)
					.doesNotHaveBean(FirebaseSessionRevocationPort.class).doesNotHaveBean(FirebaseSessionRevocationWorker.class);
		});
	}
	@Test void workerCannotRunWithoutCaptureAndFence() {
		runner.withPropertyValues("app.session-revocation.worker-enabled=true")
				.run(context -> assertThat(context).hasFailed());
	}
	@Test void fenceOnlyDeploymentInjectsCommonIssuerWithoutStartingWorker() {
		infrastructure().withPropertyValues("app.session-revocation.fence-enabled=true")
				.withBean(RefreshSessionIssuer.class, () -> new RefreshSessionIssuer(mock(RefreshTokenGenerator.class),
						new RefreshTokenHasher(), mock(RefreshSessionRepository.class),
						new RefreshTokenProperties(java.time.Duration.ofDays(14), 32), Clock.systemUTC()))
				.run(context -> {
					assertThat(context).hasNotFailed().hasSingleBean(SessionSecurityService.class)
							.doesNotHaveBean(LogoutAllCoordinator.class).doesNotHaveBean(FirebaseSessionRevocationWorker.class);
					assertThat(context.getBean(RefreshSessionIssuer.class).isFenceEnabled()).isTrue();
				});
	}
	@Test void captureRequiresConfiguredFirebaseButWorkerCanRemainOff() {
		infrastructure().withPropertyValues("app.session-revocation.fence-enabled=true", "app.session-revocation.capture-enabled=true")
				.withBean(FirebaseAuthProperties.class, () -> new FirebaseAuthProperties(true, "test-project", null,
						false, false, false, false, null, null, null, null, null, null, null, null))
				.withBean(Clock.class, Clock::systemUTC)
				.run(context -> assertThat(context).hasNotFailed().hasSingleBean(LogoutAllCoordinator.class)
						.doesNotHaveBean(FirebaseSessionRevocationPort.class));
	}
	private ApplicationContextRunner infrastructure() {
		return runner.withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
				.withBean(MongoTransactionManager.class, () -> mock(MongoTransactionManager.class))
				.withBean(UserRepository.class, () -> mock(UserRepository.class))
				.withBean(FirebaseIdentityRepository.class, () -> mock(FirebaseIdentityRepository.class));
	}
	@Test void existingIdentityReleaseSupportsTransactionalClassProxy() {
		var target = new web.tosunsaeng.identity.domain.user.application.UserWithdrawalIdentityReleaseTransactionService(
				mock(web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository.class),
				mock(UserRepository.class), mock(FirebaseIdentityRepository.class),
				mock(web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository.class),
				mock(web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository.class),
				mock(web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository.class),
				mock(web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository.class),
				mock(web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository.class));
		var factory = new org.springframework.aop.framework.ProxyFactory(target);
		factory.setProxyTargetClass(true);
		var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor();
		interceptor.setTransactionManager(mock(MongoTransactionManager.class));
		interceptor.setTransactionAttributeSource(new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
		factory.addAdvice(interceptor);
		assertThat(org.springframework.aop.support.AopUtils.isCglibProxy(factory.getProxy())).isTrue();
	}
}
