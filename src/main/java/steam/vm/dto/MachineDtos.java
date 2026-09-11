package steam.vm.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class MachineDtos {
    private MachineDtos() {}

    public record MachineResponse(Long id, String deviceKey, String name, String status, String ip,
                                  String os, String cpu, String ram, String disk, LocalDateTime lastSeen,
                                  int cpuUsage, int ramUsage, int diskUsage, boolean hasWarning) {}

    public static class HeartbeatRequest {
        @JsonAlias("device_key") public String deviceKey;
        public String name;
        public String ip;
        @JsonAlias("app_version") public String appVersion;
        @JsonAlias("os_name") public String osName;
        @JsonAlias("os_version") public String osVersion;
        @JsonAlias("os_architecture") public String osArchitecture;
        @JsonAlias("cpu_name") public String cpuName;
        @JsonAlias("cpu_usage_percent") public BigDecimal cpuUsagePercent;
        @JsonAlias("ram_used_bytes") public Long ramUsedBytes;
        @JsonAlias("ram_total_bytes") public Long ramTotalBytes;
        @JsonAlias("disk_used_bytes") public Long diskUsedBytes;
        @JsonAlias("disk_total_bytes") public Long diskTotalBytes;
        @JsonAlias("uptime_seconds") public Long uptimeSeconds;
    }

    public record HeartbeatResponse(boolean ok, Long machineId, LocalDateTime serverTime) {}
    public record MachineLogResponse(Long id, Long machineId, String machine, String level,
                                     String source, String category, String message, LocalDateTime createdAt) {}
    public static class CreateLogRequest {
        public String level;
        public String source;
        public String category;
        public String message;
    }
    public record DeleteResponse(boolean ok, Long machineId) {}
}

