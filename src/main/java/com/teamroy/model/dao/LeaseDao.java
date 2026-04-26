package com.teamroy.model.dao;

import com.teamroy.model.entity.Lease;
import java.time.LocalDate;
import java.util.List;

public interface LeaseDao extends GenericDao<Lease> {
    List<Lease> GetByTenantID(int tenantId);

    Lease GetActiveLeaseByRoom(int roomId);

    List<Lease> GetByStatus(String status);

    List<Lease> GetExpiringSoon(LocalDate endDateThreshold);

    boolean UpdateStatus(int leaseId, String status);

    boolean IsRoomAvailable(int roomId, LocalDate start, LocalDate end);
}
