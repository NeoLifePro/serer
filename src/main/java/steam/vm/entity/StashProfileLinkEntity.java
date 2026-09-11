package steam.vm.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="stash_profile_link")
public class StashProfileLinkEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="link_id") private Long linkId;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="stash_profile_id",nullable=false) private ProfileEntity stashProfile;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="farm_profile_id",nullable=false) private ProfileEntity farmProfile;
 @Column(name="linked_at",nullable=false,insertable=false,updatable=false) private LocalDateTime linkedAt;
 public Long getLinkId(){return linkId;} public ProfileEntity getStashProfile(){return stashProfile;} public void setStashProfile(ProfileEntity v){stashProfile=v;}
 public ProfileEntity getFarmProfile(){return farmProfile;} public void setFarmProfile(ProfileEntity v){farmProfile=v;} public LocalDateTime getLinkedAt(){return linkedAt;}
}