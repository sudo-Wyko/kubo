package com.teamroy.model.dao;

import com.teamroy.model.entity.Room;
import java.util.List;

public interface RoomDao extends GenericDao<Room> {
    Room GetByRoomNumber(String roomNumber);

    List<Room> GetAvailableRooms();

    List<Room> GetByType(String roomType);

    boolean IncrementOccupancy(int roomId);

    boolean DecrementOccupancy(int roomId);

    void UpdatePrice(int roomId, double amount);
}
