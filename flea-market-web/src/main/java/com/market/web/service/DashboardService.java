package com.market.web.service;

import com.market.web.dto.DashboardStatisticsDTO;

public interface DashboardService {
    DashboardStatisticsDTO getStatistics();
    int getUserCount();
    int getItemCount();
    double getSalesAmount();
}