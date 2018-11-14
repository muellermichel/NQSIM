package ch.ethz.systems.nqsim2;

public class Realm {

    // Identifier of the realm. Right now, we are supporting up to 256 reals.
    private final int id;
    // Array of links in this realm. This also includes outgoing links.
    private final Link[] links;
    // Array of incomming links.
    private final Link[] inLinks;
    // Array of outgoing links.
    private final Link[] outLinks;
    // Current timestamp
    private int time;

    public Realm(int id, Link[] links, Link[] inLinks, Link[] outLinks) {
        this.id = id;
        this.links = links;
        this.inLinks = inLinks;
        this.outLinks = outLinks;
    }

    protected int processLink(Link link) {
        int routed = 0;
        if (link.nexttime() > 0 && time >= link.nexttime()) {
            Agent agent = link.queue().peek();
            while (agent.linkFinishTime <= time) {
                Link nextHop = links[agent.plan[agent.planIndex]];
                if (nextHop.push(agent)) {
                    // TODO - need to update nextHop.nextTime
                    agent.insert(time, nextHop);
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
    public int tick(int delta) {
        int routed = 0;
        time += delta;

        // Process internal links.
        for (Link link : links) {
            routed += processLink(link);
        }

        // Send outgoing agents.
        for (Link link : outLinks) {
            // TODO - send array of agents that are ready to leave the link
        }

        // Receive incomming agents.
        for (Link link : inLinks) {
            // TODO - receive agents and try to route them locally.
            // TODO - send number of agents routed locally.
        }

        // Receive number of agents routed remotelly.
        for (Link link : outLinks) {
            // TODO - receive number of agents routed remotelly.
        }
        return routed;
    }

    public int time() {
        return this.time;
    }

    public int id() {
        return this.id;
    }

    public Link[] links() {
        return this.links;
    }

    public Link[] inLinks() {
        return this.inLinks;
    }
    
    public Link[] outLinks() {
        return this.outLinks;
    }
}