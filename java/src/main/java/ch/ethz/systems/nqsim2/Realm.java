package ch.ethz.systems.nqsim2;

public class Realm {

    // Current timestamp
    protected int time;
    // Array of all links. Links that do not belong to this World will be null.
    protected Link[] links;
    // Array of incomming links.
    private Link[] inLinks;

    public Realm(Link[] links, Link[] inLinks) {
        this.links = links;
        this.inLinks = inLinks;
    }

    protected int processLink(Link link) {
        int routed = 0;
        if (link.nextTime > 0 && time >= link.nextTime) {
            Agent agent = link.queue.peek();
            while (agent.linkFinishTime <= time) {
                Link nextHop = links[agent.plan[agent.planIndex]];
                if (nextHop.push(agent)) {
                    agent.insert(time, nextHop.timeToPass());
                    link.pop();
                    routed++;
                    if ((agent = link.queue.peek()) == null) {
                        break;
                    }
                } else {
                    break;
                }
            }
            link.nextTime = agent == null ? 0 : agent.linkFinishTime;
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
        // Process incomming links.
        for (Link link : inLinks) {
            routed += processLink(link);
        }

        return routed;
    }

}