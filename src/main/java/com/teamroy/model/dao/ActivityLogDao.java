package com.teamroy.model.dao;
import com.teamroy.model.entity.ActivityLog;
import java.util.List;
import java.time.LocalDateTime;
public interface ActivityLogDao extends GenericDao<ActivityLog> {
    List<ActivityLog> GetByTenantID(int tenantId);
    List<ActivityLog> GetRecentByTenantID(int tenantId, int limit);
    List<ActivityLog> GetByDateRange(LocalDateTime start, LocalDateTime end);
    void DeleteByTenantID(int tenantId);
}
