package com.genifast.dms.repository;

import com.genifast.dms.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    
    boolean existsByUserIdAndProjectIdAndStatus(Long userId, Long projectId, Integer status);
    
    Optional<ProjectMember> findByUserIdAndProjectId(Long userId, Long projectId);
    
    List<ProjectMember> findByProjectIdAndStatus(Long projectId, Integer status);
    
    List<ProjectMember> findByUserIdAndStatus(Long userId, Integer status);
    
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.project.id = :projectId AND pm.status = 1")
    Optional<ProjectMember> findActiveByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);
}