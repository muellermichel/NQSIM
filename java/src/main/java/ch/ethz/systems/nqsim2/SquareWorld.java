package ch.ethz.systems.nqsim2;

import java.util.Random;

public class SquareWorld extends World {

    // The world is a mesh of squares. Each edge is a link.
    public SquareWorld(int edgeSize, int numAgens, int linkCapcity, int numSteps) {
        super(new Link[edgeSize * edgeSize * 4], new Agent[numAgens], new Link[0], 2);
        Random rand = new Random();
        // Create links.
        for (int i = 0; i < links.length; i++) {
           links[i] = new Link(i, linkCapcity, false); // TODO - detect if links are internal or not!
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
           agents[i].insert(0, links[plan[0]].timeToPass());
       }
        // Setup links's nextTime.
        for (int i = 0; i < links.length; i++) {
           links[i].nextTime = links[i].queue.peek() == null ? 
                0 : links[i].queue.peek().linkFinishTime;
        }
    }
}