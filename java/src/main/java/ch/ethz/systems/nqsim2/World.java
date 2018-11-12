package ch.ethz.systems.nqsim2;

public class World {

    // Current timestamp
    protected int time;
    // Array of all links.
    protected Link[] links;
    // Array of all agents.
    protected Agent[] agents;
    // Number of parallel tasks processing links.
    protected int numTasks;
    // Array of boundary links.
    private Link[] boundaryLinks;

    class LinkProcessor extends Thread {

        private int linkStart;
        private int linkFinish;
        private int routed;

        public LinkProcessor(int linkStart, int linkFinish) {
            this.linkStart = linkStart;
            this.linkFinish = linkFinish;
            this.routed = 0;
        }

        @Override
        public void run() {
            for (int i = linkStart; i <= linkFinish; i++) {
                routed += processLink(links[i]);
            }
        }

        public int getRouted() {
            return this.routed;
        }

    }

    public World(Link[] links, Agent[] agents, Link[] boundaryLinks, int numTasks) {
        this.links = links;
        this.agents = agents;
        this.numTasks = numTasks;
        this.boundaryLinks = boundaryLinks;
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
    public int tick(int delta) throws Exception {
        LinkProcessor[] tasks = new LinkProcessor[numTasks];
        int routed = 0;
        int linksPerTask = links.length / numTasks;

        // Setup tasks.
        for (int i = 0; i < numTasks - 1; i++) {
           tasks[i] = new LinkProcessor(
               i       * linksPerTask, 
               (i + 1) * linksPerTask - 1);
        }
        tasks[numTasks - 1] = new LinkProcessor(
            (numTasks - 1) * linksPerTask, 
            links.length - 1);

        // Update time.
        time += delta;

        // Launch tasks.
        for (Thread task : tasks) {
            task.start();
        }

        // Join tasks and sum routed.
        for (LinkProcessor task : tasks) {
            task.join();
            routed += task.getRouted();
        }

        // Process boundary links.
        for (Link link : boundaryLinks) {
            routed += processLink(link);
        }

        return routed;
    }

}