package steam.vm.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public Map<String, Object> csrf(CsrfToken token) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", token != null);

        if (token != null) {
            response.put("headerName", token.getHeaderName());
            response.put("parameterName", token.getParameterName());
            response.put("token", token.getToken());
        }

        return response;
    }
}
