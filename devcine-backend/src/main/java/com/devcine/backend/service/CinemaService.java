package com.devcine.backend.service;

import com.devcine.backend.dto.request.CinemaRequest;
import com.devcine.backend.dto.response.CinemaResponse;

import java.util.List;

public interface CinemaService {
    List<CinemaResponse> getAllCinemas();
    CinemaResponse getCinemaById(Integer id);
    CinemaResponse createCinema(CinemaRequest request);
    CinemaResponse updateCinema(Integer id, CinemaRequest request);
    CinemaResponse closeCinema(Integer id);
    CinemaResponse reopenCinema(Integer id);
    void deleteCinema(Integer id);
}
