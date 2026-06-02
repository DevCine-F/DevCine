package com.devcine.backend.repository;

import com.devcine.backend.entity.BookingFnb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingFnbRepository extends JpaRepository<BookingFnb, Integer> {
}
