package steam.vm.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import steam.vm.dto.MachineDtos;
import steam.vm.entity.MachineEntity;
import steam.vm.entity.MachineLogEntity;
import steam.vm.entity.UserEntity;
import steam.vm.repo.MachineLogRepository;
import steam.vm.repo.MachineRepository;
import steam.vm.repo.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MachineService {
    private final MachineRepository machineRepo;
    private final MachineLogRepository logRepo;
    private final UserRepository userRepo;
    private final JdbcTemplate jdbc;
    private final Set<String> activeResourceWarnings = ConcurrentHashMap.newKeySet();

    public MachineService(MachineRepository machineRepo, MachineLogRepository logRepo,
                          UserRepository userRepo, JdbcTemplate jdbc) {
        this.machineRepo = machineRepo;
        this.logRepo = logRepo;
        this.userRepo = userRepo;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<MachineDtos.MachineResponse> list(String username) {
        UserEntity user = user(username);
        return machineRepo.findAllByUser_IdOrderByNameAsc(user.getId()).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<MachineDtos.MachineLogResponse> logs(String username, Long machineId, String source) {
        UserEntity user = user(username);
        String filter = source == null ? "all" : source.trim().toLowerCase(Locale.ROOT);
        List<MachineLogEntity> logs;
        if ("system".equals(filter)) {
            logs = logRepo.findTop500ByMachine_User_IdAndSourceOrderByCreatedAtDesc(
                    user.getId(), MachineLogEntity.Source.SYSTEM);
        } else if ("application".equals(filter) && machineId != null) {
            logs = logRepo.findTop500ByMachine_IdAndMachine_User_IdAndSourceInOrderByCreatedAtDesc(
                    machineId, user.getId(), List.of(MachineLogEntity.Source.PANEL, MachineLogEntity.Source.FARM));
        } else {
            logs = machineId == null
                    ? logRepo.findTop500ByMachine_User_IdOrderByCreatedAtDesc(user.getId())
                    : logRepo.findTop500ByMachine_IdAndMachine_User_IdOrderByCreatedAtDesc(machineId, user.getId());
        }
        return logs.stream().map(this::logResponse).toList();
    }

    @Transactional
    public MachineDtos.HeartbeatResponse heartbeat(String username, MachineDtos.HeartbeatRequest request, String publicClientIp) {
        UserEntity user = user(username);
        String deviceKey = required(request == null ? null : request.deviceKey, "deviceKey", 100);
        String name = required(request.name, "name", 100);
        LocalDateTime now = LocalDateTime.now();
        MachineEntity machine = machineRepo.findByUser_IdAndDeviceKey(user.getId(), deviceKey).orElse(null);
        boolean created = machine == null;
        boolean reconnect = !created && !machine.isOnline();
        if (created) {
            machine = new MachineEntity();
            machine.setUser(user);
            machine.setDeviceKey(deviceKey);
        }
        machine.setName(name);
        machine.setOsName(trim(request.osName, 100));
        machine.setOsVersion(trim(request.osVersion, 100));
        machine.setOsArchitecture(trim(request.osArchitecture, 32));
        machine.setCpuName(trim(request.cpuName, 255));
        machine.setCpuUsagePercent(percent(request.cpuUsagePercent));
        machine.setRamUsedBytes(nonNegative(request.ramUsedBytes));
        machine.setRamTotalBytes(nonNegative(request.ramTotalBytes));
        machine.setDiskUsedBytes(nonNegative(request.diskUsedBytes));
        machine.setDiskTotalBytes(nonNegative(request.diskTotalBytes));
        machine.setUptimeSeconds(nonNegative(request.uptimeSeconds));
        machine.setMetricsUpdatedAt(now);
        machine.setIpAddress(trim(publicClientIp != null ? publicClientIp : request.ip, 45));
        machine.setAppVersion(trim(request.appVersion, 32));
        machine.setStatus(MachineEntity.Status.ONLINE);
        machine.setLastSeen(now);
        if (created || reconnect) machine.setConnectedAt(now);
        machine.setDisconnectedAt(null);
        machine = machineRepo.save(machine);
        if (created) addLog(machine, MachineLogEntity.Level.INFO, "Computer registered and connected");
        else if (reconnect) addLog(machine, MachineLogEntity.Level.INFO, "Computer reconnected successfully");

        int currentCpu = cpuUsage(machine);
        int currentRam = usage(machine.getRamUsedBytes(), machine.getRamTotalBytes());
        int currentDisk = usage(machine.getDiskUsedBytes(), machine.getDiskTotalBytes());
        addResourceWarning(machine, "CPU", currentCpu, 85);
        addResourceWarning(machine, "RAM", currentRam, 85);
        addResourceWarning(machine, "Disk", currentDisk, 90);
        return new MachineDtos.HeartbeatResponse(true, machine.getId(), now);
    }

    @Transactional
    public MachineDtos.MachineLogResponse addApplicationLog(String username, Long machineId, MachineDtos.CreateLogRequest request) {
        UserEntity user = user(username);
        MachineEntity machine = owned(machineId, user.getId());
        MachineLogEntity.Level level = normalizeLevel(request == null ? null : request.level);
        MachineLogEntity.Source source = normalizeSource(request == null ? null : request.source);
        String category = trim(request == null ? null : request.category, 50);
        String message = required(request == null ? null : request.message, "message", 10000);
        return logResponse(addLog(machine, level, source, category, message));
    }

    @Transactional
    public MachineDtos.DeleteResponse delete(String username, Long machineId) {
        UserEntity user = user(username);
        MachineEntity machine = owned(machineId, user.getId());
        machineRepo.delete(machine);
        return new MachineDtos.DeleteResponse(true, machineId);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int markOffline(LocalDateTime cutoff) {
        LocalDateTime changedAt = LocalDateTime.now().withNano(0);
        List<Long> staleIds = jdbc.queryForList(
                "select computer_id from monitored_computer " +
                        "where status = 'ONLINE' and last_seen < ? order by computer_id",
                Long.class,
                java.sql.Timestamp.valueOf(cutoff)
        );
        if (staleIds.isEmpty()) return 0;

        int updated = jdbc.update(
                "update monitored_computer set status = 'OFFLINE', disconnected_at = ?, " +
                        "cpu_usage_percent = 0, ram_used_bytes = 0, disk_used_bytes = 0 " +
                        "where status = 'ONLINE' and last_seen < ?",
                java.sql.Timestamp.valueOf(changedAt),
                java.sql.Timestamp.valueOf(cutoff)
        );
        if (updated == 0) return 0;

        jdbc.update(
                "insert into machine_logs (computer_id, log_time, level, source, category, message) " +
                        "select computer_id, ?, 'ERROR', 'SYSTEM', null, 'Computer went offline' " +
                        "from monitored_computer where status = 'OFFLINE' and disconnected_at = ?",
                java.sql.Timestamp.valueOf(changedAt),
                java.sql.Timestamp.valueOf(changedAt)
        );
        for (Long machineId : staleIds) {
            String prefix = machineId + ":";
            activeResourceWarnings.removeIf(key -> key.startsWith(prefix));
        }
        return updated;
    }

    private MachineLogEntity addLog(MachineEntity machine, MachineLogEntity.Level level, String message) {
        return addLog(machine, level, MachineLogEntity.Source.SYSTEM, null, message);
    }

    private MachineLogEntity addLog(MachineEntity machine, MachineLogEntity.Level level,
                                    MachineLogEntity.Source source, String category, String message) {
        MachineLogEntity log = new MachineLogEntity();
        log.setMachine(machine);
        log.setLevel(level);
        log.setSource(source);
        log.setCategory(category);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        return logRepo.save(log);
    }

    private MachineDtos.MachineResponse response(MachineEntity machine) {
        int cpu = machine.isOnline() && machine.getCpuUsagePercent() != null
                ? clamp(machine.getCpuUsagePercent().intValue()) : 0;
        int ram = machine.isOnline() ? usage(machine.getRamUsedBytes(), machine.getRamTotalBytes()) : 0;
        int disk = machine.isOnline() ? usage(machine.getDiskUsedBytes(), machine.getDiskTotalBytes()) : 0;
        String os = join(machine.getOsName(), machine.getOsVersion(), machine.getOsArchitecture());
        return new MachineDtos.MachineResponse(machine.getId(), machine.getDeviceKey(), machine.getName(),
                machine.isOnline() ? "online" : "offline", machine.getIpAddress(), os, machine.getCpuName(),
                bytesLabel(machine.getRamTotalBytes()), bytesLabel(machine.getDiskTotalBytes()),
                machine.getLastSeen(), cpu, ram, disk, cpu >= 85 || ram >= 85 || disk >= 90);
    }

    private int cpuUsage(MachineEntity machine) {
        return machine.getCpuUsagePercent() == null ? 0
                : clamp((int) Math.round(machine.getCpuUsagePercent().doubleValue()));
    }
    private void addResourceWarning(MachineEntity machine, String resource, int current, int threshold) {
        String key = machine.getId() + ":" + resource;
        if (current >= threshold) {
            if (activeResourceWarnings.add(key)) {
                addLog(machine, MachineLogEntity.Level.WARNING,
                        resource + " usage is critically high: " + current + "%");
            }
        } else {
            activeResourceWarnings.remove(key);
        }
    }
    private void clearResourceWarnings(MachineEntity machine) {
        String prefix = machine.getId() + ":";
        activeResourceWarnings.removeIf(key -> key.startsWith(prefix));
    }
    private java.math.BigDecimal percent(java.math.BigDecimal value) {
        if (value == null) return null;
        return value.max(java.math.BigDecimal.ZERO).min(java.math.BigDecimal.valueOf(100));
    }
    private Long nonNegative(Long value) { return value == null ? null : Math.max(0L, value); }
    private int usage(Long used, Long total) {
        if (used == null || total == null || total <= 0) return 0;
        return clamp((int) Math.round(used * 100.0 / total));
    }
    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private String bytesLabel(Long bytes) {
        if (bytes == null) return null;
        double gib = bytes / 1073741824.0;
        return gib >= 100 ? String.format(Locale.ROOT, "%.0f GB", gib) : String.format(Locale.ROOT, "%.1f GB", gib);
    }
    private String join(String... parts) {
        String value = java.util.Arrays.stream(parts).filter(part -> part != null && !part.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        return value.isBlank() ? null : value;
    }

    private MachineDtos.MachineLogResponse logResponse(MachineLogEntity log) {
        return new MachineDtos.MachineLogResponse(log.getId(), log.getMachine().getId(), log.getMachine().getName(),
                frontendLevel(log.getLevel()), log.getSource().name().toLowerCase(Locale.ROOT), log.getCategory(),
                log.getMessage(), log.getCreatedAt());
    }

    private UserEntity user(String username) {
        return userRepo.findReadableByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }
    private MachineEntity owned(Long id, Long userId) {
        return machineRepo.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "machine not found"));
    }
    private String required(String value, String field, int max) {
        String out = trim(value, max);
        if (out == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " required");
        return out;
    }
    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String out = value.trim();
        return out.length() > max ? out.substring(0, max) : out;
    }
    private MachineLogEntity.Source normalizeSource(String value) {
        String source = value == null ? "PANEL" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PANEL", "FARM").contains(source))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid log source");
        return MachineLogEntity.Source.valueOf(source);
    }
    private MachineLogEntity.Level normalizeLevel(String value) {
        String level = value == null ? "INFO" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL").contains(level))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid log level");
        return MachineLogEntity.Level.valueOf(level);
    }
    private String frontendLevel(MachineLogEntity.Level level) {
        if (level == null) return "info";
        return switch (level.name()) {
            case "WARNING" -> "warning";
            case "ERROR", "CRITICAL" -> "error";
            default -> "info";
        };
    }
}

