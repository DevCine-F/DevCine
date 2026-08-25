package com.devcine.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosCheckoutRequestDTO {
    private String paymentMethod;
    private Integer customerId;
    @NotNull(message = "Danh sách món không được để trống")
    @Valid
    private List<PosFnbItemDTO> fnbs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PosFnbItemDTO {
        @JsonAlias({"fnbItemId", "itemId", "id", "fnbId"})
        @NotNull(message = "ItemId không được để trống")
        private Integer itemId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;

        private List<com.devcine.backend.dto.request.FnbOptionSelectionDTO> options;
    }
}
