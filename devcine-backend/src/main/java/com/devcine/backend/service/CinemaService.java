package com.devcine.backend.service;

import com.devcine.backend.dto.request.CinemaRequest;
import com.devcine.backend.dto.response.CinemaResponse;

import java.util.List;

public interface CinemaService {
    List<CinemaResponse> getAllCinemas();
    /** Public endpoint - chỉ trả rạp ACTIVE, không áp dụng cinema scoping cho STAFF/MANAGER */
    List<CinemaResponse> getAllActiveCinemas();
    List<CinemaResponse> getAllCinemas(boolean all);
    CinemaResponse getCinemaById(Integer id);
    CinemaResponse createCinema(CinemaRequest request);
    CinemaResponse updateCinema(Integer id, CinemaRequest request);
    CinemaResponse closeCinema(Integer id);
    CinemaResponse reopenCinema(Integer id);
    void deleteCinema(Integer id);
}
