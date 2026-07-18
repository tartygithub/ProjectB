package com.finance.app.repository;

import com.finance.app.model.FinancialMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialMessageRepository extends JpaRepository<FinancialMessage, Long> {
}
