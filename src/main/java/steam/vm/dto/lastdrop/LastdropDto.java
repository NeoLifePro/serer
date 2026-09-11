package steam.vm.dto.lastdrop;

import java.time.LocalTime;

public class LastdropDto {
    public static class LastDropInfoPanelResponse{
        public long itemCount;
        public Double ItemsPrice;
        public long UniqueItems;
        public String lastUpdate;

        public LastDropInfoPanelResponse(long itemCount, Double ItemsPrice, long UniqueItems, String lastUpdate){
            this.itemCount = itemCount;
            this.ItemsPrice = ItemsPrice;
            this.UniqueItems = UniqueItems;
            this.lastUpdate = lastUpdate;
        }
    }
    public record UniqueItemResponse(
            String itemName,
            String itemWearAndTear,
            long itemCount,
            Double totalPrice,
            Double avgPrice
    ) {}



}
