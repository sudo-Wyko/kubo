package com.teamroy.model.dao;

import com.teamroy.model.entity.UserAccount;
import java.util.*;
import java.sql.*;

public class UserAccountDaoImpl implements UserAccountDao {
    private Connection conn;

    public UserAccountDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(UserAccount entity) {
        String sql = "INSERT INTO USER_ACCOUNT (username, password_hash, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.GetUsername());
            ps.setString(2, entity.GetPassword());
            ps.setString(3, entity.GetRole());
            ps.executeUpdate();

            // Get the auto-incremented ID and set it back to the entity
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.SetUserID(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public UserAccount GetByID(int userId) {
        String sql = "SELECT * FROM USER_ACCOUNT WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<UserAccount> GetAll() {
        List<UserAccount> users = new ArrayList<>();
        String sql = "SELECT * FROM USER_ACCOUNT";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(ResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public UserAccount GetByUsername(String username) {
        String sql = "SELECT * FROM USER_ACCOUNT WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean UpdatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE USER_ACCOUNT SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void Update(UserAccount user) {
        String sql = "UPDATE USER_ACCOUNT SET username = ?, role = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.GetUsername());
            ps.setString(2, user.GetRole());
            ps.setInt(3, user.GetUserID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int userId) {
        String sql = "DELETE FROM USER_ACCOUNT WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -- Helper Method --
    private UserAccount ResultSetToUser(ResultSet rs) throws SQLException {
        UserAccount user = new UserAccount();
        user.SetUserID(rs.getInt("user_id"));
        user.SetUsername(rs.getString("username"));
        user.SetPassword(rs.getString("password_hash"));
        user.SetRole(rs.getString("role"));
        return user;
    }

}
