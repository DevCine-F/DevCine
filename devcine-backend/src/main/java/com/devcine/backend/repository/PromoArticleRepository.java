package com.devcine.backend.repository;

import com.devcine.backend.entity.PromoArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromoArticleRepository extends JpaRepository<PromoArticle, Integer> {

    // Admin: tất cả tin, sắp theo thứ tự hiển thị rồi mới nhất
    List<PromoArticle> findAllByOrderByDisplayOrderAscIdDesc();

    // Khách: chỉ tin đang bật
    List<PromoArticle> findByIsActiveTrueOrderByDisplayOrderAscIdDesc();

    boolean existsByTitleIgnoreCase(String title);
}
