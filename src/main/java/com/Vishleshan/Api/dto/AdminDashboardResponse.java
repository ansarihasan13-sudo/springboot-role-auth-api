
package com.Vishleshan.Api.dto;

public record AdminDashboardResponse(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees
) {
}
