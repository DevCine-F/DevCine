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
                    Category.builder().name("Hành động").description("Kịch tính, gay cấn với các cảnh chiến đấu, rượt đuổi và kỹ xảo mãn nhãn").build(),
                    Category.builder().name("Tình cảm").description("Những câu chuyện lãng mạn, sâu lắng về tình yêu và cảm xúc con người").build(),
                    Category.builder().name("Hài hước").description("Nội dung vui nhộn, dí dỏm mang lại tiếng cười và phút giây thư giãn").build(),
                    Category.builder().name("Kinh dị").description("Tạo cảm giác sợ hãi, rùng rợn và hồi hộp với yếu tố siêu nhiên, kỳ bí").build(),
                    Category.builder().name("Hoạt hình").description("Thế giới đồ họa sống động, phù hợp mọi lứa tuổi và gia đình").build(),
                    Category.builder().name("Viễn tưởng").description("Khám phá tương lai, không gian, công nghệ tiên tiến và thế giới tưởng tượng").build()
            ));
        }
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public Category createGenre(CategoryRequest input) {
        String name = requireName(input.getName(), "Tên danh mục không được để trống");
        checkGenreName(name);
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại");
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
        String name = requireName(input.getName(), "Tên danh mục không được để trống");
        checkGenreName(name);
        if (!name.equalsIgnoreCase(existing.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại");
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
                    MovieFormat.builder().name("2D Phụ Đề").description("Hình ảnh 2D tiêu chuẩn, âm thanh gốc kèm phụ đề tiếng Việt").surcharge(BigDecimal.ZERO).build(),
                    MovieFormat.builder().name("2D Lồng Tiếng").description("Hình ảnh 2D tiêu chuẩn, âm thanh lồng tiếng Việt phù hợp gia đình và trẻ em").surcharge(BigDecimal.ZERO).build(),
                    MovieFormat.builder().name("3D Phụ Đề").description("Hiệu ứng không gian 3 chiều sống động qua kính 3D, âm thanh gốc kèm phụ đề tiếng Việt").surcharge(new BigDecimal("30000")).build(),
                    MovieFormat.builder().name("3D Lồng Tiếng").description("Hiệu ứng không gian 3 chiều sống động qua kính 3D, âm thanh lồng tiếng Việt sinh động").surcharge(new BigDecimal("30000")).build(),
                    MovieFormat.builder().name("Superplex 2D").description("Màn chiếu siêu đại Superplex kích thước khổng lồ, hình ảnh 2D sắc nét vượt trội").surcharge(new BigDecimal("20000")).build(),
                    MovieFormat.builder().name("Superplex 3D").description("Màn chiếu siêu đại Superplex kết hợp không gian 3D hoành tráng và âm thanh đỉnh cao").surcharge(new BigDecimal("50000")).build()
            ));
        }
        return movieFormatRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public MovieFormat createFormat(MovieFormatRequest input) {
        String name = formatMovieFormatName(requireName(input.getName(), "Tên danh mục không được để trống"));
        checkNameLen(name, 2, 50, "định dạng");
        if (movieFormatRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại");
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
        String name = formatMovieFormatName(requireName(input.getName(), "Tên danh mục không được để trống"));
        checkNameLen(name, 2, 50, "định dạng");
        if (!name.equalsIgnoreCase(existing.getName()) && movieFormatRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại");
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
                    AgeRating.builder().code("P").name("Mọi đối tượng").description("Phim được phép phổ biến rộng rãi đến mọi lứa tuổi người xem").build(),
                    AgeRating.builder().code("K").name("Dưới 13 tuổi (có người lớn đi kèm)").description("Phim dành cho khán giả dưới 13 tuổi với điều kiện có cha mẹ hoặc người giám hộ đi cùng").build(),
                    AgeRating.builder().code("T13").name("Từ 13 tuổi").description("Phim chỉ dành cho khán giả từ đủ 13 tuổi trở lên (13+)").build(),
                    AgeRating.builder().code("T16").name("Từ 16 tuổi").description("Phim chỉ dành cho khán giả từ đủ 16 tuổi trở lên (16+)").build(),
                    AgeRating.builder().code("T18").name("Từ 18 tuổi").description("Phim chỉ dành cho khán giả từ đủ 18 tuổi trở lên (18+)").build()
            ));
        }
        return ageRatingRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public AgeRating createAgeRating(AgeRatingRequest input) {
        String code = requireName(input.getCode(), "Mã kiểm duyệt không được để trống").toUpperCase();
        checkAgeRatingCode(code);
        String name = requireName(input.getName(), "Tên danh mục không được để trống");
        checkNameLen(name, 2, 50, "kiểm duyệt");
        if (ageRatingRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt này đã tồn tại trên hệ thống");
        }
        if (ageRatingRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại");
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
        String name = requireName(input.getName(), "Tên danh mục không được để trống");
        checkNameLen(name, 2, 50, "kiểm duyệt");
        if (!code.equalsIgnoreCase(existing.getCode()) && ageRatingRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt này đã tồn tại trên hệ thống");
        }
        if (!name.equalsIgnoreCase(existing.getName()) && ageRatingRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục này đã tồn tại");
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
        return value.trim().replaceAll("\\s+", " ");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Ký tự đặc biệt nguy hiểm bị chặn trong tên danh mục (đồng bộ với Frontend).
    private static final java.util.regex.Pattern FORBIDDEN_NAME =
            java.util.regex.Pattern.compile("[@#$%^&*<>/,\\[\\]{}]");

    private void checkNameLen(String name, int min, int max, String label) {
        if (name.length() < min || name.length() > max) {
            throw new IllegalArgumentException("Tên danh mục phải từ " + min + " đến " + max + " ký tự");
        }
        if (FORBIDDEN_NAME.matcher(name).find()) {
            throw new IllegalArgumentException("Tên danh mục chứa ký tự không hợp lệ");
        }
    }

    // Riêng THỂ LOẠI: không cho chứa chữ số (Định dạng cần 2D/3D nên không áp).
    private void checkGenreName(String name) {
        checkNameLen(name, 2, 50, "thể loại");
        if (name.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Tên thể loại không được chứa chữ số");
        }
    }

    // Mã kiểm duyệt: 1-10 ký tự, chỉ chữ in hoa + số (P, K, T13, T16, T18, C) — đồng bộ với Frontend.
    private static final java.util.Set<String> STANDARD_AGE_CODES =
            java.util.Set.of("P", "K", "T13", "T16", "T18", "C");

    private void checkAgeRatingCode(String code) {
        if (code.length() > 10) {
            throw new IllegalArgumentException("Mã kiểm duyệt không vượt quá 10 ký tự");
        }
        if (!code.matches("^[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Mã chỉ được chứa chữ cái và số không dấu (VD: P, T13)");
        }
        if (!STANDARD_AGE_CODES.contains(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt phải thuộc bộ chuẩn: P, K, T13, T16, T18, C");
        }
    }

    // Mô tả (tùy chọn): tối đa 150 ký tự; trả về null nếu rỗng.
    private String cleanDescription(String desc) {
        String d = trimToNull(desc);
        if (d != null && d.length() > 150) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 150 ký tự");
        }
        return d;
    }
}
