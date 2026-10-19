package web.tosunsaeng.identity.domain.auth.mergeprogress;
import java.lang.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
/** Full-context tests disable Mongo autoconfiguration; focused tests exercise the real progress store. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types={UserMergeQueryService.class, MergeProgressStore.class, MergeQuerySupport.class, MergeProgressPrivacyCleanup.class})
public @interface MockMergeProgressInfrastructure {}
