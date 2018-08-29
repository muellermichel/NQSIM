package ch.ethz.systems.nqsim;

import java.util.LinkedList;
import java.util.Queue;

public class TransportQueue extends LinkedList<Agent> implements Queue<Agent> {
    public TransportQueue() {
        super();
    }
}
