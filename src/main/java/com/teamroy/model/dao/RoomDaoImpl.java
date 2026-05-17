package com.teamroy.model.dao;

import com.teamroy.model.entity.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDaoImpl implements RoomDao {
    private static final String ROOM_SELECT = "SELECT r.room_id, r.room_number, r.room_type, r.capacity, r.price, "
            + "(SELECT COUNT(*) FROM LEASE l WHERE l.room_id = r.room_id AND l.status = 'ACTIVE') AS current_occupancy "
            + "FROM ROOM r";

    private final Connection conn;

    public RoomDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Room room) {
        String sql = "INSERT INTO ROOM (room_number, room_type, capacity, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.GetRoomNumber());
            ps.setString(2, room.GetRoomType());
            ps.setInt(3, room.GetCapacity());
            ps.setDouble(4, room.GetPrice());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    room.SetRoomID(rs.getInt(1));
                }
            }
            room.SetCurrentOccupancy(0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Room GetByID(int roomId) {
        String sql = ROOM_SELECT + " WHERE r.room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToRoom(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Room> GetAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = ROOM_SELECT + " ORDER BY r.room_number";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(resultSetToRoom(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public void Update(Room room) {
        String sql = "UPDATE ROOM SET room_number=?, room_type=?, capacity=?, price=? WHERE room_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.GetRoomNumber());
            ps.setString(2, room.GetRoomType());
            ps.setInt(3, room.GetCapacity());
            ps.setDouble(4, room.GetPrice());
            ps.setInt(5, room.GetRoomID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int roomId) {
        String sql = "DELETE FROM ROOM WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Room GetByRoomNumber(String roomNumber) {
        String sql = ROOM_SELECT + " WHERE r.room_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToRoom(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Room> GetAvailableRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = ROOM_SELECT
                + " WHERE (SELECT COUNT(*) FROM LEASE l WHERE l.room_id = r.room_id AND l.status = 'ACTIVE') < r.capacity";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(resultSetToRoom(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public List<Room> GetByType(String roomType) {
        List<Room> rooms = new ArrayList<>();
        String sql = ROOM_SELECT + " WHERE r.room_type = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(resultSetToRoom(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public void UpdatePrice(int roomId, double price) {
        String sql = "UPDATE ROOM SET price = ? WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, price);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int GetActiveOccupancy(int roomId) {
        String sql = "SELECT COUNT(*) FROM LEASE WHERE room_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Room resultSetToRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.SetRoomID(rs.getInt("room_id"));
        room.SetRoomNumber(rs.getString("room_number"));
        room.SetRoomType(rs.getString("room_type"));
        room.SetCapacity(rs.getInt("capacity"));
        room.SetCurrentOccupancy(rs.getInt("current_occupancy"));
        room.SetPrice(rs.getDouble("price"));
        return room;
    }
}
