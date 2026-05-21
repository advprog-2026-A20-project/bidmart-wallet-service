package id.ac.ui.cs.advprog.walletservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalServiceSecurityConfig implements WebMvcConfigurer {

    private final InternalServiceTokenInterceptor internalServiceTokenInterceptor;

    public InternalServiceSecurityConfig(InternalServiceTokenInterceptor internalServiceTokenInterceptor) {
        this.internalServiceTokenInterceptor = internalServiceTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalServiceTokenInterceptor)
            .addPathPatterns("/wallet/internal/**", "/internal/wallet/**");
    }
}
