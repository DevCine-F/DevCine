package com.devcine.backend.service;

import com.devcine.backend.entity.AgeRating;
import com.devcine.backend.entity.Category;
import com.devcine.backend.entity.Format;
import com.devcine.backend.repository.AgeRatingRepository;
import com.devcine.backend.repository.CategoryRepository;
import com.devcine.backend.repository.FormatRepository;
import com.devcine.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
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
    private final FormatRepository formatRepository;
    private final AgeRatingRepository ageRatingRepository;
    private final MovieRepository movieRepository;

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
    public Category createGenre(Category input) {
        String name = requireName(input.getName(), "Tên thể loại không được để trống");
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Thể loại \"" + name + "\" đã tồn tại");
        }
        return categoryRepository.save(Category.builder()
                .name(name)
                .description(trimToNull(input.getDescription()))
                .build());
    }

    @Transactional
    public Category updateGenre(Integer id, Category input) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thể loại #" + id));
        String name = requireName(input.getName(), "Tên thể loại không được để trống");
        if (!name.equalsIgnoreCase(existing.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Thể loại \"" + name + "\" đã tồn tại");
        }
        existing.setName(name);
        existing.setDescription(trimToNull(input.getDescription()));
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

    @Transactional
    public List<Format> getFormats() {
        if (formatRepository.count() == 0) {
            formatRepository.saveAll(List.of(
                    Format.builder().name("2D").description("Định dạng tiêu chuẩn").build(),
                    Format.builder().name("3D").description("Định dạng 3 chiều").build(),
                    Format.builder().name("IMAX").description("Màn hình IMAX").build()
            ));
        }
        return formatRepository.findAll();
    }

    @Transactional
    public Format createFormat(Format input) {
        String name = requireName(input.getName(), "Tên định dạng không được để trống");
        if (formatRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Định dạng \"" + name + "\" đã tồn tại");
        }
        return formatRepository.save(Format.builder()
                .name(name)
                .description(trimToNull(input.getDescription()))
                .build());
    }

    @Transactional
    public Format updateFormat(Integer id, Format input) {
        Format existing = formatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy định dạng #" + id));
        String name = requireName(input.getName(), "Tên định dạng không được để trống");
        if (!name.equalsIgnoreCase(existing.getName()) && formatRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Định dạng \"" + name + "\" đã tồn tại");
        }
        existing.setName(name);
        existing.setDescription(trimToNull(input.getDescription()));
        return formatRepository.save(existing);
    }

    @Transactional
    public void deleteFormat(Integer id) {
        if (!formatRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy định dạng #" + id);
        }
        formatRepository.deleteById(id);
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
    public AgeRating createAgeRating(AgeRating input) {
        String code = requireName(input.getCode(), "Mã kiểm duyệt không được để trống").toUpperCase();
        String name = requireName(input.getName(), "Tên kiểm duyệt không được để trống");
        if (ageRatingRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt \"" + code + "\" đã tồn tại");
        }
        return ageRatingRepository.save(AgeRating.builder()
                .code(code)
                .name(name)
                .description(trimToNull(input.getDescription()))
                .build());
    }

    @Transactional
    public AgeRating updateAgeRating(Integer id, AgeRating input) {
        AgeRating existing = ageRatingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục kiểm duyệt #" + id));
        String code = requireName(input.getCode(), "Mã kiểm duyệt không được để trống").toUpperCase();
        String name = requireName(input.getName(), "Tên kiểm duyệt không được để trống");
        if (!code.equalsIgnoreCase(existing.getCode()) && ageRatingRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã kiểm duyệt \"" + code + "\" đã tồn tại");
        }
        existing.setCode(code);
        existing.setName(name);
        existing.setDescription(trimToNull(input.getDescription()));
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
}
