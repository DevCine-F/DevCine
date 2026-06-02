package com.devcine.backend.controller;

import com.devcine.backend.entity.Category;
import com.devcine.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin("*")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/genres")
    public List<Category> getAllCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().name("Hành động").description("Phim hành động").build());
            categoryRepository.save(Category.builder().name("Tình cảm").description("Phim tình cảm").build());
            categoryRepository.save(Category.builder().name("Hài hước").description("Phim hài hước").build());
            categoryRepository.save(Category.builder().name("Kinh dị").description("Phim kinh dị").build());
            categoryRepository.save(Category.builder().name("Hoạt hình").description("Phim hoạt hình").build());
            categoryRepository.save(Category.builder().name("Viễn tưởng").description("Phim viễn tưởng").build());
        }
        return categoryRepository.findAll();
    }

    @GetMapping("/formats")
    public List<java.util.Map<String, String>> getFormats() {
        return List.of(
            java.util.Map.of("id", "1", "name", "2D"),
            java.util.Map.of("id", "2", "name", "3D"),
            java.util.Map.of("id", "3", "name", "IMAX")
        );
    }

    @GetMapping("/age-ratings")
    public List<java.util.Map<String, String>> getAgeRatings() {
        return List.of(
            java.util.Map.of("code", "P", "name", "Mọi đối tượng"),
            java.util.Map.of("code", "K", "name", "Dưới 13 tuổi"),
            java.util.Map.of("code", "T13", "name", "Từ 13 tuổi"),
            java.util.Map.of("code", "T16", "name", "Từ 16 tuổi"),
            java.util.Map.of("code", "T18", "name", "Từ 18 tuổi")
        );
    }
}
