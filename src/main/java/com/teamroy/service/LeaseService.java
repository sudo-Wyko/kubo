package com.teamroy.service;
import com.teamroy.model.dao.LeaseDao;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.Lease;
import java.sql.Connection;
import java.time.LocalDate;
public class LeaseService {
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
        return lease;
    }
}
