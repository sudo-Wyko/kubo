package com.teamroy.service;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Posts monthly rent to {@code TENANT.total_balance} after each billing due date
 * (lease start + N calendar months) has <strong>passed</strong> (charge begins the day after the due date),
 * using {@code LEASE.charged_rent_periods} with optimistic locking so each period is billed at most once
 * even when this method is invoked repeatedly (e.g. dashboard / payments reload).
 */
public final class RentAccrualService {
    private static final Logger LOGGER = Logger.getLogger(RentAccrualService.class.getName());
    private RentAccrualService() {
    }
    /**
     * Next calendar due date for rent (same anchor as accrual): first due is {@code start + 1 month},
     * then advances one month each time a period is charged.
     */
    public static LocalDate nextDueDate(Lease lease) {
        if (lease == null || lease.GetStartDate() == null) {
            return null;
        }
        int charged = Math.max(0, lease.GetChargedRentPeriods());
        return lease.GetStartDate().plusMonths(charged + 1);
    }
    /**
     * Applies all due rent periods through today. Each period is applied in its own InnoDB transaction:
     * (1) optimistic lease row update {@code charged_rent_periods} from {@code n} to {@code n+1},
     * (2) tenant balance increment — then {@code COMMIT}. On any failure, {@code ROLLBACK} so the lease
     * counter and balance stay consistent.
     * <p>
     * This ordering fixes double-billing when the lease update used to fail after the balance had already
     * been increased (e.g. missing column, silent DAO failure).
     */
    public static void applyChargesThroughToday(Connection conn, int tenantId) {
        if (conn == null || tenantId <= 0) {
            return;
        }
        LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
        TenantDaoImpl tenantDao = new TenantDaoImpl(conn);
        List<Lease> leases = leaseDao.GetByTenantId(tenantId);
        Lease active = leases.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.GetStatus()))
                .max(Comparator.comparing(Lease::GetStartDate))
                .orElse(null);
        if (active == null) {
            return;
        }
        double rent = active.GetMonthlyRent();
        if (rent <= 0 || Double.isNaN(rent) || Double.isInfinite(rent)) {
            return;
        }
        LocalDate start = active.GetStartDate();
        LocalDate end = active.GetEndDate();
        LocalDate today = LocalDate.now();
        int periodsCharged = Math.max(0, active.GetChargedRentPeriods());
        int leaseId = active.GetLeaseID();
        boolean priorAutoCommit;
        try {
            priorAutoCommit = conn.getAutoCommit();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Rent accrual: could not read autoCommit for tenant " + tenantId, e);
            return;
        }
        while (true) {
            int next = periodsCharged + 1;
            LocalDate due = start.plusMonths(next);
            if (due.isAfter(end)) {
                break;
            }
            if (!today.isAfter(due)) {
                break;
            }
            final int fromPeriods = periodsCharged;
            final int toPeriods = next;
            final LocalDate dueThis = due;
            try {
                conn.setAutoCommit(false);
                if (!leaseDao.tryAdvanceChargedRentPeriods(leaseId, fromPeriods, toPeriods)) {
                    conn.rollback();
                    LOGGER.fine(() -> String.format(
                            "Rent accrual skip: tenant=%d lease=%d could not advance periods %d->%d (already advanced or DB issue)",
                            tenantId, leaseId, fromPeriods, toPeriods));
                    break;
                }
                if (!tenantDao.UpdateBalance(tenantId, rent)) {
                    conn.rollback();
                    LOGGER.warning(() -> String.format(
                            "Rent accrual aborted: tenant=%d lease=%d balance update failed after lease advance; rolled back",
                            tenantId, leaseId));
                    break;
                }
                conn.commit();
                LOGGER.info(() -> String.format(
                        "Rent accrual applied: tenant=%d lease=%d period=%d due=%s rent=%.2f charged_rent_periods=%d",
                        tenantId, leaseId, toPeriods, dueThis, rent, toPeriods));
                periodsCharged = toPeriods;
            } catch (SQLException e) {
                try {
                    if (!conn.getAutoCommit()) {
                        conn.rollback();
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rent accrual rollback failed tenant=" + tenantId, ex);
                }
                LOGGER.log(Level.SEVERE, "Rent accrual transaction failed tenant=" + tenantId + " lease=" + leaseId, e);
                break;
            } finally {
                try {
                    conn.setAutoCommit(priorAutoCommit);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Rent accrual: failed to restore autoCommit for tenant " + tenantId, e);
                }
            }
        }
    }
}
