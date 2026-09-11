package steam.vm.dto;

import java.util.List;

public class AccountDtos {

    public static class PageResponse<T> {
        public List<T> content;
        public int number;
        public int size;
        public long totalElements;
        public int totalPages;

        public PageResponse(List<T> content, int number, int size, long totalElements, int totalPages) {
            this.content = content;
            this.number = number;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }

    public static class AccountCountResponse{
        public long total;
        public long farmed;
        public long needFarm;
        public long blocked;

        public AccountCountResponse(long total, long farmed, long needFarm, long blocked){
            this.total = total;
            this.farmed = farmed;
            this.needFarm = needFarm;
            this.blocked = blocked;
        }
    }
}
