package web.tosunsaeng.identity.global.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Exports annotation metadata and explicit I2 index specs; no application context or Mongo client. */
class MongoIndexManifestTests {
    @Test
    void exportsAllMappedIndexesWithoutConnectingToMongo() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                org.springframework.data.mongodb.core.mapping.Document.class));
        var types = new HashSet<Class<?>>();
        for (var bean : scanner.findCandidateComponents("web.tosunsaeng.identity")) {
            types.add(Class.forName(bean.getBeanClassName()));
        }
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(MongoCustomConversions.create(adapter -> {}).getSimpleTypeHolder());
        context.setInitialEntitySet(types);
        context.afterPropertiesSet();
        var resolver = new MongoPersistentEntityIndexResolver(context);
        var collections = new ArrayList<Document>();
        for (var type : types) {
            var indexes = new ArrayList<Document>();
            for (var index : resolver.resolveIndexFor(type)) {
                indexes.add(new Document("keys", index.getIndexKeys())
                        .append("options", index.getIndexOptions()));
            }
            String collectionName = context.getRequiredPersistentEntity(type).getCollection();
            for (var spec : web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalIndexes.SPECS) {
                if (!spec.collection().equals(collectionName)) continue;
                var options = new Document("name",spec.name()).append("unique",spec.unique())
                        .append("sparse",false).append("collation",new Document("locale","simple"));
                if (spec.partial()!=null) options.append("partialFilterExpression",spec.partial());
                indexes.add(new Document("keys",spec.keys()).append("options",options));
            }
            indexes.sort(Comparator.comparing(d -> d.get("options", Document.class).toJson()));
            collections.add(new Document("collection", context.getRequiredPersistentEntity(type).getCollection())
                    .append("entity", type.getName()).append("indexes", indexes));
        }
        collections.sort(Comparator.comparing(d -> d.getString("collection")));
        assertThat(collections).hasSize(34);
        assertThat(collections.stream().map(d -> d.getString("collection"))).doesNotHaveDuplicates();
        var refresh = collections.stream().filter(d -> "refresh_sessions".equals(d.getString("collection")))
                .findFirst().orElseThrow().getList("indexes", Document.class);
        var rotation = refresh.stream().filter(d -> "uk_refresh_user_rotation_request".equals(
                d.get("options", Document.class).getString("name"))).findFirst().orElseThrow();
        assertThat(rotation.get("keys", Document.class))
                .isEqualTo(Document.parse("{'userId':1,'rotationRequestKeyHash':1}"));
        assertThat(rotation.get("options", Document.class).getBoolean("unique")).isTrue();
        assertThat(rotation.get("options", Document.class).get("partialFilterExpression"))
                .isEqualTo(Document.parse("{'rotationRequestKeyHash':{'$type':'string'}}"));
        var manifest = new Document("formatVersion", 1)
                .append("scope", "Identity mapped entities; implicit _id indexes and programmatic Support indexes excluded")
                .append("collections", collections);
        var output = Path.of("build/generated/mongodb/identity-index-manifest.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, manifest.toJson(JsonWriterSettings.builder().indent(true).build()) + "\n");
    }
}
