package com.devcine.backend.repository;

import com.devcine.backend.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, Integer> {
    List<UserPermissionOverride> findByUserId(Integer userId);

    void deleteByUserId(Integer userId);
}
