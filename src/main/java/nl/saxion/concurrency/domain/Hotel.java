package nl.saxion.concurrency.domain;

import java.io.Serializable;
import java.util.UUID;

public class Hotel implements Serializable {

    //Generate a unique random id
    //because id is final we can make it public
    public final String id = UUID.randomUUID().toString();


    //todo: Implement your Hotel domain object


}
