package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Covers validation and authentication failures before the controller runs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ReissueNoStoreFilter extends OncePerRequestFilter {
	@Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if ((request.getContextPath() + "/api/v1/auth/reissue").equals(request.getRequestURI())) {
			response.setHeader("Cache-Control", "no-store"); response.setHeader("Pragma", "no-cache");
		}
		chain.doFilter(request, response);
	}
}
