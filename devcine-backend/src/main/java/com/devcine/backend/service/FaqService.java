package com.devcine.backend.service;

import com.devcine.backend.dto.request.FaqRequest;
import com.devcine.backend.entity.Faq;
import com.devcine.backend.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý nội dung FAQ hiển thị ở trang Hỗ trợ. */
@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<Faq> getPublicFaqs() {
        return faqRepository.findByIsActiveTrueOrderByCategoryAscDisplayOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<Faq> getAllFaqs() {
        return faqRepository.findAllByOrderByCategoryAscDisplayOrderAscIdAsc();
    }

    @Transactional
    public Faq create(FaqRequest input) {
        validate(input);
        return faqRepository.save(Faq.builder()
                .category(input.getCategory().trim())
                .question(input.getQuestion().trim())
                .answer(trimToNull(input.getAnswer()))
                .displayOrder(input.getDisplayOrder() != null ? input.getDisplayOrder() : 0)
                .isActive(input.getIsActive() != null ? input.getIsActive() : true)
                .build());
    }

    @Transactional
    public Faq update(Integer id, FaqRequest input) {
        Faq existing = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy FAQ #" + id));
        validate(input);
        existing.setCategory(input.getCategory().trim());
        existing.setQuestion(input.getQuestion().trim());
        existing.setAnswer(trimToNull(input.getAnswer()));
        if (input.getDisplayOrder() != null) existing.setDisplayOrder(input.getDisplayOrder());
        if (input.getIsActive() != null) existing.setIsActive(input.getIsActive());
        return faqRepository.save(existing);
    }

    @Transactional
    public void delete(Integer id) {
        if (!faqRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy FAQ #" + id);
        }
        faqRepository.deleteById(id);
    }

    private void validate(FaqRequest f) {
        if (f.getCategory() == null || f.getCategory().isBlank())
            throw new IllegalArgumentException("Danh mục không được để trống");
        if (f.getQuestion() == null || f.getQuestion().isBlank())
            throw new IllegalArgumentException("Câu hỏi không được để trống");
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
