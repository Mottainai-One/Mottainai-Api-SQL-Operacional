package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.SuggestedAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuggestedActionRepository extends JpaRepository<SuggestedAction, Integer> {

    List<SuggestedAction> findByAlert_Store_IdOrderByGeneratedAtDesc(Integer storeId);

    List<SuggestedAction> findByAlert_IdOrderByGeneratedAtDesc(Integer alertId);

    Optional<SuggestedAction> findBySourceRecommendationUuid(UUID sourceRecommendationUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from SuggestedAction action join fetch action.alert alert "
            + "join fetch alert.store where action.id = :id")
    Optional<SuggestedAction> findByIdForUpdate(@Param("id") Integer id);

}
