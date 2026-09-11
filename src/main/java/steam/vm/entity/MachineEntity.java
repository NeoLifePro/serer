package steam.vm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitored_computer", uniqueConstraints = @UniqueConstraint(name = "uk_user_computer_uuid", columnNames = {"user_id", "computer_uuid"}))
public class MachineEntity {
    public enum Status { ONLINE, OFFLINE }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "computer_id")
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(name = "computer_uuid", nullable = false, length = 100)
    private String deviceKey;
    @Column(name = "computer_name", nullable = false, length = 100)
    private String name;
    @Column(name = "os_name", length = 100)
    private String osName;
    @Column(name = "os_version", length = 100)
    private String osVersion;
    @Column(name = "os_architecture", length = 32)
    private String osArchitecture;
    @Column(name = "cpu_name", length = 255)
    private String cpuName;
    @Column(name = "cpu_usage_percent", precision = 5, scale = 2)
    private BigDecimal cpuUsagePercent;
    @Column(name = "ram_used_bytes")
    private Long ramUsedBytes;
    @Column(name = "ram_total_bytes")
    private Long ramTotalBytes;
    @Column(name = "disk_used_bytes")
    private Long diskUsedBytes;
    @Column(name = "disk_total_bytes")
    private Long diskTotalBytes;
    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;
    @Column(name = "metrics_updated_at")
    private LocalDateTime metricsUpdatedAt;
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    @Column(name = "app_version", length = 32)
    private String appVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('ONLINE','OFFLINE')")
    private Status status = Status.OFFLINE;
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    @Column(name = "connected_at")
    private LocalDateTime connectedAt;
    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public String getDeviceKey() { return deviceKey; }
    public void setDeviceKey(String deviceKey) { this.deviceKey = deviceKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getOsArchitecture() { return osArchitecture; }
    public void setOsArchitecture(String osArchitecture) { this.osArchitecture = osArchitecture; }
    public String getCpuName() { return cpuName; }
    public void setCpuName(String cpuName) { this.cpuName = cpuName; }
    public BigDecimal getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(BigDecimal cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public Long getRamUsedBytes() { return ramUsedBytes; }
    public void setRamUsedBytes(Long ramUsedBytes) { this.ramUsedBytes = ramUsedBytes; }
    public Long getRamTotalBytes() { return ramTotalBytes; }
    public void setRamTotalBytes(Long ramTotalBytes) { this.ramTotalBytes = ramTotalBytes; }
    public Long getDiskUsedBytes() { return diskUsedBytes; }
    public void setDiskUsedBytes(Long diskUsedBytes) { this.diskUsedBytes = diskUsedBytes; }
    public Long getDiskTotalBytes() { return diskTotalBytes; }
    public void setDiskTotalBytes(Long diskTotalBytes) { this.diskTotalBytes = diskTotalBytes; }
    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
    public LocalDateTime getMetricsUpdatedAt() { return metricsUpdatedAt; }
    public void setMetricsUpdatedAt(LocalDateTime metricsUpdatedAt) { this.metricsUpdatedAt = metricsUpdatedAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isOnline() { return status == Status.ONLINE; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
    public LocalDateTime getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(LocalDateTime disconnectedAt) { this.disconnectedAt = disconnectedAt; }
}

