package com.teamroy.model.dao;

import com.teamroy.model.entity.Lease;
import java.time.LocalDate;
import java.util.List;

public interface LeaseDao extends GenericDao<Lease> {
    List<Lease> GetByTenantId(int tenantId);

    Lease GetActiveLeaseByRoom(int roomId);

    List<Lease> GetByStatus(String status);

    List<Lease> GetExpiringSoon(LocalDate endDateThreshold);

    boolean UpdateStatus(int leaseId, String status);

    /**
     * @param excludeLeaseId optional lease row to exclude (e.g. when editing dates)
     */
    boolean IsRoomAvailable(int roomId, LocalDate start, LocalDate end, Integer excludeLeaseId);

    default boolean IsRoomAvailable(int roomId, LocalDate start, LocalDate end) {
        return IsRoomAvailable(roomId, start, end, null);
    }
}
