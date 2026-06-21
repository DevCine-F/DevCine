package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Payload cho thao tác hàng loạt trên phim (đổi trạng thái / xoá nhiều). */
@Data
public class MovieBulkRequest {

    @NotEmpty(message = "Danh sách ID không được rỗng")
    private List<Integer> ids;

    /** Trạng thái mới — chỉ dùng cho bulk-status (active | upcoming | archived). */
    private String status;
}
