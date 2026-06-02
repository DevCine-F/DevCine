package com.devcine.backend.controller;

import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fnbs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FnbController {

    private final FnbItemRepository fnbItemRepository;

    @GetMapping
    public ResponseEntity<List<FnbItem>> getAllFnbs() {
        return ResponseEntity.ok(fnbItemRepository.findAll());
    }
}
