package com.teamroy.model.dao;

import com.teamroy.model.entity.Charge;

import java.util.List;
import java.time.LocalDate;

public interface ChargeDao extends GenericDao<Charge> {
    List<Charge> GetByLeaseID(int leaseId);

    List<Charge> GetByTenantID(int tenantId);

    int CreateCharge(Charge charge);

    LocalDate GetNextRentDueDate(int tenantId);
}
