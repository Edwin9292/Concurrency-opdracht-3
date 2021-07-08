package nl.saxion.concurrency.messages;

import akka.actor.typed.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;

public interface RentARoomMessage extends Serializable {

    /*
    Class for a general status repsons
     */
    class Response implements RentARoomMessage {
        public final String status;

        public Response(String status) {
            this.status = status;
        }
    }

    /*
    List hotels in the system
     */
    class ListHotels implements RentARoomMessage {

        public final ActorRef<RentARoomMessage> sender;

        public ListHotels(ActorRef<RentARoomMessage>  sender) {
            this.sender = sender;
        }
    }

}
