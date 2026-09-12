package com.odyssey.api.need.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransferCriteriaRepository
    extends JpaRepository<TransferCriteria, Long> {

      Optional<TransferCriteria> findByNeedId(Long needId);
}