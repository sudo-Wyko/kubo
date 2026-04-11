package com.teamroy.model.dao;

import com.teamroy.model.entity.UserAccount;

public interface UserAccountDao extends GenericDao<UserAccount> {
    UserAccount GetByUsername(String username);

    boolean UpdatePassword(int userId, String newPasswordHash);
}
