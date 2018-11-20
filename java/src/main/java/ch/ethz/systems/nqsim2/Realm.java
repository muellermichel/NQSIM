package ch.ethz.systems.nqsim2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Realm implements Serializable {

    private static final long serialVersionUID = -4933703718837514089L;
    // Identifier of the realm.
    private final int id;
    // Array of links onwer by this realm. 
    // Note 1: that outgoing are onwer by the source realm. 
    // Note 2: the id of the link is its index in the array.
    private final LinkInternal[] links;
    // Array of internal links onwer by this realm. Does not include outgoing
    // links owned by this realm.
    private final LinkInternal[] internalLinks;
    // A LinkBoundary is either an incomming or outgoing link. Each boundary
    // link contains the id of the link in the source realm. These are used to
    // regulate the communication between realms.
    private final LinkBoundary[] inLinks;
    private final LinkBoundary[] outLinks;
    // Current timestamp
    private int time;

    public Realm(int id, LinkInternal[] links, LinkBoundary[] inLinks, 
            LinkBoundary[] outLinks) throws Exception {
        this.id = id;
        this.links = links;
        this.inLinks = inLinks;
        this.outLinks = outLinks;
        this.internalLinks = setupInternalLinks();
    }

    private LinkInternal[] setupInternalLinks() {
        Set<Integer> outLinkIds = new HashSet<>(outLinks.length);
        LinkInternal[] internalLinks = new LinkInternal[links.length - outLinks.length];
        int idx = 0;

        for (LinkBoundary lb: outLinks) {
            outLinkIds.add(lb.id());
        }

        for (int i = 0; i < links.length; i++) {
            if (!outLinkIds.contains(i)) {
                internalLinks[idx++] = links[i];
            }
        }

        return internalLinks;
    }

    protected boolean processAgent(Agent agent) {
        LinkInternal next = links[agent.plan[agent.planIndex + 1]];
        if (next.push(time, agent)) {
            agent.planIndex++;
            assert(WorldSimulator.log(time, id, String.format("-> %d agent %d", 
                WorldSimulator.globalIdByLink.get(next), agent.id)));
            return true;
        } else {
            return false;
        }

    }

    protected int processInternalLinks(LinkInternal link) {
        int routed = 0;
        if (link.nexttime() > 0 && time >= link.nexttime()) {
            Agent agent = link.queue().peek();
            while (agent.linkFinishTime <= time) {
                if (agent.planIndex == (agent.plan.length - 1) || processAgent(agent)) {
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
        for (LinkInternal link : internalLinks) {
            routed += processInternalLinks(link);
        }

        // Send outgoing agents.
        for (LinkBoundary blink : outLinks) {
            LinkInternal ilink = links[blink.id()];
            ArrayList<Agent> outgoing = new ArrayList<>();
            for (Agent agent : ilink.queue()) {
                if (agent.linkFinishTime > time) {
                    break;
                } else {
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
                if (agent.planIndex == (agent.plan.length - 1) || processAgent(agent)) {
                    localrouted++;
                    routed++;
                } else {
                    break;
                }
            }
            routedAgentsByLinkId.put(entry.getKey(), localrouted);
        }

        comm.waitSends();

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

        comm.waitSends();

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