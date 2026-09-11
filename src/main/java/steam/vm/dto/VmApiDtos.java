package steam.vm.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDateTime;

public class VmApiDtos {
    public static class AddItemRequest {
        @JsonAlias("profile_id")
        public Long profileId;

        @JsonAlias("item_name")
        public String itemName;

        @JsonAlias("item_wear_and_tear")
        public String itemWearAndTear;

        @JsonAlias("item_price")
        public Double itemPrice;

        @JsonAlias("created_at")
        public LocalDateTime createdAt;
    }

    public static class AddItemResponse {
        public boolean ok;
        public Long itemId;
        public Long profileId;

        public AddItemResponse(boolean ok, Long itemId, Long profileId) {
            this.ok = ok;
            this.itemId = itemId;
            this.profileId = profileId;
        }
    }

    public static class VmProfileResponse {
        public Long profileId;
        public String login;
        public String vmName;

        public VmProfileResponse(Long profileId, String login, String vmName) {
            this.profileId = profileId;
            this.login = login;
            this.vmName = vmName;
        }
    }

    public static class ProfilePasswordResponse {
        public Long profileId;
        public String login;
        public String password;

        public ProfilePasswordResponse(Long profileId, String login, String password) {
            this.profileId = profileId;
            this.login = login;
            this.password = password;
        }
    }

    public static class ProfileIdResponse {
        public Long profileId;

        public ProfileIdResponse(Long profileId) {
            this.profileId = profileId;
        }
    }

    public static class FarmTimeRequest {
        @JsonAlias("profile_id")
        public Long profileId;

        @JsonAlias({"farm_time_seconds", "farm_timeseconds", "farmTimeSeconds"})
        public Integer farmTimeSeconds;

        @JsonAlias("created_at")
        public LocalDateTime createdAt;
    }

    public static class FarmTimeResponse {
        public boolean ok;
        public Long farmTimeId;
        public Long profileId;

        public FarmTimeResponse(boolean ok, Long farmTimeId, Long profileId) {
            this.ok = ok;
            this.farmTimeId = farmTimeId;
            this.profileId = profileId;
        }
    }

    public static class VmAvgFarmTime{
        public Long farmTime;
        public  VmAvgFarmTime(Long farmTime){
            this.farmTime = farmTime;
        }
    }

    public static class ProfileUpdateRequest {
        public String login;
    }

    public static class ProfileUpdateResponse {
        public boolean ok;
        public Long profileId;
        public String login;
        public boolean farmed;

        public ProfileUpdateResponse(boolean ok, Long profileId, String login, boolean farmed) {
            this.ok = ok;
            this.profileId = profileId;
            this.login = login;
            this.farmed = farmed;
        }
    }
}
