package com.ecomart.dto.response;

import com.ecomart.entity.enums.ReportGroupBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerGrowthResponse {

    private Long totalNewCustomers;
    private ReportGroupBy groupBy;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<CustomerGrowthData> items;
}
