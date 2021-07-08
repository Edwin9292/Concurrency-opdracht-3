package nl.saxion.concurrency;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import nl.saxion.concurrency.actors.RentARoom;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.time.Duration;
import java.util.ArrayList;
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
        Map<String, Object> overrides = new HashMap<>();
        String port = ArgumentUtil.getAkkaPort(args);
        overrides.put("akka.remote.artery.canonical.port", port);

        Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load());
        system = ActorSystem.create(RentARoom.create(), "RentARoomSystem",config);

        commandLoop();

    }


    public void commandLoop() {
        String help = "Commands:\n" +
                "\n" +
                "L: List hotels\n" +
                "B: Add broker\n" +
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
                    addBroker();
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

    private void addBroker() {
        //todo: add broker to the system
    }

    private void requestReservation() {
    }

    private void cancelReservation() {
    }

    private void confirmReservation() {
    }

    private void deleteHotel() {
    }

    private void addHotel() {
        System.out.println("Give name of the hotel:");
        Scanner s = new Scanner(System.in);
        String name = s.nextLine();
        System.out.println("Give number of rooms:");
        String roomsString = s.nextLine();
        int rooms = 0;
        try {
            rooms = Integer.parseInt(roomsString);
        } catch (Exception e) {
            System.err.println("Invalid input");
            return;
        }
        //todo: finish the code
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
    }

}
