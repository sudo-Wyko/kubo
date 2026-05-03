package com.teamroy.model.dao;

import com.teamroy.model.entity.MaintenanceRequest;
import java.util.List;

public interface MaintenanceRequestDao extends GenericDao<MaintenanceRequest> {
    List<MaintenanceRequest> GetByStatus(String status);

    List<MaintenanceRequest> GetByTenantID(int tenantId);

    List<MaintenanceRequest> GetByRoomID(int roomId);

    boolean UpdateStatus(int requestId, String status);
}