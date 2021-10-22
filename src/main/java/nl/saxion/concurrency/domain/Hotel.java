package nl.saxion.concurrency.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Hotel implements Serializable {

    //Generate a unique random id
    //because id is final we can make it public
    public final String id = UUID.randomUUID().toString();
    public final String name;

    //hashmap of roomID and Room
    HashMap<String, Room> rooms = new HashMap<>();
    int occupiedRooms = 0;

    public Hotel(String name, int amountOfRooms){
        this.name = name;
        for (int i = 0; i < amountOfRooms; i++) {
            Room room = new Room();
            rooms.put(room.roomID, room);
        }
    }

    public ArrayList<String> reserveRooms(int amount) throws ReservationException {
        ArrayList<String> reservedRoomIds = new ArrayList<>();

        List<Room> availableRooms = getAvailableRooms();
        if(availableRooms.size() < amount){
            throw new ReservationException("Trying to reserve " + amount + " rooms at hotel " +
                this.id + " while only " + availableRooms.size() + " rooms are available.");
        }
        for (int i = 0; i < amount; i++) {
            Room room = availableRooms.get(i);
            room.reserve();
            occupiedRooms++;
            reservedRoomIds.add(room.roomID);
        }
        return reservedRoomIds;
    }

    private List<Room> getAvailableRooms(){
        List<Room> availableRooms = rooms.values().stream().filter(x -> x.isAvailable()).collect(Collectors.toList());
        return availableRooms;
    }

    public void cancelReservation(String roomUUID) throws ReservationException {
        if(!rooms.containsKey(roomUUID)){
            throw new ReservationException("Hotel " + id + " does not contain a room with id " + roomUUID);
        }
        else{
            rooms.get(roomUUID).cancelReservation();
            occupiedRooms--;
        }
    }

    public void confirmReservation(String roomUUID) throws ReservationException {
        if(!rooms.containsKey(roomUUID)){
            throw new ReservationException("Hotel " + id + " does not contain a room with id " + roomUUID);
        }
        else{
            rooms.get(roomUUID).confirmReservation();
        }
    }

    @Override
    public String toString() {
        String returnString= "";

        returnString += "Hotel: " + name + " (ID: " + id + ")";
        returnString += "\n - Rooms available: " + (rooms.size()-occupiedRooms) +"/"+ rooms.size();
        for (Room room : rooms.values()) {
            returnString += "\n    - " + room.toString();
        }

        return returnString;
    }
}

class Room {
    private Status status;
    public final String roomID;
    public Room(){
        status = Status.Available;
        roomID = UUID.randomUUID().toString();
    }

    public boolean isAvailable(){
        return status == Status.Available;
    }

    public boolean reserve() throws ReservationException {
        if(status == Status.Available){
            status = Status.PendingReservation;
            return true;
        }
        else{
            throw new ReservationException("Room with id " + roomID + " is not available.");
        }
    }

    public boolean cancelReservation() throws ReservationException {
        if(status == Status.PendingReservation){
            status = Status.Available;
            return true;
        }
        else{
            throw new ReservationException("Trying to remove a reservation from room " + roomID + " while it's status is " + status.toString() +
                " instead of Available");
        }
    }

    public boolean confirmReservation() throws ReservationException {
        if(status == Status.PendingReservation){
            status = Status.Reserved;
            return true;
        }
        else{
            throw new ReservationException("Trying to confirm a reservation on room " + roomID + " while it's status is " + status.toString() +
                " instead of PendingReservation");
        }
    }

    @Override
    public String toString() {
        return "RoomID: " + roomID + " (" + (status.toString()) + ")";
    }

    private enum Status{
        Available,
        PendingReservation,
        Reserved
    }
}
