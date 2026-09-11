package steam.vm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vm_panel")
public class VmPanelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vm_id")
    private Long vmId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "vm_name", nullable = false, length = 128)
    private String vmName;

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public LocalDateTime getLastTimeStarted() {
        return lastTimeStarted;
    }

    public void setLastTimeStarted(LocalDateTime lastTimeStarted) {
        this.lastTimeStarted = lastTimeStarted;
    }

    @Column(name = "last_time_started")
    private LocalDateTime lastTimeStarted;


}
