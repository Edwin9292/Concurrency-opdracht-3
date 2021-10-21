package nl.saxion.concurrency.actors;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.messages.RentARoomMessage;

public class RentARoom extends AbstractBehavior<RentARoomMessage> {
    ActorRef<RentARoomMessage> router;
    private static int agentCounter = 0;

    public RentARoom(ActorContext<RentARoomMessage> context) {
        super(context);

        GroupRouter<RentARoomMessage> group = Routers.group(Agent.CREATE_AGENT_KEY);
        router = context.spawn(group, "Agent-group");

        context.spawn(Agent.create(), "Agent_"+ ++agentCounter);
    }

    public static Behavior<RentARoomMessage> create() {
        return Behaviors.setup(context -> {
            return new RentARoom(context);
        });
    }

    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
            .onMessage(RentARoomMessage.ListHotels.class, this::handleListRequest)
            .onMessage(RentARoomMessage.CreateHotel.class, this::handleCreateHotel)
            .onMessage(RentARoomMessage.DeleteHotel.class, this::handleDeleteHotel)
            .onMessage(RentARoomMessage.Reservation.class, this::handleReservation)
            .onMessage(RentARoomMessage.CancelReservation.class, this::handleReservationCancellation)
            .onMessage(RentARoomMessage.ConfirmReservation.class, this::handleReservationConfirmation)
            .onMessage(RentARoomMessage.SpawnAgent.class, this::handleSpawnAgent)
            .build();
    }

    /**
     * Send the list request to an agent.
     * @param message
     * @return
     */
    private Behavior<RentARoomMessage> handleListRequest(RentARoomMessage.ListHotels message){
        router.tell(message);
        return Behaviors.same();
    }

    /**
     * Create a new Hotel and spawn an hotel Manager.
     * We don't have to forward this to the agent because the receptionist will tell the agents
     * when a hotel manager has been created. We don't save the hotel in this class because
     * it is already stored in the hotel manager. We don't want to store data in multiple places.
     * @param message
     * @return
     */
    private Behavior<RentARoomMessage> handleCreateHotel(RentARoomMessage.CreateHotel message){
        Hotel hotel = new Hotel(message.name, message.amountOfRooms);
        getContext().spawn(HotelManager.create(hotel), hotel.id);
        message.sender.tell(new RentARoomMessage.Response("Hotel \"" + hotel.name + "\" successfully created with ID: " + hotel.id));
        return Behaviors.same();
    }

    /**
     * forward the delete message to an agent
     * @param message
     * @return
     */
    private Behavior<RentARoomMessage> handleDeleteHotel(RentARoomMessage.DeleteHotel message){
        router.tell(message);
        return Behaviors.same();
    }

    /**
     * forward the reservation to an agent
     * @param message message containing the reservation and the actor to reply to
     * @return
     */
    private Behavior<RentARoomMessage> handleReservation(RentARoomMessage.Reservation message){
        router.tell(message);
        return Behaviors.same();
    }

    /**
     * forward the cancellation to an agent
     * @param message message containing the reservationNumber and the actor to reply to
     * @return
     */
    private Behavior<RentARoomMessage> handleReservationCancellation(RentARoomMessage.CancelReservation message){
        router.tell(message);
        return Behaviors.same();
    }

    /**
     * forward the confirmation to an agent
     * @param message message containing the reservationNumber and the actor to reply to
     * @return
     */
    private Behavior<RentARoomMessage> handleReservationConfirmation(RentARoomMessage.ConfirmReservation message){
        router.tell(message);
        return Behaviors.same();
    }

    /**
     * Spawn a new agent
     * @param message
     * @return
     */
    private Behavior<RentARoomMessage> handleSpawnAgent(RentARoomMessage.SpawnAgent message){
        getContext().spawn(Agent.create(), "Agent_"+ ++RentARoom.agentCounter);
        message.replyTo.tell(new RentARoomMessage.Response("Successfully created an agent with the name Agent_" + agentCounter));
        return Behaviors.same();
    }










}
