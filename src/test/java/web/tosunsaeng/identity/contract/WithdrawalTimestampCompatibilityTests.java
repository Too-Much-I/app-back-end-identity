package web.tosunsaeng.identity.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnEventMapper;

/** Actual entity/converter/mapper tests; no Mongo server, transactions or snapshot history involved. */
class WithdrawalTimestampCompatibilityTests {
    private static final String USER_ID = "11111111-1111-4111-8111-111111111111";
    private static final String EVENT_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-07T00:00:00Z", "2026-10-07T00:00:00.123Z",
            "2026-10-07T00:00:00.123456Z", "2026-10-07T00:00:00.123456789Z"})
    void persistedUserAndOutboxAgreeButPrePersistenceWireCanDiffer(String text) {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
                    ObjectMapper json = context.getBean(ObjectMapper.class);
                    var converter = converter();
                    var time = Instant.parse(text);
                    var storedTime = time.truncatedTo(ChronoUnit.MILLIS);
                    var user = User.createFederatedMember("synthetic", UserConsents.unconsented(), time)
                            .toWithdrawnTombstone(time);
                    ReflectionTestUtils.setField(user, "userId", USER_ID);
                    var event = UserWithdrawnOutbox.create(USER_ID, time);
                    ReflectionTestUtils.setField(event, "eventId", EVENT_ID);
                    var userDoc = new Document();
                    var eventDoc = new Document();
                    converter.write(user, userDoc);
                    converter.write(event, eventDoc);
                    assertThat(userDoc.get("withdrawnAt")).isInstanceOf(Date.class);
                    assertThat(eventDoc.get("withdrawnAt")).isEqualTo(userDoc.get("withdrawnAt"));
                    User persistedUser = converter.read(User.class, userDoc);
                    UserWithdrawnOutbox persistedEvent = converter.read(UserWithdrawnOutbox.class, eventDoc);
                    assertThat(persistedUser.getWithdrawnAt()).isEqualTo(storedTime);
                    assertThat(persistedEvent.getWithdrawnAt()).isEqualTo(storedTime);

                    var mapper = new UserWithdrawnEventMapper(json);
                    String before = new String(mapper.serialize(event), java.nio.charset.StandardCharsets.UTF_8);
                    String after = new String(mapper.serialize(persistedEvent), java.nio.charset.StandardCharsets.UTF_8);
                    assertThat(PaymentLifecycleFixtureTests.decode(before)).isEqualTo("ACCEPT");
                    assertThat(PaymentLifecycleFixtureTests.decode(after)).isEqualTo("ACCEPT");
                    assertThat(json.readTree(before).get("withdrawnAt").asText()).isEqualTo(time.toString());
                    assertThat(json.readTree(after).get("withdrawnAt").asText()).isEqualTo(storedTime.toString());
                    assertThat(json.readTree(after).size()).isEqualTo(4);
                    if (text.equals("2026-10-07T00:00:00Z"))
                        assertThat(json.readTree(after)).isEqualTo(
                                PaymentLifecycleFixtureTests.fixture("protocol-cases").get("withdrawal"));
                    if (time.equals(storedTime)) assertThat(after).isEqualTo(before);
                    else assertThat(after).isNotEqualTo(before);

                    var snapshotRow = new PaymentLifecycleFixtureTests.Row(USER_ID, persistedUser.getWithdrawnAt().toString());
                    var eventRow = new PaymentLifecycleFixtureTests.Row(USER_ID, persistedEvent.getWithdrawnAt().toString());
                    var originalRow = new PaymentLifecycleFixtureTests.Row(USER_ID, time.toString());
                    String snapshotDigest = PaymentLifecycleFixtureTests.digest(List.of(List.of(snapshotRow)));
                    assertThat(PaymentLifecycleFixtureTests.digest(List.of(List.of(eventRow)))).isEqualTo(snapshotDigest);
                    if (!time.equals(storedTime))
                        assertThat(PaymentLifecycleFixtureTests.digest(List.of(List.of(originalRow))))
                                .isNotEqualTo(snapshotDigest);

                    // A candidate canonical-text journal field survives conversion; not a production schema change.
                    var canonical = new Document("withdrawnAtCanonical", time.toString());
                    var converted = new Document();
                    converter.write(canonical, converted);
                    assertThat(converted.getString("withdrawnAtCanonical")).isEqualTo(text);
                });
    }

    private static MappingMongoConverter converter() {
        var conversions = MongoCustomConversions.create(adapter -> { });
        var mapping = new MongoMappingContext();
        mapping.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mapping.afterPropertiesSet();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mapping);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }
}
