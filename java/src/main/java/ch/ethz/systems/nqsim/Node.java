package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public final class Node {
    private List<Link> incoming_links;
    private List<Link> outgoing_links;

    @JsonCreator
    public Node(@JsonProperty("incoming_links") List<Link> incoming_links) {
        this.incoming_links = incoming_links;
    }

    public Node() {
        this.incoming_links = new ArrayList<Link>();
        this.outgoing_links = new ArrayList<Link>();
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
                    "invalid incoming link index for this node: %d",
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

    public void route(int node_index) throws NodeException {
        ListIterator<Link> incoming_link_iterator = this.incoming_links.listIterator();
        while (incoming_link_iterator.hasNext()) {
            int link_idx = incoming_link_iterator.nextIndex();
            Link link = incoming_link_iterator.next();
            Agent current_agent = link.peek();
            while (current_agent != null && current_agent.current_travel_time >= current_agent.time_to_pass_link) {
                byte next_link_idx = current_agent.peekPlan();
                if (next_link_idx != -1) {
                    Link next_link = this.getOutgoingLink(next_link_idx);
                    if (!next_link.isAccepting()) {
                        break;
                    }
                    current_agent.pollPlan();
                    try {
                        next_link.add(current_agent);
                    }
                    catch (LinkException e) {
                        throw new NodeException(String.format(
                                "link %d: %s",
                                link_idx,
                                e.getMessage()
                        ));
                    }
                }
                System.err.println(String.format(
                    "node %d: agent %s is crossing over from link %d to %d",
                    node_index,
                    current_agent.getId(),
                    link_idx,
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
                current_agent = link.peek();
            }
        }
    }
}
