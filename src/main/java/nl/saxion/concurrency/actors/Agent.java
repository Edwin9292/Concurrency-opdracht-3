package nl.saxion.concurrency.actors;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.util.*;


public class Agent extends AbstractBehavior<RentARoomMessage> {
    private final HashMap<String, ActorRef<RentARoomMessage>> hotels = new HashMap<>();
    private static final HashMap<String, ActorRef<RentARoomMessage>> reservationAggregators = new HashMap<>();

    public Agent(ActorContext<RentARoomMessage> context) {
        super(context);

        // Subscribe to the receptionist to listen which hotel managers are available.
        ActorRef<Receptionist.Listing> hotelAdapter = context.messageAdapter(Receptionist.Listing.class, RentARoomMessage.ReceiveHotelsList::new);
        context.getSystem().receptionist().tell(Receptionist.subscribe(HotelManager.CREATE_HOTEL_KEY, hotelAdapter));

        // Register with the receptionist so we can use it with the group router
        context.getSystem().receptionist().tell(Receptionist.register(CREATE_AGENT_KEY, context.getSelf()));
    }

    public static final ServiceKey<RentARoomMessage> CREATE_AGENT_KEY = ServiceKey.create(RentARoomMessage.class, "RegisterHotelAgent");

    public static Behavior<RentARoomMessage> create() {
        return Behaviors.setup(Agent::new);
    }

    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
            .onMessage(RentARoomMessage.ReceiveHotelsList.class, this::updateHotelList)
            .onMessage(RentARoomMessage.ListHotels.class, this::handleHotelDataRequest)
            .onMessage(RentARoomMessage.DeleteHotel.class, this::handleDeleteHotel)
            .onMessage(RentARoomMessage.Reservation.class, this::handleReservation)
            .onMessage(RentARoomMessage.AggregatorStopped.class, message -> {reservationAggregators.remove(message.aggregator.path().name()); return Behaviors.same();})
            .onMessage(RentARoomMessage.CancelReservation.class, this::handleReservationCancellation)
            .onMessage(RentARoomMessage.ConfirmReservation.class, this::handleReservationConfirmation)
            .build();
    }

    /**
     * Update the list with hotels whenever the receptionist notices a hotel is created or deleted.
     * @param message message containing a list of all currently active hotel managers.
     * @return
     */
    private Behavior<RentARoomMessage> updateHotelList(RentARoomMessage.ReceiveHotelsList message){
        List<String> deleteList = new ArrayList<>();

        // Figure out which actors (hotel managers) have appeared / disappeared
        Set<ActorRef<RentARoomMessage>> liveHotels = message.hotels.getServiceInstances(HotelManager.CREATE_HOTEL_KEY);
        for (ActorRef<RentARoomMessage> actor : liveHotels) {
            // The name of the actor is the hotel id...
            // See if it is a new hotel, if so - add it
            if (!hotels.containsKey(actor.path().name())) {
                getContext().getLog().info("[{}] Discovered new actor for hotel {}", getContext().getSelf().path().name(), actor.path().name());
                hotels.put(actor.path().name(), actor);
            }
        }
        // See if any hotels have seized to exist
        for (Map.Entry<String, ActorRef<RentARoomMessage>> entry : hotels.entrySet()) {
            if (!liveHotels.contains(entry.getValue())) {
                getContext().getLog().warn("[{}] Hotel {} no longer exists", getContext().getSelf().path().name(), entry.getKey());
                deleteList.add(entry.getKey());
            }
        }
        for (String key : deleteList) {
            hotels.remove(key);
        }
        return Behaviors.same();
    }

    /**
     * Handle the deletion of a hotel. It will get the reference to the hotelmanager actor. If it does not exist,
     * it will tell the sender that the given hotel does not exist.
     * If the hotel does exist, it will tell the hotel manager that it should stop it's behaviours.
     * @param message message containing the id of the hotel to delete.
     * @return
     */
    private Behavior<RentARoomMessage> handleDeleteHotel(RentARoomMessage.DeleteHotel message){
        if(hotels.containsKey(message.id)){
            ActorRef hotelToDelete = hotels.get(message.id);
            hotelToDelete.tell(new RentARoomMessage.StopBehaviors(message.sender));
        }
        else{
            message.sender.tell(new RentARoomMessage.Response("No hotel exists with id: " + message.id));
        }
        return Behaviors.same();
    }

    /**
     * Request a list of all hotels. It will send a request for the hotel information to all the known hotel managers.
     * This request will contain a reference to a data aggregator, which is responsible for merging all the data
     * and sending it to the original sender (client)
     * @param message message containing the sender of the list request.
     * @return
     */
    private Behavior<RentARoomMessage> handleHotelDataRequest(RentARoomMessage.ListHotels message){
        ActorRef dataAggregator = getContext().spawn(HotelDataAggregator.create(), "hotelDataAggregator" + (int)(Math.random()*1000));

        dataAggregator.tell(new RentARoomMessage.hotelMessagesToExpect(message.sender, hotels.size()));
        for (ActorRef<RentARoomMessage> hotelManager: hotels.values()) {
            hotelManager.tell(new RentARoomMessage.RequestHotelInformation("", dataAggregator));
        }

        return Behaviors.same();
    }

    /**
     * Handle new reservations. New reservations will be checked for validation (if all the hotels exist)
     * If they do exist, it will create an aggregator that will handle all the communication with the hotel managers.
     * If one of the hotels isn't available, the aggregator will reverse all already made hotel reservations, and tell the
     * user that the reservation failed.
     * @param message Message containing the hotels and the amount of rooms per hotel to reserve.
     * @return
     */
    private Behavior<RentARoomMessage> handleReservation(RentARoomMessage.Reservation message){
        //first make sure all the hotels actually exist.
        for(String hotelID : message.reservation.keySet()){
            if(!hotels.containsKey(hotelID)){
                message.replyTo.tell(new RentARoomMessage.Response("No hotel with id " + hotelID + " exists. " +
                    "Your Reservation has been cancelled"));
                return Behaviors.same();
            }
        }
        //create a reservation id and make sure it does not exist yet
        String reservationID = UUID.randomUUID().toString();
        while(Agent.reservationAggregators.containsKey(reservationID)){
            reservationID = UUID.randomUUID().toString();
        }
        //create an aggregator to merge all the reservation replies and reverse them if something went wrong.
        ActorRef<RentARoomMessage> reservationAggregator = getContext().spawn(HotelReservationAggregator.create(getContext().getSelf()), reservationID);
        Agent.reservationAggregators.put(reservationID, reservationAggregator);
        reservationAggregator.tell(new RentARoomMessage.hotelMessagesToExpect(message.replyTo, message.reservation.size()));

        //send the reservation request to every hotel from the reservation
        ArrayList<ActorRef> reservationHotelActors = new ArrayList<>();
        for(Map.Entry<String, Integer> hotelReservation: message.reservation.entrySet()){
            ActorRef<RentARoomMessage> hotelManager = hotels.get(hotelReservation.getKey());
            hotelManager.tell(new RentARoomMessage.HotelReservation(hotelReservation.getValue(), reservationAggregator));
        }
        return Behaviors.same();
    }

    /**
     * Handle the cancellation of a reservation. First it will check if there is a reservation by this id.
     * If there is, it will tell the aggregator that is responsible for this reservation to cancel it.
     * @param message Message containing the reservation id and reference to the user.
     * @return
     */
    private Behavior<RentARoomMessage> handleReservationCancellation(RentARoomMessage.CancelReservation message){
        //check if the reservation number exists, if so, tell all the involved hotels to cancel the reservation.
        if(Agent.reservationAggregators.containsKey(message.reservationNumber)){
            ActorRef<RentARoomMessage> aggregator = Agent.reservationAggregators.get(message.reservationNumber);
            aggregator.tell(message);
        }
        else{
            message.replyTo.tell(new RentARoomMessage.Response("No reservation exists by number " + message.reservationNumber));
        }
        return Behaviors.same();
    }

    /**
     * Handle the confirmation of a reservation. First it will check if there is a reservation by this id. If there is
     * one, it will tell the corresponding aggregator to confirm the reservation.
     * @param message Message containing the reservation id and a reference to the user.
     * @return
     */
    private Behavior<RentARoomMessage> handleReservationConfirmation(RentARoomMessage.ConfirmReservation message){
        //check if the reservation number exists, if so, tell all the involved hotels to cancel the reservation.
        if(Agent.reservationAggregators.containsKey(message.reservationNumber)){
            ActorRef<RentARoomMessage> aggregator = Agent.reservationAggregators.get(message.reservationNumber);
            aggregator.tell(message);
        }
        else{
            message.replyTo.tell(new RentARoomMessage.Response("No reservation exists by number " + message.reservationNumber));
        }
        return Behaviors.same();
    }

}
