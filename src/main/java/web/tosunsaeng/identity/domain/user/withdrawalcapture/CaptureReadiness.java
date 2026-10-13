package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.util.Objects;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

/** Local database/config readiness only; not a clock or old-writer attestation. */
public final class CaptureReadiness {
    private final MongoTemplate mongo;
    private final WithdrawalCaptureProperties properties;
    public CaptureReadiness(MongoTemplate mongo, WithdrawalCaptureProperties properties) {
        this.mongo = Objects.requireNonNull(mongo); this.properties = Objects.requireNonNull(properties);
    }
    public boolean ready() {
        return activeSource().isPresent();
    }
    /** Return the same validated read used for readiness; never re-select a different source. */
    public java.util.Optional<WithdrawalRecords.Stream> activeSource() {
        String environment = properties.getEnvironment();
        if (!properties.isRequired() || environment == null || !environment.matches("[a-z0-9-]{1,40}")) return java.util.Optional.empty();
        var sources = mongo.find(Query.query(Criteria.where("environment").is(environment).and("status").is("ACTIVE"))
                .collation(Collation.of("simple")).limit(2).maxTimeMsec(5000), WithdrawalRecords.Stream.class);
        if (sources.size() != 1) return java.util.Optional.empty();
        var source = sources.getFirst();
        try { WithdrawalRecords.uuid(source.streamId()); } catch (IllegalArgumentException failure) { return java.util.Optional.empty(); }
        boolean valid = environment.equals(source.environment()) && "ACTIVE".equals(source.status())
                && source.captureCutover() != null && source.lastSequence() >= 0 && source.lastSequence() < Long.MAX_VALUE
                && source.replayFloor() >= 0 && source.replayFloor() <= source.lastSequence();
        return valid ? java.util.Optional.of(source) : java.util.Optional.empty();
    }
}
