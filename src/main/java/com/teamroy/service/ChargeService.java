package com.teamroy.service;

import com.teamroy.model.dao.ActivityLogDao;
import com.teamroy.model.dao.ActivityLogDaoImpl;
import com.teamroy.model.dao.ChargeDao;
import com.teamroy.model.dao.ChargeDaoImpl;
import com.teamroy.model.dao.DaoException;
import com.teamroy.model.dao.LeaseDao;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.ActivityLog;
import com.teamroy.model.entity.Charge;
import com.teamroy.model.entity.Lease;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChargeService {
    private static final Logger LOGGER = Logger.getLogger(ChargeService.class.getName());

    private final Connection conn;
    private final ChargeDao chargeDao;
    private final LeaseDao leaseDao;
    private final ActivityLogDao activityLogDao;

    public ChargeService(Connection conn) {
        this.conn = conn;
        this.chargeDao = new ChargeDaoImpl(conn);
        this.leaseDao = new LeaseDaoImpl(conn);
        this.activityLogDao = new ActivityLogDaoImpl(conn);
    }

    public int issueChargeToLease(int leaseId, String chargeType, double amount, LocalDate dueDate, String description) {
        Lease lease = leaseDao.GetByID(leaseId);
        if (lease == null) {
            throw new DaoException("Lease not found.");
        }
        return issueCharge(lease, chargeType, amount, dueDate, description);
    }

    public int issueChargeToTenant(int tenantId, String chargeType, double amount, LocalDate dueDate, String description) {
        Lease lease = leaseDao.GetActiveLeaseByTenant(tenantId);
        if (lease == null) {
            throw new DaoException("Tenant has no active lease. Create a lease before issuing a charge.");
        }
        return issueCharge(lease, chargeType, amount, dueDate, description);
    }

    private int issueCharge(Lease lease, String chargeType, double amount, LocalDate dueDate, String description) {
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            Charge charge = new Charge(lease.GetLeaseID(), chargeType, amount, dueDate, description);
            int chargeId = chargeDao.CreateCharge(charge);

            String logMessage = String.format(Locale.ENGLISH,
                    "Added %s charge of ₱%,.2f", chargeType.trim().toUpperCase(), amount);
            ActivityLog activityLog = new ActivityLog(lease.GetTenantID(), logMessage, null);
            activityLogDao.Create(activityLog);

            conn.commit();
            LOGGER.info(() -> "Issued charge #" + chargeId + " on lease #" + lease.GetLeaseID());
            return chargeId;
        } catch (DaoException ex) {
            rollbackQuietly();
            LOGGER.log(Level.WARNING, "Charge issue failed: " + ex.getMessage());
            throw ex;
        } catch (SQLException ex) {
            rollbackQuietly();
            LOGGER.log(Level.SEVERE, "Charge issue transaction failed", ex);
            throw new DaoException("Database error while issuing charge: " + ex.getMessage(), ex);
        } finally {
            restoreAutoCommit(previousAutoCommit);
        }
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Rollback failed after charge error", ex);
        }
    }

    private void restoreAutoCommit(boolean previousAutoCommit) {
        try {
            conn.setAutoCommit(previousAutoCommit);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to restore auto-commit", ex);
        }
    }
}
