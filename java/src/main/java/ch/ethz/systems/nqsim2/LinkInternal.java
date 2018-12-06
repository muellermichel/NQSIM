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
    // Time to traverse the link.
    private final int timeToPass;
    // Number of free slots in the queue.
    private int currentCapacity;

    // TODO - add waiting center to wait for a specific route!
    // Note: length in meters; speed in m/s
    public LinkInternal(int capacity, int length, int speed) {
        this.queue = new ArrayDeque<>(capacity);
        this.timeToPass = length / speed;
        this.currentCapacity = capacity;
    }

    protected int timeToPass() {
        return timeToPass;
    }

    // TODO - have a push_wait (waits for a vehicle)
    public boolean push(int time, Agent agent) {
        // TODO - check for agents waiting to get into the route!
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

    public int currentCapacity() {
        return this.currentCapacity;
    }

    public Queue<Agent> queue() {
        return this.queue;
    }

}