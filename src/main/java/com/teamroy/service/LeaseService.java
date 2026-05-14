package com.teamroy.service;
import com.teamroy.model.dao.LeaseDao;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.Lease;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
public class LeaseService {
    private static final Logger LOGGER = Logger.getLogger(LeaseService.class.getName());
    private final LeaseDao leaseDao;
    public LeaseService(Connection conn) {
        this.leaseDao = new LeaseDaoImpl(conn);
    }
    public Lease createLease(int tenantId, int roomId, LocalDate start, LocalDate end, double rent, int roomCapacity) {
        boolean available = leaseDao.IsRoomAvailableWithCapacity(roomId, start, end, null, roomCapacity);
        if (!available) {
            return null;
        }
        Lease lease = new Lease(tenantId, roomId, start, end, rent, "ACTIVE");
        leaseDao.Create(lease);
        if (lease.GetLeaseID() <= 0) {
            LOGGER.severe(() -> "Lease INSERT did not return a lease_id (check DB error log, LEASE.charged_rent_periods column, and connection autoCommit). tenantId="
                    + tenantId + " roomId=" + roomId);
            return null;
        }
        return lease;
    }
}
