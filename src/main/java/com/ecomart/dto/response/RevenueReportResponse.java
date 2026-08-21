package com.ecomart.dto.response;

import com.ecomart.entity.enums.ReportGroupBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    private BigDecimal totalRevenue;
    private Long totalCompletedOrders;
    private BigDecimal averageOrderValue;
    private ReportGroupBy groupBy;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<RevenuePeriodData> items;
}
