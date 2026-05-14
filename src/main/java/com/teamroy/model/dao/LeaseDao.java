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
    boolean IsRoomAvailable(int roomId, LocalDate start, LocalDate end, Integer excludeLeaseId);
    default boolean IsRoomAvailable(int roomId, LocalDate start, LocalDate end) {
        return IsRoomAvailable(roomId, start, end, null);
    }
    boolean IsRoomAvailableWithCapacity(int roomId, LocalDate start, LocalDate end, Integer excludeLeaseId, int capacity);
    boolean UpdateChargedRentPeriods(int leaseId, int chargedRentPeriods);
    /**
     * Atomically sets {@code charged_rent_periods} from {@code expectedCurrent} to {@code newValue} for one row.
     * Use for idempotent rent accrual (prevents double-billing when combined with balance update in one transaction).
     */
    boolean tryAdvanceChargedRentPeriods(int leaseId, int expectedCurrent, int newValue);
}
