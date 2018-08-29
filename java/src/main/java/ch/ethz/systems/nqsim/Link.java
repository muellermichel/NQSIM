package ch.ethz.systems.nqsim;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public final class Link {
    private String id;
    private int length;
    private int free_flow_velocity;
    private int free_flow_capacity;
    private int jam_capacity;
    private int free_flow_travel_time;
    private int jam_travel_time;
    private Queue<Agent> q;

    private static long instance_counter = 0;
    private static int is_using_auto_id = -1;

    private static String getAutoId() {
        long id = instance_counter;
        return String.valueOf(id);
    }

    private Link(String id, int length, int free_flow_velocity, int autoIdOption, Queue<Agent> queue) throws LinkException {
        if (is_using_auto_id != autoIdOption && is_using_auto_id != -1) {
            throw new LinkException("cannot mix auto ids and fixed ids for links in same sim");
        }
        if (length < 0) {
            throw new IllegalArgumentException("negative length");
        }
        if (free_flow_velocity < Constants.JAM_VELOCITY) {
            throw new IllegalArgumentException(
                    "free flow velocity cannot be lower than jam velocity"
            );
        }
        this.id = id;
        this.length = length;
        this.free_flow_velocity = free_flow_velocity;
        this.q = queue;
        this.free_flow_capacity = this.length / Math.max(
                Constants.JAM_AGENT_LENGTH,
                Constants.FREE_FLOW_AGENT_LENGTH_PER_KPH * this.free_flow_velocity
        );
        this.jam_capacity = this.length / Constants.JAM_AGENT_LENGTH;
        this.free_flow_travel_time = this.length / this.free_flow_velocity;
        this.jam_travel_time = this.length / Constants.JAM_VELOCITY;
        is_using_auto_id = autoIdOption;
        instance_counter += 1;
    }

    public Link(int length, int free_flow_velocity) throws LinkException {
        this(getAutoId(), length, free_flow_velocity, 1, new TransportQueue());
    }

    public Link(String id, int length, int free_flow_velocity) throws LinkException {
        this(id, length, free_flow_velocity, 0, new TransportQueue());
    }

    @Override
    public String toString() {
        return "Link{" +
            "length=" + length +
            ", free_flow_velocity=" + free_flow_velocity +
            ", free_flow_capacity=" + free_flow_capacity +
            ", q=" + q +
            '}';
    }

    public boolean isAccepting() {
        if (this.q.size() < this.jam_capacity) {
            return true;
        }
        return false;
    }

    //very inefficient! don't use this outside of testing code!
    public Agent get(int index) {
        Iterator<Agent> iterator = this.q.iterator();
        int currIndex = 0;
        while (iterator.hasNext()) {
            Agent currAgent = iterator.next();
            if (currIndex == index) {
                return currAgent;
            }
            currIndex++;
        }
        throw new IndexOutOfBoundsException(String.format("%d", index));
    }

    public Agent peek() {
        return this.q.peek();
    }

    public void add(Agent agent) throws LinkException {
        if (this.q.size() < this.free_flow_capacity) {
            agent.time_to_pass_link = this.free_flow_travel_time;
        }
        else if (this.q.size() < this.jam_capacity) {
            agent.time_to_pass_link = this.jam_travel_time;
        }
        else {
            throw new LinkException("link full");
        }
        this.q.add(agent);
    }

    public Agent removeFirstWaiting() throws LinkException {
        Agent to_be_removed = this.peek();
        if (to_be_removed == null) {
            throw new LinkException(String.format("no agent waiting on link"));
        }
        Agent popped = this.q.poll();
        if (popped != to_be_removed) {
            throw new LinkException(String.format("this should not happen: peek is not first waiting on link"));
        }
        popped.current_travel_time = 0;
        return popped;
    }

    public int queueLength() {
        return this.q.size();
    }

    public void tick(int delta_t) {
        for (Agent agent:this.q) {
            agent.tick(delta_t);
        }
    }
}
