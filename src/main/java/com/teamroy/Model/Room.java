package com.teamroy.Model;

public class Room {
    private int roomId;
    private String roomNumber;
    private String roomType;
    private int capacity;
    private int currentOccupancy;
    private String status;
    private String maintenance;

    public Room(int roomId, String roomNumber, String roomType, int capacity, int currentOccupancy, String status, String maintenance) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.status = status;
        this.maintenance = maintenance;
    }

    public int getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public int getCapacity() { return capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public String getStatus() { return status; }
    public String getMaintenance() { return maintenance; }
}