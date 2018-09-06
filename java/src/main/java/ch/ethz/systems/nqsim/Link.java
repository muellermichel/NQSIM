package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Iterator;
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

    private static long next_id = 0;

    private static String getAutoId() {
        long id = next_id;
        return String.valueOf(id);
    }

    @JsonCreator
    public Link(
            @JsonProperty("id") String id,
            @JsonProperty("length") int length,
            @JsonProperty("free_flow_velocity") int free_flow_velocity,
            @JsonProperty("q") TransportQueue q
    ) {
        long numeric_id = 0;
        try {
            numeric_id = Long.parseLong(id);
        }
        catch (NumberFormatException e) {
            //ignore
        }
        if (numeric_id >= next_id) {
            next_id = numeric_id + 1;
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
        this.q = q;
        this.free_flow_capacity = this.length / Math.max(
                Constants.JAM_AGENT_LENGTH,
                Constants.FREE_FLOW_AGENT_LENGTH_PER_KPH * this.free_flow_velocity
        );
        this.jam_capacity = this.length / Constants.JAM_AGENT_LENGTH;
        this.free_flow_travel_time = this.length / this.free_flow_velocity;
        this.jam_travel_time = this.length / Constants.JAM_VELOCITY;
        next_id += 1;
    }

    public Link(int length, int free_flow_velocity) {
        this(getAutoId(), length, free_flow_velocity, new TransportQueue());
    }

    public Link(String id, int length, int free_flow_velocity) {
        this(id, length, free_flow_velocity, new TransportQueue());
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
            throw new LinkException("no agent waiting on link");
        }
        Agent popped = this.q.poll();
        if (popped != to_be_removed) {
            throw new LinkException("this should not happen: peek is not first waiting on link");
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

    public String getId() {
        return this.id;
    }

    public int getFreeFlowVelocity() {
        return this.free_flow_velocity;
    }

    public int getLength() {
        return this.length;
    }

    public int getJamCapacity() {
        return this.jam_capacity;
    }

    public int getFreeFlowCapacity() {
        return this.free_flow_capacity;
    }

    public int getJamTravelTime() {
        return this.jam_travel_time;
    }

    public int getFreeFlowTravelTime() {
        return this.free_flow_travel_time;
    }
}
