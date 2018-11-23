package ch.ethz.systems.nqsim2;

import java.io.Serializable;

public class Realm implements Serializable {

    private static final long serialVersionUID = -4933703718837514089L;
    // Identifier of the realm.
    private final int id;
    // Array of links onwer by this realm. 
    // Note 1: that outgoing are onwer by the source realm. 
    // Note 2: the id of the link is its index in the array.
    private final LinkInternal[] links;
    // A LinkBoundary is either an incomming or outgoing link. Each boundary
    // link contains the id of the link in the source realm. These are used to
    // regulate the communication between realms.
    private final LinkInternal[] inLinks;
    // Current timestamp
    private int secs;
    private int routed;

    public Realm(int id, LinkInternal[] links, LinkInternal[] inLinks) throws Exception {
        this.id = id;
        this.links = links;
        this.inLinks = inLinks;
    }

    protected boolean processAgent(Agent agent) {
        LinkInternal next = links[agent.plan[agent.planIndex + 1]];
        if (next.push(secs, agent)) {
            agent.planIndex++;
            assert(WorldSimulator.log(secs, id, String.format("-> %d agent %d", 
                WorldSimulator.globalIdByLink.get(next), agent.id)));
            return true;
        } else {
            return false;
        }
    }

    protected void processInternalLinks(LinkInternal link) {
        if (link.nexttime() > 0 && secs >= link.nexttime()) {
            Agent agent = link.queue().peek();
            while (agent.linkFinishTime <= secs) {
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
    }

    // Updates all links and agents. Returns the number of routed agents.
    public int tick(int delta) throws Exception {
        long start, frouting, fcomm;
        routed = 0;
        secs += delta;
        start = System.currentTimeMillis();

        // Process internal links.
        for (LinkInternal link : links) {
            processInternalLinks(link);
        }

        WorldSimulator.barrier(secs, id);
        frouting = System.currentTimeMillis();

        // Process incomming links.
        for (LinkInternal link : inLinks) {
            processInternalLinks(link);
        }

        WorldSimulator.barrier(secs, id);
        fcomm = System.currentTimeMillis();

        WorldSimulator.log(secs, id, String.format(
                "Processed %d agents in %d ms (routing = %d ms; comm = %d ms)",
                routed,
                fcomm - start,
                frouting - start,
                fcomm - frouting));
        return routed;
    }

    public int time() {
        return this.secs;
    }

    public int id() {
        return this.id;
    }

    public LinkInternal[] links() {
        return this.links;
    }

    public LinkInternal[] inLinks() {
        return this.inLinks;
    }
    
}