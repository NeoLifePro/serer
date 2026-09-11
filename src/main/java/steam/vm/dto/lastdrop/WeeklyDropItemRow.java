package steam.vm.dto.lastdrop;

public class WeeklyDropItemRow {
    public String itemName;
    public String itemWearAndTear;
    public String profileLogin;
    public Long itemCount;
    public Double totalPrice;
    public Double avgPrice;

    public WeeklyDropItemRow(
            String itemName,
            String itemWearAndTear,
            String profileLogin,
            Long itemCount,
            Double totalPrice,
            Double avgPrice
    ) {
        this.itemName = itemName;
        this.itemWearAndTear = itemWearAndTear;
        this.profileLogin = profileLogin;
        this.itemCount = itemCount;
        this.totalPrice = totalPrice;
        this.avgPrice = avgPrice;
    }
}
