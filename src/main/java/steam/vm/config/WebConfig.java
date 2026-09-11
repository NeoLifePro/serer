package steam.vm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://nx-panel.com",
                        "https://www.nx-panel.com",
                        "https://api.nx-panel.com",
                        "http://nx-panel.com",
                        "http://www.nx-panel.com",
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "http://45.43.163.112",
                        "http://45.43.163.112:4200",
                        "http://45.43.163.112:8080"
                )
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
