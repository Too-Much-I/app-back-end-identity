package web.tosunsaeng.identity.domain.support;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;

@Component
public final class SupportActorResolver {
    private final CurrentUserProvider current;
    private final UserRepository users;
    public SupportActorResolver(CurrentUserProvider current, UserRepository users) { this.current = current; this.users = users; }
    public SupportService.Actor resolve(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) return new SupportService.Actor(null, "ANONYMOUS");
        if (!(authentication instanceof JwtAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new BusinessException(CommonErrorStatus.UNAUTHORIZED);
        }
        String id = current.getCurrentUserId();
        var user = users.findById(id).orElseThrow(() -> new BusinessException(UserErrorStatus.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) throw new BusinessException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
        return new SupportService.Actor(user.getUserId(), user.getAccountType().name());
    }
}
