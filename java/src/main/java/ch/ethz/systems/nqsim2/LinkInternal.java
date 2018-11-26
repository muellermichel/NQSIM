package ch.ethz.systems.nqsim2;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Queue;

public class LinkInternal implements Serializable {
    private static final long serialVersionUID = 221067454362884616L;
    // Timestamp of the next agent to arrive.
    private int nextTime;
    // Queue of agents on this link.
    private final Queue<Agent> queue;
    // Maximum number of agents on the link to be considered free.
    private final int free_capacity;
    // Time it takes to pass the link under free capacity.
    private final int free_time;
    // Time it takes to pass the link under jam capacity.
    private final int jam_time;
    // Basically queue.capacity - queue.size
    private int currentCapacity;

    public LinkInternal(int capacity, int free_capacity, int free_time, int jam_time) {
        this.queue = new ArrayDeque<>(capacity);
        this.free_capacity = free_capacity;
        this.free_time = free_time;
        this.jam_time = jam_time;
        this.currentCapacity = capacity;
    }

    protected int timeToPass() {
        return currentCapacity > free_capacity ? free_time : jam_time;
    }

    public boolean push(int time, Agent agent) {
        if (currentCapacity > 0) { 
            queue.add(agent);
            currentCapacity--;
            agent.linkFinishTime = time + timeToPass();
            nextTime = queue.peek().linkFinishTime;
            return true;
        } else {
            return false;
        }
    }

    public void pop() {
        queue.poll();
        currentCapacity++;
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