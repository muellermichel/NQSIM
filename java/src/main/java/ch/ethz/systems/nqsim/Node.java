package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import mpi.MPIException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class Node {
    private List<Link> incoming_links;
    private List<Link> outgoing_links;
    private long routed;
    private int assigned_rank;

    @JsonCreator
    public Node(@JsonProperty("incoming_links") List<Link> incoming_links) {
        this(incoming_links, new ArrayList<>());
    }

    public Node() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public Node(List<Link> incoming_links, List<Link> outgoing_links) {
        this.incoming_links = incoming_links;
        this.outgoing_links = outgoing_links;
        this.routed = 0;
        this.assigned_rank = 0;
    }

    public void addIncomingLink(Link link) {
        this.incoming_links.add(link);
    }

    public void addOutgoingLink(Link link) {
        this.outgoing_links.add(link);
    }

    public void addAgent(Agent agent, byte link_index) throws NodeException {
        try {
            this.incoming_links.get(link_index).add(agent);
        }
        catch (LinkException e) {
            throw new NodeException(String.format(
                    "cannot add agent %s to incoming link %d - invalid link for this node.",
                    agent.getId(),
                    link_index
            ));
        }
        if (agent.peekPlan() == link_index) {
            agent.pollPlan();
        }
    }

    public List<Link> getIncomingLinks() {
        return this.incoming_links;
    }

    public List<Link> getOutgoingLinks() {
        return this.outgoing_links;
    }

    public long getRouted() { return this.routed; }

    public Link getIncomingLink(byte link_index) throws NodeException {
        try {
            return this.incoming_links.get(link_index);
        }
        catch (IndexOutOfBoundsException e) {
            throw new NodeException(String.format(
                    "invalid incoming link index for this node: %d",
                    link_index
            ));
        }
    }

    public Link getOutgoingLink(byte link_index) throws NodeException {
        try {
            return this.outgoing_links.get(link_index);
        }
        catch (IndexOutOfBoundsException e) {
            throw new NodeException(String.format(
                    "invalid outgoing link index for this node: %d",
                    link_index
            ));
        }
    }

    public int incomingQueueLength(byte link_index) throws NodeException {
        return this.getIncomingLink(link_index).queueLength();
    }

    public int outgoingQueueLength(byte link_index) throws NodeException {
        return this.getOutgoingLink(link_index).queueLength();
    }

    public void tick(int delta_t) {
        for (Link link:this.incoming_links) {
            link.tick(delta_t);
        }
    }

    public int route_agent(Agent agent, int node_index, Communicator communicator) throws NodeException, MPIException {
        byte next_link_idx = agent.peekPlan();
        if (next_link_idx == -1) {
            return -1;
        }
        Link next_link = this.getOutgoingLink(next_link_idx);
        try {
            if (!next_link.isAccepting()) {
                return -2;
            }
        } catch (LinkException e) {
            throw new NodeException(String.format(
                    "link %d: %s",
                    next_link_idx,
                    e.getMessage()
            ));
        }
        if (communicator != null) {
            int my_rank = communicator.getMyRank();
            if (next_link.getAssignedRank() != my_rank) {
                communicator.prepareAgentForTransmission(
                        agent,
                        next_link.getAssignedRank(),
                        communicator.getGlobalNodeIdxFromLocalIdx(node_index, my_rank)
                );
            }
        }
        agent.pollPlan();
        try {
            next_link.add(agent);
        } catch (LinkException e) {
            throw new NodeException(String.format(
                    "link %d: %s",
                    next_link_idx,
                    e.getMessage()
            ));
        }
        return next_link_idx;
    }

    public void route(int node_index, Communicator communicator) throws NodeException, MPIException {
        ListIterator<Link> incoming_link_iterator = this.incoming_links.listIterator();
        while (incoming_link_iterator.hasNext()) {
            int link_idx = incoming_link_iterator.nextIndex();
            Link link = incoming_link_iterator.next();
            Agent current_agent = link.peek();
            try {
                while (current_agent != null && current_agent.current_travel_time >= current_agent.time_to_pass_link) {
                    int next_link_idx = this.route_agent(current_agent, node_index, communicator);
                    if (next_link_idx == -2) {
                        break;
                    }
                    System.err.println(String.format(
                        "node %d: agent %s has crossed over from link %d(%s) to %d",
                        node_index,
                        current_agent.getId(),
                        link_idx,
                        link.getId(),
                        next_link_idx
                    ));
                    try {
                        link.removeFirstWaiting();
                    }
                    catch (LinkException e) {
                        throw new NodeException(String.format(
                                "link %d: %s",
                                link_idx,
                                e.getMessage()
                        ));
                    }
                    this.routed += 1;
                    current_agent = link.peek();
                }
            }
            catch (NodeException e) {
                throw new NodeException(String.format(
                        "agent %s: %s",
                        current_agent.getId(),
                        e.getMessage()
                ));
            }
        }
    }

    public void computeCapacities() {
        for (Link link : this.incoming_links) {
            link.computeCapacity();
        }
    }

    public void finalizeTimestep() {
        for (Link link : this.incoming_links) {
            link.finalizeTimestep();
        }
    }

    public int getAssignedRank() {
        return this.assigned_rank;
    }

    public void setAssignedRank(int rank) {
        this.assigned_rank = rank;
        for (Link link : this.incoming_links) {
            link.setAssignedRank(rank);
        }
    }
}
