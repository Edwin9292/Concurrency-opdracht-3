package nl.saxion.concurrency.actors;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import jnr.ffi.provider.jffi.AnnotationTypeMapper;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HotelReservationAggregator extends AbstractBehavior<RentARoomMessage> {
    private ActorRef reservationReportTo, cancellationReportTo, confirmationReportTo;
    private ActorRef parentAgent;
    private long dataToExpect, reservationDataReceived, cancellationDataReceived, confirmationDataReceived = 0;
    private HashMap<ActorRef, ArrayList<String>> successfulReservations = new HashMap<>();
    private String reservationErrorMessage = "Failed to make the reservation: \n";
    private String cancellationErrorMessage = "Failed to cancel the reservation: \n";
    private int failedCancellations = 0;
    private String confirmationErrorMessage = "Failed to confirm the reservation: \n";
    private int failedConfirmations = 0;

    public HotelReservationAggregator(ActorContext<RentARoomMessage> context, ActorRef parentAgent) {
        super(context);
        this.parentAgent = parentAgent;
    }

    public static Behavior<RentARoomMessage> create(ActorRef agent) {
        return Behaviors.setup((context) -> new HotelReservationAggregator(context, agent));
    }

    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
            .onMessage(RentARoomMessage.hotelMessagesToExpect.class, this::ReservationDataToExpect)
            .onMessage(RentARoomMessage.HotelReservationReply.class, this::receiveReservationData)
            .onMessage(RentARoomMessage.CancelReservation.class, message -> {cancellationReportTo = message.replyTo; reverseReservations(); return this;})
            .onMessage(RentARoomMessage.CancelReservationReply.class, this::receiveCancellationData)
            .onMessage(RentARoomMessage.ConfirmReservation.class, message -> {confirmationReportTo = message.replyTo; confirmReservation(); return this;})
            .onMessage(RentARoomMessage.ConfirmReservationReply.class, this::receiveConfirmationData)
            .onSignal(PostStop.class, signal -> cleanUpWhenStopping())
            .build();
    }

    /**
     * Handle the message which contains information on how many hotels we should expect the data from
     * and where to send the final result to.
     * @param message message containing the amount of hotels and a reference to the actor where the final result should go to.
     * @return
     */
    private Behavior<RentARoomMessage> ReservationDataToExpect(RentARoomMessage.hotelMessagesToExpect message){
        reservationReportTo = message.reportTo;
        dataToExpect = message.amount;
        return reportReservation();
    }

    /**
     * Receive data from the hotels. This data contains the list status of the reservation
     * and if success, it will contain an object with all the room id's. If failed,
     * the message will contain of the error.
     * @param message message containing the hotel information.
     * @return
     */
    private Behavior<RentARoomMessage> receiveReservationData(RentARoomMessage.HotelReservationReply message){
        reservationDataReceived++;
        //if the reservation is success, add it to the success hashmap in case we need to reverse the reservation
        if(message.status){
            successfulReservations.put(message.sender, (ArrayList<String>)message.message);
        }
        else{
            //lets save the error message so we can send it to the client when we have all the data
            //We don't reverse the successful reservations yet, since we might receive more later.
            //instead we just report.
            reservationErrorMessage += "  - " + message.message + "\n";
        }
        return reportReservation();
    }

    /**
     * Report the final result to the actor that requested the reservation.
     * Reply will contain a reservation number if all reservations are successful, or an error message if
     * something went wrong.
     * @return Returns Behaviours.stopped() if we had to reverse all reservations. Else it will return Behaviours.same().
     */
    private Behavior<RentARoomMessage> reportReservation(){
        if(reservationReportTo != null && dataToExpect == reservationDataReceived) {
            //all reservations were successful, reply with a reservation number
            if (reservationDataReceived == successfulReservations.size()) {
                reservationReportTo.tell(new RentARoomMessage.Response("Your reservation number is: " + getContext().getSelf().path().name()));
                return Behaviors.same();
            }
            //one or more of the reservations failed. We have to reverse all successful reservations and we stop this aggregator.
            else {
                reverseReservations();
                reservationReportTo.tell(new RentARoomMessage.Response(reservationErrorMessage));
                return Behaviors.stopped();
            }
        }
        return Behaviors.same();
    }

    /**
     * Receive cancellation data from the hotels. This data contains status of the cancellation.
     * If failed, the message will contain of the error and will be stored to send to the client.
     * @param message message containing the status and error information.
     * @return
     */
    private Behavior<RentARoomMessage> receiveCancellationData(RentARoomMessage.CancelReservationReply message){
        cancellationDataReceived++;
        //if the cancellation failed for a hotel, we add it to the cancellation error
        if(!message.status){
            //lets save the error message so we can send it to the client when we have all the data
            reservationErrorMessage += "  - " + message.message + "\n";
            failedCancellations++;
        }
        return reportCancellation();
    }

    /**
     * Report the final result to the actor that requested the cancellation.
     * Reply will contain a success message if all reservations are successful, or an error message if
     * something went wrong.
     * @return Returns Behaviours.stopped() if the reservation is cancelled. Else it will return Behaviours.same().
     */
    private Behavior<RentARoomMessage> reportCancellation(){
        if(cancellationReportTo != null && dataToExpect == cancellationDataReceived) {
            //all cancellations were successful, reply with a success message
            if (failedCancellations == 0) {
                cancellationReportTo.tell(new RentARoomMessage.Response("Your reservation with number " + getContext().getSelf().path().name() +
                    " is successfully cancelled."));
            }
            //one or more of the cancellations failed. We send the error to the client
            else {
                cancellationReportTo.tell(new RentARoomMessage.Response(cancellationErrorMessage));
            }
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    /**
     * Receive confirmation data from the hotels. This data contains the status of the confirmation.
     * If failed, the message will also contain the error.
     * @param message message containing the status and error information.
     * @return
     */
    private Behavior<RentARoomMessage> receiveConfirmationData(RentARoomMessage.ConfirmReservationReply message){
        confirmationDataReceived++;
        //if the cancellation failed for a hotel, we add it to the cancellation error
        if(!message.status){
            //lets save the error message so we can send it to the client when we have all the data
            confirmationErrorMessage += "  - " + message.message + "\n";
            failedConfirmations ++;
        }
        return reportConfirmation();
    }

    /**
     * Report the final result to the actor that requested the confirmation.
     * Reply will contain a success message if all reservations are successful, or an error message if
     * something went wrong.
     * @return Returns Behaviours.stopped() if the reservation is confirmed. Else it will return Behaviours.same().
     */
    private Behavior<RentARoomMessage> reportConfirmation(){
        if(confirmationReportTo != null && dataToExpect == confirmationDataReceived) {
            //all confirmations were successful, reply with a success message
            if (failedCancellations == 0) {
                confirmationReportTo.tell(new RentARoomMessage.Response("Your reservation with number " + getContext().getSelf().path().name() +
                    " is successfully confirmed."));
            }
            //one or more of the confirmations failed. We send the error to the client
            else {
                confirmationReportTo.tell(new RentARoomMessage.Response(confirmationErrorMessage));
            }
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    private void reverseReservations() {
        for(Map.Entry<ActorRef, ArrayList<String>> reservation : successfulReservations.entrySet()){
            reservation.getKey().tell(new RentARoomMessage.CancelReservation(getContext().getSelf().path().name(), getContext().getSelf()));
        }
    }
    private void confirmReservation(){
        for(Map.Entry<ActorRef, ArrayList<String>> reservation : successfulReservations.entrySet()){
            reservation.getKey().tell(new RentARoomMessage.ConfirmReservation(getContext().getSelf().path().name(), getContext().getSelf()));
        }
    }

    /**
     * send the Agent that created this aggregator that it has stopped working so the agent can remove
     * the reservation from its list.
     * @return
     */
    private Behavior<RentARoomMessage> cleanUpWhenStopping(){
        if(parentAgent != null){
            parentAgent.tell(new RentARoomMessage.AggregatorStopped(getContext().getSelf()));
        }
        return this;
    }

}
