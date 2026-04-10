package com.teamroy.model.entity;

public class Room {
    private int roomId;
    private String roomNumber;
    private String roomType;
    private int capacity;
    private int currentOccupancy;

    public Room(int roomId, String roomNumber, String roomType, int capacity, int currentOccupancy) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
    }

    // -- Getters --
    public int GetRoomID() {
        return roomId;
    }

    public String GetRoomNumber() {
        return roomNumber;
    }

    public String GetRoomType() {
        return roomType;
    }

    public int GetCapacity() {
        return capacity;
    }

    public int GetCurrentOccupancy() {
        return currentOccupancy;
    }

    // -- Setters --
    public void SetRoomID(int roomId) {
        this.roomId = roomId;
    }

    public void SetRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void SetRoomtType(String roomType) {
        this.roomType = roomType;
    }

    public void SetCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void SetCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }
}
