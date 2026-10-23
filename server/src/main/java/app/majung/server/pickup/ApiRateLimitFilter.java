package app.majung.server.pickup;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded, per-process fixed windows. Trust the socket peer, not forwarded headers. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = 60_000;
    private final Clock clock;
    private final boolean enabled;
    private final int requestsPerMinute;
    private final int writesPerMinute;
    private final int maxClients;
    private final Map<String, Window> clients = new HashMap<>();
    private long nextSweep;

    private static class Window {
        final long started;
        int requests;
        int writes;
        Window(long started) { this.started = started; }
    }

    ApiRateLimitFilter(Clock clock, @Value("${majung.rate-limit.enabled:true}") boolean enabled,
                       @Value("${majung.rate-limit.requests-per-minute:240}") int requestsPerMinute,
                       @Value("${majung.rate-limit.writes-per-minute:90}") int writesPerMinute,
                       @Value("${majung.rate-limit.max-clients:10000}") int maxClients) {
        if (requestsPerMinute < 1 || writesPerMinute < 1 || writesPerMinute > requestsPerMinute || maxClients < 1) {
            throw new IllegalArgumentException("Invalid API rate limit configuration");
        }
        this.clock = clock;
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute;
        this.writesPerMinute = writesPerMinute;
        this.maxClients = maxClients;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getServletPath().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean write = !request.getMethod().equals("GET") && !request.getMethod().equals("HEAD");
        int status = acquire(request.getRemoteAddr(), write);
        if (status == 0) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");
        String code = status == 429 ? "RATE_LIMITED" : "SERVER_BUSY";
        response.getWriter().write("{\"code\":\"" + code
                + "\",\"message\":\"요청이 많습니다. 잠시 후 다시 시도해주세요.\",\"requestId\":\""
                + request.getAttribute("requestId") + "\"}");
    }

    synchronized int acquire(String peer, boolean write) {
        long now = clock.millis();
        if (now >= nextSweep) {
            clients.values().removeIf(window -> now - window.started >= WINDOW_MILLIS);
            nextSweep = now + WINDOW_MILLIS;
        }
        Window window = clients.get(peer);
        if (window == null || now < window.started || now - window.started >= WINDOW_MILLIS) {
            if (window == null && clients.size() >= maxClients) return 503;
            window = new Window(now);
            clients.put(peer, window);
        }
        if (window.requests >= requestsPerMinute || (write && window.writes >= writesPerMinute)) return 429;
        window.requests++;
        if (write) window.writes++;
        return 0;
    }
}
