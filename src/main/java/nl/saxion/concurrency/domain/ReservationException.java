package nl.saxion.concurrency.domain;

public class ReservationException extends Exception{
    public ReservationException(String error){
        super(error);
    }
}
