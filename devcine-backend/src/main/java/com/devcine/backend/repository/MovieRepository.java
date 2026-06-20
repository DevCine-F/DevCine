package com.devcine.backend.repository;

import com.devcine.backend.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer> {
    
    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres ORDER BY m.id DESC")
    List<Movie> findAllWithGenres();

    @Query("SELECT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.id = :id")
    Optional<Movie> findByIdWithGenres(@Param("id") Integer id);

    Optional<Movie> findBySlug(String slug);

    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.titleVietnamese) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM m.genres g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.id DESC")
    List<Movie> searchMovies(@Param("keyword") String keyword);

    /** Đếm số phim đang gắn 1 thể loại — chặn xoá thể loại đang được sử dụng. */
    long countByGenres_Id(Integer categoryId);
}
