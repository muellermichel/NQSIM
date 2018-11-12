package ch.ethz.systems.nqsim2;

import java.util.ArrayDeque;
import java.util.Queue;

public class Link {
    // Id of the link (index for World.links).
    protected int id;
    // Timestamp of the next agent to arrive.
    protected int nextTime;
    // Queue of agents on this link.
    protected Queue<Agent> queue;
    // Maximum number of agents on the link to be considered free.
    protected int free_capacity; // TODO - check default values.
    // Time it takes to pass the link under free capacity.
    protected int free_time; // TODO - check default values.
    // Time it takes to pass the link under jam capacity.
    protected int jam_time; // TODO - check default values.
    // Basically queue.capacity - queue.size
    protected int currentCapacity;
    // If a node is internal. Boundary/external nodes are processed seperately.
    protected boolean internal;

    public Link(int id, int capacity, boolean internal) {
        this.id = id;
        this.queue = new ArrayDeque<>(capacity);
        this.free_capacity = new Float(capacity * 0.8f).intValue();
        this.free_time = 60;
        this.jam_time = 120;
        this.currentCapacity = capacity;
        this.internal = internal;
    }

    protected int timeToPass() {
        return currentCapacity > free_capacity ? free_time : jam_time;
    }

    public boolean push(Agent agent) {
        if (currentCapacity > 0) { 
            queue.add(agent);
            currentCapacity--;
            return true;
        } else {
            return false;
        }
    }

    public void pop() {
        queue.poll();
        currentCapacity++;
    }

}