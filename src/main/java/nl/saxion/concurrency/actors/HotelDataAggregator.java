package nl.saxion.concurrency.actors;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import jnr.ffi.Struct;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.messages.RentARoomMessage;
import scala.util.hashing.ByteswapHashing;

//todo: Finsh the implementation of the class
public class HotelDataAggregator extends AbstractBehavior<RentARoomMessage> {
    private ActorRef reportTo;
    private long dataToExpect, dataReceived = 0;
    private StringBuilder builder = new StringBuilder();

    public HotelDataAggregator(ActorContext<RentARoomMessage> context) {
        super(context);
    }

    public static Behavior<RentARoomMessage> create() {
        return Behaviors.setup(HotelDataAggregator::new);
    }

    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
            .onMessage(RentARoomMessage.hotelMessagesToExpect.class, this::dataToReceive)
            .onMessage(RentARoomMessage.RequestHotelInformation.class, this::receiveData)
            .build();
    }

    /**
     * Receive data from the hotels. This data will contain all information we need to know
     * about the hotel (name, id, room information).
     * @param message message containing the hotel information.
     * @return
     */
    private Behavior<RentARoomMessage> receiveData(RentARoomMessage.RequestHotelInformation message){
        if(builder.length() > 0){
            builder.append("\n\n " + message.hotelInformation);
        }
        else{
            builder.append(message.hotelInformation);
        }
        dataReceived++;

        //if the data is successfully aggregated and sent back, we stop the aggregator.
        if(report()){
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    /**
     * Handle the message which contains information on how many hotels we should expect the data from
     * and where to send the final result to.
     * @param message message containing the amount of hotels and a reference to the actor where the final result should go to.
     * @return
     */
    private Behavior<RentARoomMessage> dataToReceive(RentARoomMessage.hotelMessagesToExpect message){
        reportTo = message.reportTo;
        dataToExpect = message.amount;

        //if the data is successfully aggregated and sent back, we stop the aggregator.
        if(report()){
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    /**
     * Report the final result to the actor that requested the hotel list.
     * @return Returns true if successfull, or false if either not all the data has been received, or if
     * there is no actor known yet to report to.
     */
    private boolean report(){
        if(reportTo != null && dataToExpect == dataReceived){
            String message = builder.toString();
            if(message == null || message.equals("")) message = "There are currently no hotels! Press 'H' to create a hotel!";
            reportTo.tell(new RentARoomMessage.Response(message));
            return true;
        }
        return false;
    }
}
