package steam.vm.dto;
import java.util.List;
public class StashDtos {
 public static class SaveRequest { public String login; public String email; public String password; public Boolean blocked; public List<Long> farmProfileIds; }
 public record FarmAccount(Long profileId,String login,String vmName,boolean farmed,boolean blocked){}
 public record StashAccount(Long profileId,String login,String email,boolean blocked,boolean hasSteamGuard,List<FarmAccount> linkedAccounts){}
 public record StorageSnapshot(List<StashAccount> stashes,List<FarmAccount> farmAccounts){}
 public record Assignment(Long stashProfileId,String stashLogin,String stashEmail){}
 public record TradeProfile(Long profileId,String login,String password,String steamId64,String sharedSecret,String identitySecret,String tradeToken){}
 public record TradeGroup(TradeProfile stash,List<TradeProfile> farms){}
 public record TradeConfig(List<TradeGroup> groups){}
}