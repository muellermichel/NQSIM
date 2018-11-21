package ch.ethz.systems.nqsim2;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

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

    public LinkInternal(int capacity) {
        this.queue = new ArrayDeque<>(capacity);
        // len (=1000 m) / Max(5, 60) -> 16
        this.free_capacity = 16;
        // len (=1000 m) / vel (= 60 Km/h) -> 60 secs
        //this.free_time = 60;
        // Random speed between 20 Km/h and 100 Km/h
        this.free_time = 3600 / Math.max(new Random().nextInt(100), 40);
        // len (=1000 m) / 5 -> 200 secs
        this.jam_time = 200;
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