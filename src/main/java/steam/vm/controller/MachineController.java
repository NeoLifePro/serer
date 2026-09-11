package steam.vm.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.MachineDtos;
import steam.vm.service.MachineService;

import java.net.InetAddress;
import java.util.List;

@RestController
@RequestMapping("/api/machines")
public class MachineController {
    private final MachineService service;

    public MachineController(MachineService service) { this.service = service; }

    @GetMapping
    public List<MachineDtos.MachineResponse> machines(Authentication authentication) {
        return service.list(username(authentication));
    }

    @GetMapping("/logs")
    public List<MachineDtos.MachineLogResponse> logs(Authentication authentication,
                                                     @RequestParam(required = false) Long machineId,
                                                     @RequestParam(required = false) String source) {
        return service.logs(username(authentication), machineId, source);
    }


    @PostMapping("/heartbeat")
    public MachineDtos.HeartbeatResponse heartbeat(Authentication authentication,
                                                    @RequestBody MachineDtos.HeartbeatRequest request,
                                                    HttpServletRequest httpRequest) {
        return service.heartbeat(username(authentication), request, publicClientIp(httpRequest));
    }


    @PostMapping("/{machineId}/logs")
    public MachineDtos.MachineLogResponse addLog(Authentication authentication,
                                                  @PathVariable Long machineId,
                                                  @RequestBody MachineDtos.CreateLogRequest request) {
        return service.addApplicationLog(username(authentication), machineId, request);
    }

    @DeleteMapping("/{machineId}")
    public MachineDtos.DeleteResponse delete(Authentication authentication, @PathVariable Long machineId) {
        return service.delete(username(authentication), machineId);
    }

    private String publicClientIp(HttpServletRequest request) {
        String remote = normalizeIp(request.getRemoteAddr());
        if (isPublicIp(remote)) return remote;

        for (String header : List.of("CF-Connecting-IP", "X-Real-IP", "X-Forwarded-For")) {
            String value = request.getHeader(header);
            if (value == null) continue;
            for (String candidate : value.split(",")) {
                String ip = normalizeIp(candidate);
                if (isPublicIp(ip)) return ip;
            }
        }
        return null;
    }

    private String normalizeIp(String value) {
        if (value == null) return null;
        String ip = value.trim();
        if (ip.startsWith("::ffff:")) ip = ip.substring(7);
        return ip.length() <= 45 && ip.matches("[0-9a-fA-F:.]+") ? ip : null;
    }

    private boolean isPublicIp(String value) {
        if (value == null) return false;
        try {
            InetAddress address = InetAddress.getByName(value);
            return !address.isAnyLocalAddress() && !address.isLoopbackAddress()
                    && !address.isLinkLocalAddress() && !address.isSiteLocalAddress()
                    && !address.isMulticastAddress();
        } catch (Exception ignored) {
            return false;
        }
    }
    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }
        return authentication.getName();
    }
}
