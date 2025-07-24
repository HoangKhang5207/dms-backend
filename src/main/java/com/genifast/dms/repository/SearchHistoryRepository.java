package com.genifast.dms.repository;

import com.genifast.dms.entity.SearchHistory;
import com.genifast.dms.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface cho SearchHistory entity.
 * Chuyển đổi từ ISearchHistoryRepository của Golang.
 */
@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

        /**
         * Tìm một lịch sử tìm kiếm đã tồn tại của một user với từ khóa chính xác.
         * Tương ứng GetExistedHistoryOfUser.
         *
         * @param userId   ID của người dùng
         * @param keywords Từ khóa tìm kiếm (không phân biệt hoa thường)
         * @return Optional chứa SearchHistory nếu tồn tại
         */
        Optional<SearchHistory> findByUserAndKeywordsIgnoreCase(User user, String keywords);

        /**
         * Lấy danh sách các từ khóa gợi ý dựa trên lịch sử của user và các từ khóa
         * chung.
         * Tương ứng GetAllSearchHistoryPersonalize.
         *
         * @param userId       ID của người dùng (có thể là null)
         * @param keywordInput Từ khóa người dùng đang gõ
         * @return Danh sách các từ khóa gợi ý
         */
        @Query("SELECT s.keywords FROM SearchHistory s " +
                        "WHERE (s.user.id = :userId OR s.user.id IS NULL) " +
                        "AND LOWER(s.keywords) LIKE LOWER(CONCAT('%', :keywordInput, '%')) " +
                        "ORDER BY s.updatedAt DESC")
        List<String> findPersonalizedKeywords(
                        @Param("userId") Long userId,
                        @Param("keywordInput") String keywordInput);
}