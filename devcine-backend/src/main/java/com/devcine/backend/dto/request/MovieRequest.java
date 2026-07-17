package com.devcine.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

/**
 * Dữ liệu tạo/sửa phim gửi từ màn quản trị (MovieFormModal.vue).
 * <p>
 * Ràng buộc cố ý "lỏng" — chỉ {@code title} bắt buộc; các trường còn lại chỉ kiểm tra khi CÓ giá trị
 * (để không chặn khi sửa phim cũ thiếu dữ liệu). Đồng bộ với {@code MovieService.validateMoviePayload}
 * (lớp phòng thủ thứ hai, còn giữ luật trailer Youtube).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRequest {

    @NotBlank(message = "Tên phim không được để trống")
    @Size(min = 2, max = 150, message = "Tên phim phải từ 2 đến 150 ký tự.")
    private String title;

    private String slug;

    @Min(value = 30, message = "Thời lượng phim phải từ 30 đến 300 phút.")
    @Max(value = 300, message = "Thời lượng phim phải từ 30 đến 300 phút.")
    private Integer durationMins;

    private String ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String status;
    private String country;
    private String rating;
    private String posterUrl;
    private String bannerUrl;
    private Boolean showOnBanner;
    private String trailerUrl;
    private String format;
    private String supportedFormats;
    private String titleVietnamese;

    @Min(value = 2020, message = "Năm sản xuất phải từ 2020 đến 2035.")
    @Max(value = 2035, message = "Năm sản xuất phải từ 2020 đến 2035.")
    private Integer productionYear;

    private String language;

    @DecimalMin(value = "10000", message = "Giá vé gốc phải từ 10.000đ trở lên.")
    private Double basePrice;

    @Size(max = 1000, message = "Tóm tắt nội dung tối đa 1000 ký tự.")
    private String description;

    private String originalLanguage;
    private String versionType;

    @Size(max = 500, message = "Ghi chú nội bộ tối đa 500 ký tự.")
    private String internalNotes;

    private LocalDate startDate;
    private String director;
    private String castMembers;
    private Integer ratingCount;

    /** Danh sách thể loại — FE gửi mảng object {id, name}; chỉ dùng id (name nhận cho đủ, không dùng). */
    private Set<GenreRef> genres;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreRef {
        private Integer id;
        private String name;
    }
}
