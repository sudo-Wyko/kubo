package com.teamroy.model.dao;

import com.teamroy.model.entity.Room;
import java.util.*;
import java.sql.*;

public class RoomDaoImpl implements RoomDao {
    private Connection conn;

    public RoomDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Room room) {
        String sql = "INSERT INTO ROOM (room_number, floor, room_type, capacity, current_occupancy, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.GetRoomNumber());
            ps.setInt(2, room.GetFloor());
            ps.setString(3, room.GetRoomType());
            ps.setInt(4, room.GetCapacity());
            ps.setInt(5, room.GetCurrentOccupancy());
            ps.setDouble(6, room.GetPrice());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    room.SetRoomID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Room GetByID(int roomId) {
        String sql = "SELECT * FROM ROOM WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToRoom(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Room> GetAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM ROOM";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                rooms.add(ResultSetToRoom(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public void Update(Room room) {
        String sql = "UPDATE ROOM SET room_number=?, floor=?, room_type=?, capacity=?, current_occupancy=?, price=? WHERE room_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.GetRoomNumber());
            ps.setInt(2, room.GetFloor());
            ps.setString(3, room.GetRoomType());
            ps.setInt(4, room.GetCapacity());
            ps.setInt(5, room.GetCurrentOccupancy());
            ps.setDouble(6, room.GetPrice());
            ps.setInt(7, room.GetRoomID());
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
        String sql = "SELECT * FROM ROOM WHERE room_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToRoom(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Room GetByFloor(int floor) {
        String sql = "SELECT * FROM ROOM WHERE floor = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, floor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToRoom(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Room> GetAvailableRooms() {
        List<Room> rooms = new ArrayList<>();
        // Rooms where there is still space (current < capacity)
        String sql = "SELECT * FROM ROOM WHERE current_occupancy < capacity";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                rooms.add(ResultSetToRoom(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public List<Room> GetByType(String roomType) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM ROOM WHERE room_type = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rooms.add(ResultSetToRoom(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    @Override
    public boolean IncrementOccupancy(int roomId) {
        // Atomic increment that respects capacity
        String sql = "UPDATE ROOM SET current_occupancy = current_occupancy + 1 " +
                "WHERE room_id = ? AND current_occupancy < capacity";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            // Returns true if a row was updated (meaning there was space)
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean DecrementOccupancy(int roomId) {
        // Atomic decrement that ensures occupancy never goes below zero
        String sql = "UPDATE ROOM SET current_occupancy = current_occupancy - 1 " +
                "WHERE room_id = ? AND current_occupancy > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);

            // Returns true if a row was updated (meaning there was someone to remove)
            // Returns false if the room was already empty (0 rows affected)
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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

    private Room ResultSetToRoom(ResultSet rs) throws SQLException {
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
