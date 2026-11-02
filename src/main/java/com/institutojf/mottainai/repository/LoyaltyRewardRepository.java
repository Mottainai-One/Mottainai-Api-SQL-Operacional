package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.LoyaltyReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyRewardRepository extends JpaRepository<LoyaltyReward, Integer> {

    List<LoyaltyReward> findByActiveTrueOrderByName();

}
