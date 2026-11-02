package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.SalesSummaryResponse;
import com.institutojf.mottainai.repository.SalesSummaryRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesSummaryServiceTest {

    @Mock
    private SalesSummaryRepository salesSummaryRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SalesSummaryService salesSummaryService;

    @Test
    @DisplayName("Should return the current store's completed sales total for this month")
    void shouldReturnCurrentMonthSummaryForAuthenticatedStore() {
        YearMonth month = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        when(inventoryAccess.store(authentication, 1)).thenReturn(1);
        when(salesSummaryRepository.summarizeCurrentMonth(eq(1), any(), any()))
                .thenReturn(new SalesSummaryRepository.SummaryTotals(3, new BigDecimal("125.50")));

        SalesSummaryResponse response = salesSummaryService.currentMonth(authentication, 1);

        assertEquals(1, response.storeId());
        assertEquals(month.toString(), response.month());
        assertEquals(3, response.transactionCount());
        assertEquals(0, new BigDecimal("125.50").compareTo(response.totalAmount()));
        verify(inventoryAccess).store(authentication, 1);
        verify(salesSummaryRepository).summarizeCurrentMonth(1, start, end);
    }

    @Test
    @DisplayName("Should return zero totals when the store has no completed sales")
    void shouldReturnZeroTotalsForEmptyMonth() {
        when(inventoryAccess.store(authentication, 1)).thenReturn(1);
        when(salesSummaryRepository.summarizeCurrentMonth(eq(1), any(), any()))
                .thenReturn(new SalesSummaryRepository.SummaryTotals(0, BigDecimal.ZERO));

        SalesSummaryResponse response = salesSummaryService.currentMonth(authentication, 1);

        assertEquals(0L, response.transactionCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalAmount()));
    }
}
