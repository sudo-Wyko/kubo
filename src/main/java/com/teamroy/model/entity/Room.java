package com.teamroy.model.entity;

public class Room {
    private int roomId;
    private String roomNumber;
    private int floor;
    private String roomType;
    private int capacity;
    private int currentOccupancy;
    private double price;

    public Room() {
    }

    public Room(int roomId, String roomNumber, int floor, String roomType, int capacity, int currentOccupancy,
            double price) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.roomType = roomType;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.price = price;
    }

    // -- Getters --
    public int GetRoomID() {
        return roomId;
    }

    public String GetRoomNumber() {
        return roomNumber;
    }

    public int GetFloor() {
        return floor;
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

    public double GetPrice() {
        return price;
    }

    // -- Setters --
    public void SetRoomID(int roomId) {
        this.roomId = roomId;
    }

    public void SetRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void SetFloor(int floor) {
        this.floor = floor;
    }

    public void SetRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void SetCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void SetCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }

    public void SetPrice(double price) {
        this.price = price;
    }
}
