package com.realestate.dto;

public record AdminStatisticsResponse(
        long totalUsers,
        long totalAgents,
        long totalCustomers,
        long totalProperties,
        long availableProperties,
        long soldOrRentedProperties,
        long totalInquiries,
        long pendingInquiries,
        long totalVisits,
        long pendingVisitRequests
) {
}
