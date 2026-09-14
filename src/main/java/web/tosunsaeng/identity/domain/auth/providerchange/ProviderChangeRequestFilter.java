package web.tosunsaeng.identity.domain.auth.providerchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded body (including chunked bodies) and bounded per-instance ingress budget. No body logging. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ProviderChangeRequestFilter extends OncePerRequestFilter {
	private static final int MAX_BYTES = 24_576;
	private final ProviderRequestBudget budget = new ProviderRequestBudget(10_000, 120);
	@Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		if (!path.startsWith("/api/v1/auth/firebase/providers/")) { chain.doFilter(request, response); return; }
		long minute = System.currentTimeMillis() / 60_000;
		// Do not trust X-Forwarded-For. Deployment ingress must enforce distributed quotas too.
		String address = request.getRemoteAddr();
		if (!budget.admit(address, minute)) { reject(response, 429, "PROVIDER_RATE_LIMITED"); return; }
		byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
		if (body.length > MAX_BYTES) { java.util.Arrays.fill(body, (byte) 0); reject(response, 413, "PROVIDER_REQUEST_TOO_LARGE"); return; }
		var wrapped = new HttpServletRequestWrapper(request) {
			@Override public ServletInputStream getInputStream() {
				var input = new ByteArrayInputStream(body);
				return new ServletInputStream() {
					public int read() { return input.read(); }
					public boolean isFinished() { return input.available() == 0; }
					public boolean isReady() { return true; }
					public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous request only."); }
				};
			}
			@Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
		};
		try { chain.doFilter(wrapped, response); }
		finally { java.util.Arrays.fill(body, (byte) 0); }
	}
	private void reject(HttpServletResponse response, int status, String code) throws IOException {
		response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store"); response.setHeader("Pragma", "no-cache");
		if (status == 429) response.setHeader("Retry-After", "60");
		response.getWriter().write("{\"isSuccess\":false,\"code\":\"" + code + "\",\"message\":\"Request cannot be processed.\",\"result\":null}");
	}
}
