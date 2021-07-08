package nl.saxion.concurrency.actors;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import nl.saxion.concurrency.messages.RentARoomMessage;

//todo: Finsh the implementation of the class
public class RentARoom extends AbstractBehavior<RentARoomMessage> {



    public RentARoom(ActorContext<RentARoomMessage> context) {
        super(context);
    }

    public static Behavior<RentARoomMessage> create() {
        return Behaviors.setup(context -> {
            return new RentARoom(context);
        });
    }
    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder().
                build();
    }

}
