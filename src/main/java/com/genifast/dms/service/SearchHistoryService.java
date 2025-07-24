package com.genifast.dms.service;

import java.util.List;

import com.genifast.dms.dto.request.SaveSearchHistoryRequest;

public interface SearchHistoryService {
    List<String> getSearchKeywords(String keyword);

    void saveSearchHistory(SaveSearchHistoryRequest saveDto);
}