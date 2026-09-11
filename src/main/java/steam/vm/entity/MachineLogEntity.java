package steam.vm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "machine_logs")
public class MachineLogEntity {
    public enum Level { DEBUG, INFO, WARNING, ERROR, CRITICAL }
    public enum Source { SYSTEM, PANEL, FARM }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "computer_id", nullable = false)
    private MachineEntity machine;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('DEBUG','INFO','WARNING','ERROR','CRITICAL')")
    private Level level;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('SYSTEM','PANEL','FARM')")
    private Source source = Source.SYSTEM;
    @Column(length = 50)
    private String category;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(name = "log_time", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public MachineEntity getMachine() { return machine; }
    public void setMachine(MachineEntity machine) { this.machine = machine; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

