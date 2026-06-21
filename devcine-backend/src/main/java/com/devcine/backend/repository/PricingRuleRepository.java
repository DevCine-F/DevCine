package com.devcine.backend.repository;

import com.devcine.backend.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Integer> {
    List<PricingRule> findAllByOrderByStartDateDesc();
    List<PricingRule> findByRuleTypeAndActiveTrue(String ruleType);
    List<PricingRule> findByRuleType(String ruleType);
}
