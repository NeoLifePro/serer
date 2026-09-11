package steam.vm.dto;

public class ItemDtos {
    public static class ItemPriceResponse {
        public Double item_price;
        public ItemPriceResponse(Double item_price){
            this.item_price = item_price;
        }
    }

    public static class ItemAvgPriceResponse{
        public Double avg_price;

        public ItemAvgPriceResponse(Double avg_price){
            this.avg_price = avg_price;
        }
    }
}
