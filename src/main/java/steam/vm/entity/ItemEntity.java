package steam.vm.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "items")
public class ItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "item_float")
    private Double itemFloat;

    @Column(name = "item_wear_and_tear", length = 64)
    private String itemWearAndTear;

    @Column(name = "item_price", nullable = false)
    private double itemPrice = 0.00;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getItemFloat() {
        return itemFloat;
    }

    public void setItemFloat(Double itemFloat) {
        this.itemFloat = itemFloat;
    }

    public String getItemWearAndTear() {
        return itemWearAndTear;
    }

    public void setItemWearAndTear(String itemWearAndTear) {
        this.itemWearAndTear = itemWearAndTear;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
