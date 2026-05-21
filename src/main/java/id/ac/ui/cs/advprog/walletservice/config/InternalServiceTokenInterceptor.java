package id.ac.ui.cs.advprog.walletservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalServiceTokenInterceptor implements HandlerInterceptor {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String internalServiceToken;

    public InternalServiceTokenInterceptor(
        @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:}}") String internalServiceToken
    ) {
        this.internalServiceToken = internalServiceToken == null ? "" : internalServiceToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        if (internalServiceToken.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Internal service token is not configured");
            return false;
        }

        String providedToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (!internalServiceToken.equals(providedToken)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid internal service token");
            return false;
        }

        return true;
    }
}
