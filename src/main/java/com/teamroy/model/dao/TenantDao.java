package com.teamroy.model.dao;

import com.teamroy.model.entity.Tenant;
import java.util.List;

public interface TenantDao extends GenericDao<Tenant> {
    Tenant GetByUserID(int userId);

    List<Tenant> GetByName(String name);

    List<Tenant> GetAllActive();

    void Restore(int tenantId);

    List<Tenant> GetTenantsWithBalance();

    boolean UpdateBalance(int tenantId, double amount);

}
