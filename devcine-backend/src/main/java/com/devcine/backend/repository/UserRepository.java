package com.devcine.backend.repository;

import com.devcine.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE LOWER(r.name) = LOWER(:roleName)")
    List<User> findAllByRoleName(@Param("roleName") String roleName);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Integer id);

    Optional<User> findByPhone(String phone);

    // Đăng nhập bằng số điện thoại HOẶC email (giữ cả username để tương thích admin/demo cũ).
    // Trả List để tránh lỗi NonUnique nếu dữ liệu trùng; service lấy bản ghi đầu.
    @Query("SELECT u FROM User u JOIN FETCH u.role " +
           "WHERE u.phone = :id OR LOWER(u.email) = LOWER(:id) OR u.username = :id")
    List<User> findByLoginIdentifier(@Param("id") String id);

    // Tìm tài khoản theo email (không phân biệt hoa thường) — dùng cho quên mật khẩu.
    // Trả List để tránh NonUnique nếu dữ liệu lịch sử trùng; service lấy bản ghi đầu.
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    List<User> findByEmailIgnoreCase(@Param("email") String email);

}
