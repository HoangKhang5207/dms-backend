package com.genifast.dms.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.SaveSearchHistoryRequest;
import com.genifast.dms.entity.SearchHistory;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.SearchHistoryRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.SearchHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<String> getSearchKeywords(String keyword) {
        User currentUser = userRepository.findByEmail(JwtUtils.getCurrentUserLogin().orElse("")).orElse(null);
        if (currentUser == null) {
            // Trường hợp user không tồn tại nhưng token hợp lệ (hiếm)
            // Chỉ tìm kiếm theo các từ khóa trending (user_id IS NULL)
            return searchHistoryRepository.findPersonalizedKeywords(null, keyword);
        }
        return searchHistoryRepository.findPersonalizedKeywords(currentUser.getId(), keyword);
    }

    @Override
    @Transactional
    public void saveSearchHistory(SaveSearchHistoryRequest saveDto) {
        User currentUser = userRepository.findByEmail(JwtUtils.getCurrentUserLogin().orElse("")).orElse(null);

        // Loại 2 là trending search, không gắn với user cụ thể (user = null)
        User targetUser = (saveDto.getType() == 2) ? null : currentUser;

        // Tìm kiếm xem từ khóa đã tồn tại cho user này (hoặc cho trending) chưa
        Optional<SearchHistory> existingHistory = searchHistoryRepository
                .findByUserAndKeywordsIgnoreCase(targetUser, saveDto.getKeyword());

        if (existingHistory.isPresent()) {
            // Nếu đã tồn tại, chỉ cần save lại để @UpdateTimestamp tự động cập nhật
            SearchHistory historyToUpdate = existingHistory.get();
            searchHistoryRepository.save(historyToUpdate);
        } else {
            // Nếu chưa tồn tại, tạo mới
            SearchHistory newHistory = SearchHistory.builder()
                    .keywords(saveDto.getKeyword())
                    .type(saveDto.getType())
                    .user(targetUser)
                    .build();
            searchHistoryRepository.save(newHistory);
        }
    }
}
