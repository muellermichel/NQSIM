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
    private int assigned_rank;
    private int assigned_node_index;
    private byte assigned_incoming_link_idx;
    private int current_capacity;
    public int sleep_till = -1;
    private Queue<Agent> q;

    private static long next_id = 0;

    private static String getAutoId() {
        long id = next_id;
        return String.valueOf(id);
    }

    public static void resetAutoIds() {
        next_id = 0;
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
        this.assigned_rank = 0;
        this.assigned_node_index = -1;
        this.current_capacity = -1;
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

    public boolean isAccepting() throws LinkException {
        return this.availableCapacity() > 0;
    }

    public boolean isEmpty() throws LinkException {
        return this.availableCapacity() == this.jam_capacity;
    }

    public int availableCapacity() throws LinkException {
        if (this.current_capacity == -1) {
            throw new LinkException("capacity not yet computed for " + this.getId());
        }
        return this.current_capacity;
    }

    public void computeCapacity() {
        this.current_capacity = this.jam_capacity - this.q.size();
    }

    public void setCommunicatedCapacity(int capacity) {
        this.current_capacity = capacity;
    }

    public void finalizeTimestep() {
        this.current_capacity = -1;
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

    public void add(Agent agent, int t) throws LinkException {
        agent.link_start_time = t;
        if (this.q.size() < this.free_flow_capacity) {
            agent.time_to_pass_link = t + this.free_flow_travel_time;
        }
        else if (this.q.size() < this.jam_capacity) {
            agent.time_to_pass_link = t + this.jam_travel_time;
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
        popped.link_start_time = -1;
        return popped;
    }

    public int queueLength() {
        return this.q.size();
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

    public void setAssignedRank(int rank) {
        this.assigned_rank = rank;
    }

    public int getAssignedRank() {
        return this.assigned_rank;
    }

    public void setAssignedIncomingLinkIdx(byte idx) {
        this.assigned_incoming_link_idx = idx;
    }

    public byte getAssignedIncomingLinkIdx() {
        return this.assigned_incoming_link_idx;
    }

    public void setAssignedNodeIndex(int idx) {
        this.assigned_node_index = idx;
    }

    public int getAssignedNodeIndex() throws LinkException {
        if (this.assigned_node_index < 0) {
            throw new LinkException("node index not yet assigned");
        }
        return this.assigned_node_index;
    }
}
