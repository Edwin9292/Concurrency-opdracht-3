package nl.saxion.concurrency.messages;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import nl.saxion.concurrency.actors.HotelManager;
import nl.saxion.concurrency.actors.HotelReservationAggregator;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public interface RentARoomMessage extends Serializable {

    /*
    Class for a general status response
     */
    class Response implements RentARoomMessage {
        public final String status;

        public Response(String status) {
            this.status = status;
        }
    }

    //Request a list of all hotels
    class ListHotels implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public ListHotels(ActorRef<RentARoomMessage>  sender) {
            this.sender = sender;
        }
    }

    // Request hotel information from hotelManagers
    class RequestHotelInformation implements RentARoomMessage {
        public final String hotelInformation;
        public final ActorRef sendTo;

        public RequestHotelInformation(String hotelInformation, ActorRef sendTo){
            this.hotelInformation = hotelInformation;
            this.sendTo = sendTo;
        }
    }

    //tell the hotelDataAggregator how many message to expect and where to send the result
    class hotelMessagesToExpect implements RentARoomMessage{
        public final ActorRef reportTo;
        public final long amount;

        public hotelMessagesToExpect (ActorRef reportTo, long amount){
            this.reportTo = reportTo;
            this.amount = amount;
        }
    }

    //create a hotel message
    class CreateHotel implements RentARoomMessage{
        public final ActorRef sender;
        public final String name;
        public final int amountOfRooms;

        public CreateHotel(ActorRef sender, String name, int amountOfRooms){
            this.sender = sender;
            this.name = name;
            this.amountOfRooms = amountOfRooms;
        }
    }

    //delete a hotel message
    class DeleteHotel implements RentARoomMessage{
        public final ActorRef sender;
        public final String id;

        public DeleteHotel(ActorRef sender, String id){
            this.sender = sender;
            this.id = id;
        }
    }

    //message to stop the behaviour
    class StopBehaviors implements RentARoomMessage{
        public final ActorRef sender;
        public StopBehaviors(ActorRef sender){
            this.sender = sender;
        }
    }

    //signal agent that the reservation aggregator has stopped
    class AggregatorStopped implements RentARoomMessage{
        public final ActorRef aggregator;
        public AggregatorStopped(ActorRef aggregator){
            this.aggregator = aggregator;
        }
    }

    //receptionist adapter
    class ReceiveHotelsList implements  RentARoomMessage{
        public final Receptionist.Listing hotels;

        public ReceiveHotelsList(Receptionist.Listing hotels){
            this.hotels = hotels;
        }
    }

    class Reservation implements RentARoomMessage {
        public final HashMap<String, Integer> reservation;
        public final ActorRef replyTo;

        public Reservation(HashMap<String,Integer> reservation, ActorRef replyTo){
            this.reservation = reservation;
            this.replyTo = replyTo;
        }
    }

    class HotelReservation implements RentARoomMessage{
        public final int amountOfRooms;
        public final ActorRef aggregator;

        public HotelReservation(int amountOfRooms, ActorRef aggregator){
            this.amountOfRooms = amountOfRooms;
            this.aggregator = aggregator;
        }
    }
    class HotelReservationReply implements RentARoomMessage{
        public final boolean status;
        public final ActorRef sender;
        public final Object message;

        public HotelReservationReply(boolean status, ActorRef sender, Object message){
            this.status = status;
            this.sender = sender;
            this.message = message;
        }
    }

    class CancelReservation implements RentARoomMessage{
        public final String reservationNumber;
        public final ActorRef replyTo;

        public CancelReservation(String reservationNumber, ActorRef replyTo){
            this.reservationNumber = reservationNumber;
            this.replyTo = replyTo;
        }
    }
    class CancelReservationReply implements RentARoomMessage{
        public final boolean status;
        public final ActorRef sender;
        public final String message;

        public CancelReservationReply(boolean status, ActorRef sender, String message){
            this.status = status;
            this.sender = sender;
            this.message = message;
        }
    }

    class ConfirmReservation implements RentARoomMessage{
        public final String reservationNumber;
        public final ActorRef replyTo;

        public ConfirmReservation(String reservationNumber, ActorRef replyTo){
            this.reservationNumber = reservationNumber;
            this.replyTo = replyTo;
        }
    }

    class ConfirmReservationReply implements RentARoomMessage{
        public final boolean status;
        public final ActorRef sender;
        public final String message;

        public ConfirmReservationReply(boolean status, ActorRef sender, String message){
            this.status = status;
            this.sender = sender;
            this.message = message;
        }
    }

    class SpawnAgent implements RentARoomMessage{
        public final ActorRef replyTo;
        public SpawnAgent(ActorRef replyTo){
            this.replyTo = replyTo;
        }
    }

}
