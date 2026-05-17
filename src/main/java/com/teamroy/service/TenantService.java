package com.teamroy.service;

import com.teamroy.PasswordUtil;
import com.teamroy.model.dao.DaoException;
import com.teamroy.model.dao.TenantDao;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.dao.UserAccountDao;
import com.teamroy.model.dao.UserAccountDaoImpl;
import com.teamroy.model.entity.Tenant;
import com.teamroy.model.entity.UserAccount;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TenantService {
    private static final Logger LOGGER = Logger.getLogger(TenantService.class.getName());

    private final Connection conn;
    private final TenantDao tenantDao;
    private final UserAccountDao userDao;

    public TenantService(Connection conn) {
        this.conn = conn;
        this.tenantDao = new TenantDaoImpl(conn);
        this.userDao = new UserAccountDaoImpl(conn);
    }

    public int createTenant(String firstName, String lastName, String contact, String email,
                           String username, String password) {
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            Integer userId = null;
            if (username != null && !username.isBlank()) {
                if (password == null || password.isBlank()) {
                    throw new DaoException("Password is required when a username is provided.");
                }
                if (userDao.GetByUsername(username.trim()) != null) {
                    throw new DaoException("Username already exists.");
                }
                UserAccount account = new UserAccount(username.trim(), PasswordUtil.hash(password), "TENANT");
                userDao.Create(account);
                if (account.GetUserID() <= 0) {
                    throw new DaoException("User account was not created.");
                }
                userId = account.GetUserID();
            }

            Tenant tenant = new Tenant(firstName, lastName, contact, email);
            tenant.SetUserID(userId);
            tenantDao.Create(tenant);
            if (tenant.GetTenantID() <= 0) {
                throw new DaoException("Tenant record was not created.");
            }

            conn.commit();
            LOGGER.info(() -> "Created tenant #" + tenant.GetTenantID());
            return tenant.GetTenantID();
        } catch (DaoException ex) {
            rollbackQuietly();
            LOGGER.log(Level.WARNING, "Tenant creation failed: " + ex.getMessage());
            throw ex;
        } catch (SQLException ex) {
            rollbackQuietly();
            LOGGER.log(Level.SEVERE, "Tenant creation transaction failed", ex);
            throw new DaoException("Database error while creating tenant: " + ex.getMessage(), ex);
        } finally {
            restoreAutoCommit(previousAutoCommit);
        }
    }

    public void updateTenant(Tenant tenant) {
        tenantDao.Update(tenant);
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Rollback failed after tenant error", ex);
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
