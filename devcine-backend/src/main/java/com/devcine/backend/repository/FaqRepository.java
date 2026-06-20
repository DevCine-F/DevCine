package com.devcine.backend.repository;

import com.devcine.backend.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Integer> {

    /** Công khai: chỉ FAQ đang bật, sắp theo danh mục rồi thứ tự hiển thị. */
    List<Faq> findByIsActiveTrueOrderByCategoryAscDisplayOrderAscIdAsc();

    /** Quản trị: tất cả FAQ. */
    List<Faq> findAllByOrderByCategoryAscDisplayOrderAscIdAsc();

    /** Tìm theo danh mục (phục vụ backfill đổi tên danh mục). */
    List<Faq> findByCategory(String category);
}
