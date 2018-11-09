package ch.ethz.systems.nqsim2;

public class World {

    // Current timestamp
    protected int time;
    // Array of all links.
    protected Link[] links;
    // Array of all agents.
    protected Agent[] agents;

    public World(Link[] links, Agent[] agents) {
        this.links = links;
        this.agents = agents;
    }

    // Updates all links and agents. Returns the number of routed agents.
    public int tick(int delta) {
        time += delta;
        int routed = 0;
        for (Link link : links) {
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
        }
        return routed;
    }

}