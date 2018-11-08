//package ch.ethz.systems.nqsim.engine;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class World {

    // Current timestamp
    private int time;
    // Array of all links.
    private Link[] links;
    // Array of all agents.
    private Agent[] agents;

    class Link {
        // Id of the link (index for World.links).
        private int id;
        // Timestamp of the next agent to arrive.
        private int nextTime;
        // Queue of agents on this link.
        private Queue<Agent> queue;
        // Maximum number of agents on the link to be considered free.
        private int free_capacity; // TODO - check default values.
        // Time it takes to pass the link under free capacity.
        private int free_time; // TODO - check default values.
        // Time it takes to pass the link under jam capacity.
        private int jam_time; // TODO - check default values.
        // Basically queue.capacity - queue.size
        private int currentCapacity;

        public Link(int id, int capacity) {
            this.id = id;
            this.queue = new ArrayDeque<>(capacity);
            this.free_capacity = new Float(capacity * 0.8f).intValue();
            this.free_time = 30;
            this.jam_time = 60;
            this.currentCapacity = capacity;
        }

        private int timeToPass() {
            if (currentCapacity == 0) {
                return 0;
            } 
            else if (currentCapacity > free_capacity) {
                return free_time;
            } else {
                return jam_time;
            }
        }

        public boolean push(Agent agent) {
            if (currentCapacity > 0) { 
                queue.add(agent);
                currentCapacity--;
                return true;
            } else {
                return false;
            }
        }

        public void pop() {
            queue.poll();
            currentCapacity++;
        }

    }

    class Agent {
        // Id of the link (index for World.agents).
        private int id;
        // Timestamp of when the agent will be ready to exit link.
        private int linkFinishTime;
        // Time that the agent takes to traverse a link;
        private int timeToPass; // TODO - check default values.
        // Array of link ids where the agent will go.
        private int[] plan;
        // Current position in plan.
        private int planIndex;

        public Agent(int id, int[] plan) {
            this.id = id;
            this.timeToPass = 1;
            this.plan = plan;
        }

        public void insert(Link link) {
            planIndex++;
            linkFinishTime = time + this.timeToPass + link.timeToPass();
        }

    }

    // The world is a mesh of squares. Each edge is a link.
    public World(int edgeSize, int numAgens, int linkCapcity, int numSteps) {
        Random rand = new Random();
        links = new Link[edgeSize * edgeSize * 4];
        agents = new Agent[numAgens];
        // Create links.
        for (int i = 0; i < links.length; i++) {
           links[i] = new Link(i, linkCapcity);
        }
        // Create agents.
         for (int i = 0; i < agents.length; i++) {
           int[] plan = new int[numSteps];
           plan[0] = rand.nextInt(edgeSize * edgeSize);
           for (int j = 1; j < numSteps; j++) {
               int direction = rand.nextInt(4);
               switch (direction) {
                   // up
                   case 0: plan[j] = (plan[j - 1] - edgeSize) % (edgeSize * edgeSize);
                   // down
                   case 1: plan[j] = (plan[j - 1] + edgeSize) % (edgeSize * edgeSize);
                   // left
                   case 2: plan[j] = (plan[j - 1] - 1) % (edgeSize * edgeSize);
                   // right
                   case 3: plan[j] = (plan[j - 1] + 1) % (edgeSize * edgeSize);
               }
           }
           agents[i] = new Agent(i, plan);
           if (!links[plan[0]].push(agents[i])) {
               i--;
               continue;
           }
           agents[i].insert(links[plan[0]]);
       }
        // Setup links's nextTime.
        for (int i = 0; i < links.length; i++) {
           links[i].nextTime = links[i].queue.peek() == null ? 
                0 : links[i].queue.peek().linkFinishTime;
        }

    }

    // Updates all links and agents. Returns the number of routed agents.
    public int tick() {
        int routed = 0;
        for (Link link : links) {
            if (link.nextTime > 0 && time >= link.nextTime) {
                Agent agent = link.queue.peek();
                while (agent.linkFinishTime <= time) {
                   Link nextHop = links[agent.plan[agent.planIndex]];
                   if (nextHop.push(agent)) {
                        agent.insert(nextHop);
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

    public static void main(String[] args) throws Exception {
        System.out.println("Populating world");
        World world = new World(256, 5000000, 256, 3600/60);
        System.out.println("Populating world finished");
        System.out.println("Running world");
        for (world.time = 60; world.time < 3600; world.time += 60) {
            long time = System.currentTimeMillis();
            int routed = world.tick();
            time = System.currentTimeMillis() - time; 
            System.out.println(
                "Time = " + world.time + ": " +
                "routed " + routed + " agents in " +
                time + "ms " +
                "(" + 60*1000/time + " s/r)");
        }
        System.out.println("Running world finished");
    }

}