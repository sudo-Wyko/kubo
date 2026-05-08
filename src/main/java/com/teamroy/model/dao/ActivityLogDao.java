package com.teamroy.model.dao;

import com.teamroy.model.entity.ActivityLog;
import java.util.List;
import java.time.LocalDateTime;

public interface ActivityLogDao extends GenericDao<ActivityLog> {

    // Fetches all activity for a specific tenant
    List<ActivityLog> GetByTenantID(int tenantId);

    // Useful for the dashboard so you don't load 500 rows at once
    List<ActivityLog> GetRecentByTenantID(int tenantId, int limit);

    // Useful for filtering history by date
    List<ActivityLog> GetByDateRange(LocalDateTime start, LocalDateTime end);

    // Optional: Delete all logs for a tenant (though SQL ON DELETE CASCADE handles
    // this too)
    void DeleteByTenantID(int tenantId);
}