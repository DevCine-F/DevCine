package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.repository.MovieFormatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formats")
@RequiredArgsConstructor
public class MovieFormatController {

    private final MovieFormatRepository movieFormatRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieFormat>>> getAllFormats() {
        return ResponseEntity.ok(ApiResponse.ok(movieFormatRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))));
    }
}
