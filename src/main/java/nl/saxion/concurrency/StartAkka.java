package nl.saxion.concurrency;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import nl.saxion.concurrency.actors.RentARoom;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

public class StartAkka {

    private ActorSystem<RentARoomMessage> system;

    public static void main(String[] args) {
        new StartAkka().run(args);

    }

    private void run(String[] args) {
        system = ActorSystem.create(RentARoom.create(), "RentARoomSystem");

        commandLoop();
    }


    public void commandLoop() {
        String help = "Commands:\n" +
                "\n" +
                "L: List hotels\n" +
                "B: Add agent\n" +
                "H: Add hotels\n" +
                "D: Delete hotels\n" +
                "R: Request reservation\n" +
                "X: Cancel reservation\n" +
                "C: Confirm resrvation\n" +
                "?: This menu\n" +
                "Q: Quit\n";
        System.out.println(help);
        Scanner s = new Scanner(System.in);
        String c = s.nextLine().toLowerCase();
        while (!c.equals("q")) {
            switch (c) {
                case "?":
                    System.out.println(help);
                    break;
                case "l":
                    listHotels();
                    break;
                case "h":
                    addHotel();
                    break;
                case "b":
                    addAgent();
                    break;
                case "r":
                    requestReservation();
                    break;
                case "x":
                    cancelReservation();
                    break;
                case "c":
                    confirmReservation();
                    break;
                case "d":
                    deleteHotel();
                    break;
            }
            c = s.nextLine().toLowerCase();
        }

    }

    private void addAgent() {
        CompletionStage<RentARoomMessage> result =
            AskPattern.ask(system,
                replyTo -> new RentARoomMessage.SpawnAgent(replyTo),
                Duration.ofSeconds(6),
                system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }

    private void requestReservation() {
        String stop = "";
        HashMap<String, Integer> reservation = new HashMap<>();
        Scanner s = new Scanner(System.in);
        while (!stop.equals("n")){
            //print current reservation (if there is any)
            printReservationDetails(reservation);

            //get the hotel id
            String hotelID = askHotelID(reservation);

            //get the amount of rooms
            int rooms = askAmountOfRooms();

            //put the reservation information in a hashmap
            reservation.put(hotelID, rooms);

            //ask if the user wants to add more hotels/rooms to his reservation
            System.out.println("Do you want to reserve another hotel? (y/n)");
            stop = s.nextLine();
            while (!stop.equals("y") && !stop.equals("n")) {
                System.err.println("Invalid input!");
                System.out.println("Do you want to reserve another hotel? (y/n)");
                stop = s.nextLine();
            }
        }
        //print final reservation
        printReservationDetails(reservation);

        //Send reservation and wait for a response
        CompletionStage<RentARoomMessage> result =
            AskPattern.ask(system,
                replyTo -> new RentARoomMessage.Reservation(reservation, replyTo),
                Duration.ofSeconds(6),
                system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        //display the information on the hotels
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }

    private void cancelReservation() {
        System.out.println("Give the id of the reservation to cancel:");
        Scanner s = new Scanner(System.in);
        String reservationNumber = s.nextLine();
        CompletionStage<RentARoomMessage> result =
            AskPattern.ask(system,
                replyTo -> new RentARoomMessage.CancelReservation(reservationNumber, replyTo),
                Duration.ofSeconds(6),
                system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }

    private void confirmReservation() {
        System.out.println("Give the id of the reservation to confirm:");
        Scanner s = new Scanner(System.in);
        String reservationNumber = s.nextLine();
        CompletionStage<RentARoomMessage> result =
            AskPattern.ask(system,
                replyTo -> new RentARoomMessage.ConfirmReservation(reservationNumber, replyTo),
                Duration.ofSeconds(6),
                system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }

    private void deleteHotel() {
        System.out.println("Give the id of the hotel to delete:");
        Scanner s = new Scanner(System.in);
        String id = s.nextLine();
        CompletionStage<RentARoomMessage> result =
            AskPattern.ask(system,
                replyTo -> new RentARoomMessage.DeleteHotel(replyTo, id),
                Duration.ofSeconds(6),
                system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }

    private void addHotel() {
        System.out.println("Give the name of the hotel:");
        Scanner s = new Scanner(System.in);
        String name = s.nextLine();
        System.out.println("Give the number of rooms:");
        String roomsString = s.nextLine();
        int rooms = 0;
        try {
            rooms = Integer.parseInt(roomsString);
        } catch (Exception e) {
            System.err.println("Invalid input");
            return;
        }
        int finalRooms = rooms;
        CompletionStage<RentARoomMessage> result =
            AskPattern.ask(system,
                replyTo -> new RentARoomMessage.CreateHotel(replyTo, name, finalRooms),
                Duration.ofSeconds(6),
                system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }

    private void listHotels() {
        CompletionStage<RentARoomMessage> result =
                AskPattern.ask(system,
                        replyTo -> new RentARoomMessage.ListHotels(replyTo),
                        Duration.ofSeconds(6),
                        system.scheduler());
        //wait on the result
        RentARoomMessage message = result.toCompletableFuture().join();
        //display the information on the hotels
        if(message instanceof RentARoomMessage.Response){
            String status = ((RentARoomMessage.Response)message).status;
            System.out.println(status);
        }
    }
    

    private void printReservationDetails(HashMap<String, Integer> reservation){
        if(reservation.size() > 0){
            System.out.println("Reservation: ");
            for (Map.Entry<String, Integer> entry : reservation.entrySet()) {
                System.out.println(" - Hotel: " + entry.getKey() + " | Rooms: " + entry.getValue());
            }
            System.out.println("");
        }
    }
    private String askHotelID(HashMap<String, Integer> currentReservation){
        Scanner s = new Scanner(System.in);
        String hotelID = "";
        while(hotelID.equals("")){
            System.out.println("Give the id of the hotel to reserve:");
            String name = s.nextLine();
            if(currentReservation.containsKey(name)){
                System.err.println("You already reserved rooms at this hotel!");
            }
            else{
                hotelID = name;
            }
        }
        return hotelID;
    }
    private int askAmountOfRooms(){
        Scanner s = new Scanner(System.in);
        int rooms = 0;
        while(rooms == 0){
            System.out.println("Give the number of rooms to reserve:");
            String roomsString = s.nextLine();
            try {
                rooms = Integer.parseInt(roomsString);
            } catch (Exception e) {
                System.err.println("Invalid input");
            }
            if(rooms == 0){
                System.err.println("Amount of rooms should be more than 0");
            }
        }
        return rooms;
    }
}
