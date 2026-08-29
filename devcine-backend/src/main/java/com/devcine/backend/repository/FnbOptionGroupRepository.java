package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

@Repository
public interface FnbOptionGroupRepository extends JpaRepository<FnbOptionGroup, Integer> {
    @EntityGraph(attributePaths = {"items"})
    List<FnbOptionGroup> findAll();

    Optional<FnbOptionGroup> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
