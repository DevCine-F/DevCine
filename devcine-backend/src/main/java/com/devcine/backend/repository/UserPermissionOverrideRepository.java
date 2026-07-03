package com.devcine.backend.repository;

import com.devcine.backend.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, Integer> {
    List<UserPermissionOverride> findByUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM UserPermissionOverride u WHERE u.user.id = :userId")
    void deleteByUserId(Integer userId);
}
