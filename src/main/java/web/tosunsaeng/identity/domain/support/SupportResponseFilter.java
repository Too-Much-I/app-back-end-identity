package web.tosunsaeng.identity.domain.support;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Includes authentication and framework errors, not just successful controller responses. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SupportResponseFilter extends OncePerRequestFilter {
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals(request.getContextPath() + "/api/v1/support/inquiries");
    }
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        // Conservative bound valid for either hourly or daily quota; not a promise of future admission.
        HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper(response) {
            @Override public void setStatus(int status) {
                super.setStatus(status);
                if (status == 429) setHeader("Retry-After", "86400");
            }
        };
        chain.doFilter(request, wrapped);
    }
}
