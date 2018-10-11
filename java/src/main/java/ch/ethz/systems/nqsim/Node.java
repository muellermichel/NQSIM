package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import mpi.MPIException;

import java.util.*;

public final class Node {
    private List<Link> incoming_links;
    private List<Link> outgoing_links;
    private long routed;
    private int assigned_rank;

    public static List<String> stringsFromLinks(List<Link> links, LinkToStringOperator operator) {
        List<String> result = new LinkedList<>();
        for (Link link : links) {
            result.add(operator.stringOp(link));
        }
        return result;
    }

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

    public String toString() {
        return String.format(
            "[%s]->[%s]",
            String.join(",", stringsFromLinks(this.incoming_links, link -> link.getId())),
            String.join(",", stringsFromLinks(this.outgoing_links, link -> link.getId()))
        );
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

    public byte route_agent(Agent agent, int node_index, Communicator communicator) throws NodeException, MPIException {
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
        agent.pollPlan();
        if (communicator != null) {
            int my_rank = communicator.getMyRank();
            if (next_link.getAssignedRank() != my_rank) {
                int global_idx = -2;
                int assigned_rank = -1;
                int assigned_node_index = -1;
                try {
                    global_idx = next_link.getAssignedNodeIndex(); //only our local nodes have local indices set - others have global ones.
                    assigned_rank = next_link.getAssignedRank();
                    assigned_node_index = next_link.getAssignedNodeIndex();
                }
                catch (LinkException e) {
                    throw new NodeException(e.getMessage());
                }
                if (global_idx < 0) {
                    throw new NodeException(String.format(
                        "link (out idx %d)'s global index could not be resolved from local index %d on rank %d: %d",
                        next_link_idx,
                        assigned_node_index,
                        assigned_rank,
                        global_idx
                    ));
                }
                if (agent.getId().equals("3495")) {
                    System.out.println("preparing 3495 to send to " + global_idx + "(Link " + next_link.getId() + "). plan:" + agent.getPlan().toString());
                }
                communicator.prepareAgentForTransmission(agent, assigned_rank, global_idx);
                return next_link_idx;
            }
        }
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
                    byte next_link_idx = this.route_agent(current_agent, node_index, communicator);
                    if (next_link_idx == -2) {
                        break;
                    }
                    if (current_agent.getId().equals("3355") || this.getOutgoingLink(next_link_idx).getAssignedRank() != communicator.getMyRank()) {
                        System.out.println(String.format(
                            "node %d: agent %s%s(tt:%d,lt:%d) has crossed over from link %d(%s) to %d(%s) (rank %d -> %d)",
                            node_index,
                            current_agent.getId(),
                            current_agent.getPlan().toString(),
                            current_agent.current_travel_time,
                            current_agent.time_to_pass_link,
                            link_idx,
                            link.getId(),
                            next_link_idx,
                            (next_link_idx >= 0) ? this.getOutgoingLink(next_link_idx).getId() : "none",
                            communicator.getMyRank(),
                            this.getOutgoingLink(next_link_idx).getAssignedRank()
                        ));
                    }
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
                        (current_agent != null) ? current_agent.getId() : "none",
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
