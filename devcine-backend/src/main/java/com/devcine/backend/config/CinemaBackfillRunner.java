package com.devcine.backend.config;

import com.devcine.backend.entity.Cinema;
import com.devcine.backend.repository.CinemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Backfill (idempotent) cho các cụm rạp đã tồn tại TRƯỚC khi có trường {@code district}:
 * với mọi cụm rạp đang bỏ trống Quận/Huyện, suy district từ chuỗi {@code address} và điền vào.
 *
 * <p>Chỉ đụng tới cụm có district trống nên an toàn khi chạy lại: sau lần đầu điền xong,
 * các lần khởi động sau không còn cụm nào trống -> no-op. Không đoán bừa: nếu địa chỉ không
 * khớp danh mục quận/huyện đã biết thì để nguyên (log cảnh báo) cho admin tự sửa qua UI.
 */
@Component
@Order(101)
public class CinemaBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CinemaBackfillRunner.class);

    private final CinemaRepository cinemaRepository;

    public CinemaBackfillRunner(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    // Map loại cụm rạp legacy (không thuộc danh mục hợp lệ) -> giá trị chuẩn Lotte, để updateCinema không 400.
    private static final java.util.Map<String, String> LEGACY_TYPE_FIX = java.util.Map.of(
            "Standard/Sweetbox", "Sweetbox",
            "Premium/IMAX ", "Superplex",
            "Premium/IMAX", "Superplex",
            "Gold Class", "Cine Comfort",
            "Deluxe", "Cine Comfort"
    );

    // Danh mục quận/huyện để dò trong địa chỉ. Thứ tự quan trọng: các mục dài/đặc thù đứng trước
    // để "Quận 1" không "ăn" nhầm địa chỉ của "Quận 10/11/12".
    private static final List<String> DISTRICTS = List.of(
            // TP. Hồ Chí Minh — quận đánh số (12→10 trước để tránh trùng tiền tố với "Quận 1")
            "Quận 12", "Quận 11", "Quận 10",
            "Quận 9", "Quận 8", "Quận 7", "Quận 6", "Quận 5", "Quận 4", "Quận 3", "Quận 2", "Quận 1",
            // TP. Hồ Chí Minh — quận/huyện có tên
            "Bình Thạnh", "Thủ Đức", "Gò Vấp", "Tân Bình", "Tân Phú", "Phú Nhuận",
            "Bình Tân", "Bình Chánh", "Nhà Bè", "Hóc Môn", "Củ Chi", "Cần Giờ",
            // Hà Nội — một số quận thường gặp
            "Hoàn Kiếm", "Ba Đình", "Đống Đa", "Hai Bà Trưng", "Cầu Giấy", "Thanh Xuân",
            "Hà Đông", "Tây Hồ", "Long Biên", "Nam Từ Liêm", "Bắc Từ Liêm", "Hoàng Mai"
    );

    @Override
    public void run(String... args) {
        int filled = 0, skipped = 0;
        int typeFixed = 0;
        for (Cinema c : cinemaRepository.findAll()) {
            boolean dirty = false;

            // Chuẩn hoá loại cụm rạp legacy không còn hợp lệ (chặn 400 khi cập nhật qua updateCinema).
            String canonicalType = LEGACY_TYPE_FIX.get(c.getType());
            if (canonicalType != null) {
                c.setType(canonicalType);
                dirty = true;
                typeFixed++;
            }

            // Điền Quận/Huyện nếu đang trống (suy từ address).
            if (c.getDistrict() == null || c.getDistrict().isBlank()) {
                String district = extractDistrict(c.getAddress());
                if (district != null) {
                    c.setDistrict(district);
                    dirty = true;
                    filled++;
                } else {
                    skipped++;
                    log.warn("[CinemaBackfill] Không suy được Quận/Huyện cho cụm rạp #{} '{}' (địa chỉ: {}).",
                            c.getId(), c.getName(), c.getAddress());
                }
            }

            if (dirty) cinemaRepository.save(c);
        }
        if (typeFixed > 0) {
            log.info("[CinemaBackfill] Chuẩn hoá loại cụm rạp legacy: {} cụm được cập nhật.", typeFixed);
        }
        if (filled > 0 || skipped > 0) {
            log.info("[CinemaBackfill] Điền Quận/Huyện: {} cụm rạp được cập nhật, {} cụm không suy được (cần sửa tay).",
                    filled, skipped);
        }
    }

    /** Dò quận/huyện đầu tiên khớp trong địa chỉ; null nếu không tìm thấy. */
    private String extractDistrict(String address) {
        if (address == null || address.isBlank()) return null;
        String lower = address.toLowerCase();
        for (String d : DISTRICTS) {
            if (lower.contains(d.toLowerCase())) return d;
        }
        return null;
    }
}
