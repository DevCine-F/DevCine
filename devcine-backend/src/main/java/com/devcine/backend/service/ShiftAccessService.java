package com.devcine.backend.service;

import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.StaffScheduleRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftAccessService {

    private final StaffScheduleRepository staffScheduleRepository;

    @Transactional(readOnly = true)
    public Optional<StaffSchedule> findCurrentScheduleForCurrentUser() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Optional.empty();
        }
        return findCurrentScheduleForUser(currentUserId);
    }

    @Transactional(readOnly = true)
    public Optional<StaffSchedule> findCurrentScheduleForUser(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return staffScheduleRepository.findCurrentApprovedSchedules(userId, LocalDateTime.now()).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public StaffSchedule requireCurrentShiftForStaff(Collection<String> allowedPositions, String friendlyFeatureName) {
        if (!SecurityUtils.hasRole("STAFF") || SecurityUtils.isAdmin()) {
            return null;
        }

        StaffSchedule schedule = findCurrentScheduleForCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Bạn chưa có ca làm việc đang hoạt động."));

        String currentPosition = normalize(schedule.getWorkPosition());
        boolean allowed = allowedPositions.stream()
                .map(this::normalize)
                .anyMatch(currentPosition::equals);
        if (!allowed) {
            throw new AccessDeniedException("Ca hiện tại không được phân công cho " + friendlyFeatureName + ".");
        }
        return schedule;
    }

    @Transactional(readOnly = true)
    public StaffSchedule requireCurrentStaffSchedule() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Bạn cần đăng nhập để thực hiện thao tác này.");
        }
        return findCurrentScheduleForCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Bạn chưa có ca làm việc đang hoạt động."));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
