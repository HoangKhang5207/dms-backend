package com.genifast.dms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genifast.dms.dto.request.SaveSearchHistoryRequest;
import com.genifast.dms.dto.request.SearchKeywordRequest;
import com.genifast.dms.service.SearchHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping("/keywords")
    public ResponseEntity<List<String>> getSearchKeywords(
            @RequestBody SearchKeywordRequest requestDto) {

        List<String> keywords = searchHistoryService.getSearchKeywords(requestDto.getKeyword());
        return ResponseEntity.ok(keywords);
    }

    @PostMapping
    public ResponseEntity<Void> saveSearchHistory(
            @Valid @RequestBody SaveSearchHistoryRequest saveDto) {

        searchHistoryService.saveSearchHistory(saveDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
