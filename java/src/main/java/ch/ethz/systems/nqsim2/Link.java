package ch.ethz.systems.nqsim2;

import java.util.ArrayDeque;
import java.util.Queue;

public class Link {
    // Id of the Link inside the owner realm (from realm).
    private final int id;
    // Source real id.
    private final int fromRealm;
    // Destination real id.
    private final int toRealm;
    // Timestamp of the next agent to arrive.
    private int nextTime;
    // Queue of agents on this link.
    private final Queue<Agent> queue;
    // Maximum number of agents on the link to be considered free.
    private final int free_capacity; // TODO - check default values.
    // Time it takes to pass the link under free capacity.
    private final int free_time; // TODO - check default values.
    // Time it takes to pass the link under jam capacity.
    private final int jam_time; // TODO - check default values.
    // Basically queue.capacity - queue.size
    private int currentCapacity;

    public Link(int id, int fromRealm, int toRealm, int capacity) {
        this.id = id;
        this.fromRealm = fromRealm;
        this.toRealm = toRealm;
        this.queue = new ArrayDeque<>(capacity);
        this.free_capacity = new Float(capacity * 0.8f).intValue();
        this.free_time = 60;
        this.jam_time = 120;
        this.currentCapacity = capacity;
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

    public int id() {
        return this.id;
    }

    public int torealm() {
        return this.toRealm;
    }

    public int fromrealm() {
        return this.fromRealm;
    }

    public int nexttime () {
        return this.nextTime;
    }

    public int nexttime (int nexttime) {
        return this.nextTime = nexttime;
    }

    public int freeCapacity() {
        return this.free_capacity;
    }

    public int freeTime() {
        return this.free_time;
    }

    public int jamTime() {
        return this.jam_time;
    }

    public int currentCapacity() {
        return this.currentCapacity;
    }

    public Queue<Agent> queue() {
        return this.queue;
    }

}