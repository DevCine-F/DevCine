package com.devcine.backend.service;

import com.devcine.backend.dto.request.AgeRatingRequest;
import com.devcine.backend.dto.request.CategoryRequest;
import com.devcine.backend.dto.request.MovieFormatRequest;
import com.devcine.backend.entity.AgeRating;
import com.devcine.backend.entity.Category;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.repository.AgeRatingRepository;
import com.devcine.backend.repository.CategoryRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.ShowtimeRepository;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quản lý danh mục phim: Thể loại (Category), Định dạng (Format), Kiểm duyệt độ tuổi (AgeRating).
 * Tự seed dữ liệu mặc định lần đầu và chặn xoá thể loại đang được phim sử dụng.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MovieFormatRepository movieFormatRepository;
    private final AgeRatingRepository ageRatingRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    /**
     * Chuẩn hóa tên định dạng phim theo Title Case chuẩn (VD: "2D Phụ Đề", "3D Lồng Tiếng", "Superplex 2D").
     * Tự động giữ hoa các tiền tố công nghệ (2D, 3D, 4D, 4DX, IMAX).
     */
    public static String formatMovieFormatName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String[] words = raw.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            w = w.trim();
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            String lower = w.toLowerCase();
            if (lower.equals("2d") || lower.equals("3d") || lower.equals("4d") || lower.equals("4dx") || lower.equals("imax")) {
                sb.append(w.toUpperCase());
            } else {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    // ===================== THỂ LOẠI (GENRES) =====================

    @Transactional
    public List<Category> getGenres() {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    Category.builder().name("Hành động").description("Phim hành động").build(),
                    Category.builder().name("Tình cảm").description("Phim tình cảm").build(),
                    Category.builder().name("Hài hước").description("Phim hài hước").build(),
                    Category.builder().name("Kinh dị").description("Phim kinh dị").build(),
                    Category.builder().name("Hoạt hình").description("Phim hoạt hình").build(),
                    Category.builder().name("Viễn tưởng").description("Phim viễn tưởng").build()
            ));
        }
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createGenre(CategoryRequest input) {
        String name = requireName(input.getName(), "Tên thể loại không được để trống");
        checkGenreName(name);
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Thể loại \"" + name + "\" đã tồn tại");
        }
        return categoryRepository.save(Category.builder()
                .name(name)
                .description(cleanDescription(input.getDescription()))
                .build());
    }

    @Transactional
    public Category updateGenre(Integer id, CategoryRequest input) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thể loại #" + id));
        String name = requireName(input.getName(), "Tên thể loại không được để trống");
        checkGenreName(name);
        if (!name.equalsIgnoreCase(existing.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Thể loại \"" + name + "\" đã tồn tại");
        }
        existing.setName(name);
        existing.setDescription(cleanDescription(input.getDescription()));
        return categoryRepository.save(existing);
    }

    @Transactional
    public void deleteGenre(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy thể loại #" + id);
        }
        long used = movieRepository.countByGenres_Id(id);
        if (used > 0) {
            throw new IllegalStateException("Không thể xoá: thể loại đang được " + used + " phim sử dụng");
        }
        categoryRepository.deleteById(id);
    }

    // ===================== ĐỊNH DẠNG (FORMATS) =====================

    // Dùng CHUNG bảng movie_formats (gắn vào Suất chiếu + Cấu hình giá) — một nguồn định dạng duy nhất.
    @Transactional
    public List<MovieFormat> getFormats() {
        if (movieFormatRepository.count() == 0) {
            movieFormatRepository.saveAll(List.of(
                    MovieFormat.builder().name("2D Phụ Đề").description("2D Phụ Đề").surcharge(BigDecimal.ZERO).build(),
                    MovieFormat.builder().name("2D Lồng Tiếng").description("2D Lồng Tiếng").surcharge(BigDecimal.ZERO).build(),
                    MovieFormat.builder().name("3D Phụ Đề").description("3D Phụ Đề").surcharge(new BigDecimal("30000")).build(),
                    MovieFormat.builder().name("3D Lồng Tiếng").description("3D Lồng Tiếng").surcharge(new BigDecimal("30000")).build(),
                    MovieFormat.builder().name("Superplex 2D").description("Superplex 2D").surcharge(new BigDecimal("20000")).build(),
                    MovieFormat.builder().name("Superplex 3D").description("Superplex 3D").surcharge(new BigDecimal("50000")).build()
            ));
        }
        return movieFormatRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public MovieFormat createFormat(MovieFormatRequest input) {
        String name = formatMovieFormatName(requireName(input.getName(), "Tên định dạng không được để trống"));
        checkNameLen(name, 2, 30, "định dạng");
        if (movieFormatRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Định dạng \"" + name + "\" đã tồn tại");
        }
        return movieFormatRepository.save(MovieFormat.builder()
                .name(name)
                .description(cleanDescription(input.getDescription()))
                .surcharge(BigDecimal.ZERO)
                .build());
    }

    // Chỉ sửa tên + mô tả; phụ thu/giá cố định được chỉnh ở màn "Cấu hình giá".
    @Transactional
    public MovieFormat updateFormat(Integer id, MovieFormatRequest input) {
        MovieFormat existing = movieFormatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy định dạng #" + id));
        String name = formatMovieFormatName(requireName(input.getName(), "Tên định dạng không được để trống"));
        checkNameLen(name, 2, 30, "định dạng");
        if (!name.equalsIgnoreCase(existing.getName()) && movieFormatRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Định dạng \"" + name + "\" đã tồn tại");
        }
        existing.setName(name);
        existing.setDescription(cleanDescription(input.getDescription()));
        return movieFormatRepository.save(existing);
    }

    @Transactional
    public void deleteFormat(Integer id) {
        if (!movieFormatRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy định dạng #" + id);
        }
        if (showtimeRepository.existsByFormat_Id(id)) {
            throw new IllegalStateException("Không thể xoá: định dạng đang được suất chiếu sử dụng");
        }
        movieFormatRepository.deleteById(id);
    }

    // ===================== KIỂM DUYỆT (AGE RATINGS) =====================

    @Transactional
    public List<AgeRating> getAgeRatings() {
        if (ageRatingRepository.count() == 0) {
            ageRatingRepository.saveAll(List.of(
                    AgeRating.builder().code("P").name("Mọi đối tượng").build(),
                    AgeRating.builder().code("K").name("Dưới 13 tuổi (có người lớn đi kèm)").build(),
                    AgeRating.builder().code("T13").name("Từ 13 tuổi").build(),
                    AgeRating.builder().code("T16").name("Từ 16 tuổi").build(),
                    AgeRating.builder().code("T18").name("Từ 18 tuổi").build()
            ));
        }
        return ageRatingRepository.findAll();
    }

    @Transactional
    public AgeRating createAgeRating(AgeRatingRequest input) {
        String code = requireName(input.getCode(), "Mã kiểm duyệt không được để trống").toUpperCase();
        checkAgeRatingCode(code);
        String name = requireName(input.getName(), "Tên kiểm duyệt không được để trống");
        checkNameLen(name, 2, 50, "kiểm duyệt");
        if (ageRatingRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt \"" + code + "\" đã tồn tại");
        }
        return ageRatingRepository.save(AgeRating.builder()
                .code(code)
                .name(name)
                .description(cleanDescription(input.getDescription()))
                .build());
    }

    @Transactional
    public AgeRating updateAgeRating(Integer id, AgeRatingRequest input) {
        AgeRating existing = ageRatingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục kiểm duyệt #" + id));
        String code = requireName(input.getCode(), "Mã kiểm duyệt không được để trống").toUpperCase();
        checkAgeRatingCode(code);
        String name = requireName(input.getName(), "Tên kiểm duyệt không được để trống");
        checkNameLen(name, 2, 50, "kiểm duyệt");
        if (!code.equalsIgnoreCase(existing.getCode()) && ageRatingRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt \"" + code + "\" đã tồn tại");
        }
        existing.setCode(code);
        existing.setName(name);
        existing.setDescription(cleanDescription(input.getDescription()));
        return ageRatingRepository.save(existing);
    }

    @Transactional
    public void deleteAgeRating(Integer id) {
        if (!ageRatingRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy mục kiểm duyệt #" + id);
        }
        ageRatingRepository.deleteById(id);
    }

    // ===================== HELPERS =====================

    private String requireName(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Ký tự đặc biệt nguy hiểm bị chặn trong tên danh mục (đồng bộ với Frontend).
    private static final java.util.regex.Pattern FORBIDDEN_NAME =
            java.util.regex.Pattern.compile("[@#$%^&*<>/,\\[\\]{}]");

    private void checkNameLen(String name, int min, int max, String label) {
        if (name.length() < min || name.length() > max) {
            throw new IllegalArgumentException("Tên " + label + " phải từ " + min + " đến " + max + " ký tự");
        }
        if (FORBIDDEN_NAME.matcher(name).find()) {
            throw new IllegalArgumentException("Tên " + label + " chứa ký tự không hợp lệ");
        }
    }

    // Riêng THỂ LOẠI: không cho chứa chữ số (Định dạng cần 2D/3D nên không áp).
    private void checkGenreName(String name) {
        checkNameLen(name, 2, 30, "thể loại");
        if (name.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Tên thể loại không được chứa chữ số");
        }
    }

    // Mã kiểm duyệt: 1-10 ký tự, chỉ chữ in hoa + số (P, T13, C18...) — đồng bộ với Frontend.
    // Mã kiểm duyệt phải thuộc BỘ CHUẨN phân loại phim Việt Nam (Thông tư 05/2023) — đồng bộ với Frontend.
    // P: mọi lứa tuổi · K: dưới 13 tuổi (có người bảo hộ) · T13/T16/T18: từ 13/16/18 tuổi · C: cấm phổ biến.
    // Chặn các mã sai chuẩn kiểu "C13/C16/C18" (nhầm T thành C).
    private static final java.util.Set<String> STANDARD_AGE_CODES =
            java.util.Set.of("P", "K", "T13", "T16", "T18", "C");

    private void checkAgeRatingCode(String code) {
        if (!STANDARD_AGE_CODES.contains(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt phải thuộc bộ chuẩn: P, K, T13, T16, T18, C");
        }
    }

    // Mô tả (tùy chọn): tối đa 150 ký tự; trả về null nếu rỗng.
    private String cleanDescription(String desc) {
        String d = trimToNull(desc);
        if (d != null && d.length() > 150) {
            throw new IllegalArgumentException("Mô tả tối đa 150 ký tự");
        }
        return d;
    }
}
