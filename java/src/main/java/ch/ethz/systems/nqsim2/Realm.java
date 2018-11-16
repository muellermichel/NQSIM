package ch.ethz.systems.nqsim2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Realm implements Serializable {

    private static final long serialVersionUID = -4933703718837514089L;
    // Identifier of the realm.
    private final int id;
    // Array of links in this realm. This also includes outgoing links.
    private final LinkInternal[] links;
    // Array of incomming links.
    private final LinkBoundary[] inLinks;
    // Array of outgoing links.
    private final LinkBoundary[] outLinks;
    // Current timestamp
    private int time;

    public Realm(
            int id, LinkInternal[] links, LinkBoundary[] inLinks, 
            LinkBoundary[] outLinks) throws Exception {
        this.id = id;
        this.links = links;
        this.inLinks = inLinks;
        this.outLinks = outLinks;
    }

    protected boolean processAgent(Agent agent) {
        LinkInternal nextHop = links[agent.plan[agent.planIndex]];
        if (nextHop.push(time, agent)) {
            agent.planIndex++;
            return true;
        } else {
            return false;
        }

    }

    protected int processLink(LinkInternal link) {
        int routed = 0;
        if (link.nexttime() > 0 && time >= link.nexttime()) {
            Agent agent = link.queue().peek();
            while (agent.linkFinishTime <= time) {
                if (agent.planIndex == agent.plan.length || processAgent(agent)) {
                    link.pop();
                    routed++;
                    if ((agent = link.queue().peek()) == null) {
                        break;
                    }
                } else {
                    break;
                }
            }
            link.nexttime(agent == null ? 0 : agent.linkFinishTime);
        }
        return routed;
    }

    // Updates all links and agents. Returns the number of routed agents.
    public int tick(int delta, Communicator comm) throws Exception {
        int routed = 0;
        Map<Integer, Integer> routedAgentsByLinkId = new HashMap<>();
        Map<LinkBoundary, ArrayList<Agent>> outAgentsByBoundary = new HashMap<>();
        Map<Integer, ArrayList<Agent>> inAgentsByLinkId = new HashMap<>();
        time += delta;

        // Process internal links.
        for (LinkInternal link : links) {
            // TODO - we should not process outgoing links!
            routed += processLink(link);
        }

        // Send outgoing agents.
        for (LinkBoundary blink : outLinks) {
            LinkInternal ilink = links[blink.id()];
            ArrayList<Agent> outgoing = new ArrayList<>();
            for (Agent agent : ilink.queue()) {
                System.out.println(String.format("#####Outgoing time %d agent %d finishtime %d", time, agent.id, agent.linkFinishTime));
                if (agent.linkFinishTime > time) {
                    break;
                } else {
                    System.out.println("#####Adding agent " + agent.id);
                    outgoing.add(agent);
                }
            }
            outAgentsByBoundary.put(blink, outgoing);
        }
        comm.sendAgents(outAgentsByBoundary);

        // Receive incomming agents.
        inAgentsByLinkId = comm.receiveAgents();
        for (Map.Entry<Integer, ArrayList<Agent>> entry : inAgentsByLinkId.entrySet()) {
            int localrouted = 0;
            for (Agent agent : entry.getValue()) {
                if (processAgent(agent)) {
                    localrouted++;
                    routed++;
                } else {
                    break;
                }
            }
            routedAgentsByLinkId.put(entry.getKey(), localrouted);
        }

        // Send locally rounted agents counters.
        comm.sendRoutedCounters(routedAgentsByLinkId);

        // Receive number of agents routed remotelly.
        routedAgentsByLinkId = comm.receiveRoutedCounters();
        for (Integer linkid : routedAgentsByLinkId.keySet()) {
            int counter = routedAgentsByLinkId.get(linkid);
            for (int i = 0; i < counter; i++) {
                links[linkid].pop();
            }
        }

        return routed;
    }

    public int time() {
        return this.time;
    }

    public int id() {
        return this.id;
    }

    public LinkInternal[] links() {
        return this.links;
    }

    public LinkBoundary[] inLinks() {
        return this.inLinks;
    }
    
    public LinkBoundary[] outLinks() {
        return this.outLinks;
    }
}