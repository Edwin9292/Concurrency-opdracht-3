package nl.saxion.concurrency.actors;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.domain.ReservationException;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class HotelManager extends AbstractBehavior<RentARoomMessage> {
    Hotel hotel;
    HashMap<String, ArrayList<String>> notYetConfirmedReservations = new HashMap<>();

    public HotelManager(ActorContext<RentARoomMessage> context, Hotel hotel) {
        super(context);
        this.hotel = hotel;
        // Register with the receptionist
        context.getSystem().receptionist().tell(Receptionist.register(CREATE_HOTEL_KEY, context.getSelf()));
    }

    public static final ServiceKey<RentARoomMessage> CREATE_HOTEL_KEY = ServiceKey.create(RentARoomMessage.class, "RegisterHotelService");

    public static Behavior<RentARoomMessage> create(Hotel hotel) {
        return Behaviors.setup(context -> {
            return new HotelManager(context, hotel);
        });
    }

    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
            .onMessage(RentARoomMessage.RequestHotelInformation.class, this::handleDataRequest)
            .onMessage(RentARoomMessage.StopBehaviors.class, this::handleStop)
            .onMessage(RentARoomMessage.HotelReservation.class, this::handleReservation)
            .onMessage(RentARoomMessage.CancelReservation.class, this::handleReservationCancellation)
            .onMessage(RentARoomMessage.ConfirmReservation.class, this::handleReservationConfirmation)
            .build();
    }

    /**
     * This method will send the hotel information to the actor that is provided in the message as "sendTo"
     * @param message message that contains a reference to the aggregator where the hotel information should be send to.
     * @return
     */
    private Behavior<RentARoomMessage> handleDataRequest(RentARoomMessage.RequestHotelInformation message) {
        message.sendTo.tell(new RentARoomMessage.RequestHotelInformationReply(hotel.toString()));
        return Behaviors.same();
    }

    /**
     * Handles the message to stop this hotel manager. It will tell the sender a message that the attempt was
     * successful.
     * @param message
     * @return
     */
    private Behavior<RentARoomMessage> handleStop(RentARoomMessage.StopBehaviors message){
        message.sender.tell(new RentARoomMessage.Response("Successfully deleted hotel " + hotel.name + " (ID: " + hotel.id + ")"));
        return Behaviors.stopped();
    }

    /**
     * Handle a request for a reservation at this hotel with X amount of rooms.
     * It will tell the sender (the aggregator) a list of room id's if the request was succesfull.
     * If the request failed, it will tell the aggregator what failed.
     * @param message Message containing the amount of rooms and reference to the aggregator.
     * @return
     */
    private Behavior<RentARoomMessage> handleReservation(RentARoomMessage.HotelReservation message){
        try{
            ArrayList<String> reservedRoomIds = hotel.reserveRooms(message.amountOfRooms);
            String reservationID = message.aggregator.path().name();
            this.notYetConfirmedReservations.put(reservationID, reservedRoomIds);

            //send the aggregator a list of the reserved room id's
            message.aggregator.tell(new RentARoomMessage.HotelReservationReply(true, getContext().getSelf(), reservedRoomIds));
        } catch (ReservationException e) {
            //if the reservation failed, send the aggregator a message with failed status and message what failed.
            message.aggregator.tell(new RentARoomMessage.HotelReservationReply(false, getContext().getSelf(), e.getMessage()));
        }
        return Behaviors.same();
    }

    /**
     * Handle the cancellation of a reservation. This method will check if the hotel contains a reservation by this id.
     * If it does, it will try to cancel the reservation for each given hotelroom. If something goes wrong, it will tell
     * the aggregator that the cancellation failed, with the error as message.
     * @param message Message containing the reservation id to cancel and a reference to the aggregator
     * @return
     */
    private Behavior<RentARoomMessage> handleReservationCancellation(RentARoomMessage.CancelReservation message){
        //check if there is a reservation by this number
        if(notYetConfirmedReservations.containsKey(message.reservationNumber)){
            ArrayList<String> roomNumbers = notYetConfirmedReservations.get(message.reservationNumber);
            try {
                for(String roomNumber: roomNumbers) {
                    hotel.cancelReservation(roomNumber);
                }
                message.replyTo.tell(new RentARoomMessage.CancelReservationReply(
                    true, getContext().getSelf(),"Success"));
            } catch (ReservationException e) {
                message.replyTo.tell(new RentARoomMessage.CancelReservationReply(false, getContext().getSelf(),
                    e.getMessage()));
            }
            notYetConfirmedReservations.remove(message.reservationNumber);
        }
        //else reply that this reservation does not exist
        else{
            message.replyTo.tell(new RentARoomMessage.CancelReservationReply(false, getContext().getSelf(),
                "Hotel " + hotel.id + " does not have a reservation by this number."));
        }
        return Behaviors.same();
    }

    /**
     * Handle the confirmation of a reservation. This method will check if the hotel contains a reservation by this id.
     * If it does, it will try to confirm the reservation for every given hotel room. If something goes wrong, it will
     * tell the aggregator that it failed together with the reason. Else it will send a message with status = true;
     * @param message Message containing the reservation id to confirm and a reference to the aggregator.
     * @return
     */
    private Behavior<RentARoomMessage> handleReservationConfirmation(RentARoomMessage.ConfirmReservation message){
        //check if there is a reservation by this number
        if(notYetConfirmedReservations.containsKey(message.reservationNumber)){
            ArrayList<String> roomNumbers = notYetConfirmedReservations.get(message.reservationNumber);
            try {
                for(String roomNumber: roomNumbers) {
                    hotel.confirmReservation(roomNumber);
                }
                message.replyTo.tell(new RentARoomMessage.ConfirmReservationReply(true, getContext().getSelf(),
                    "Success"));
            } catch (ReservationException e) {
                message.replyTo.tell(new RentARoomMessage.ConfirmReservationReply(false, getContext().getSelf(),
                e.getMessage()));
            }
            notYetConfirmedReservations.remove(message.reservationNumber);
        }
        //else reply that this reservation does not exist
        else{
            message.replyTo.tell(new RentARoomMessage.ConfirmReservationReply(false, getContext().getSelf(),
                "Hotel " + hotel.id + " does not have a reservation by this number."));
        }
        return Behaviors.same();
    }


}
