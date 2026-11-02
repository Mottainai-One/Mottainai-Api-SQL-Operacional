package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.SalesSummaryResponse;
import com.institutojf.mottainai.repository.SalesSummaryRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SalesSummaryService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final SalesSummaryRepository salesSummaryRepository;
    private final InventoryAccess inventoryAccess;

    @Transactional(readOnly = true)
    public SalesSummaryResponse currentMonth(Authentication authentication, Integer requestedStoreId) {
        Integer storeId = inventoryAccess.store(authentication, requestedStoreId);
        YearMonth month = YearMonth.now(BUSINESS_ZONE);
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        SalesSummaryRepository.SummaryTotals totals = salesSummaryRepository
                .summarizeCurrentMonth(storeId, from, to);
        return new SalesSummaryResponse(storeId, month.toString(), totals.transactionCount(), totals.totalAmount());
    }
}
