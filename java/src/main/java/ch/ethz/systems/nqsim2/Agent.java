package ch.ethz.systems.nqsim2;

public class Agent {
    // Id of the link (index for World.agents).
    protected final int id;
    // Timestamp of when the agent will be ready to exit link.
    protected int linkFinishTime;
    // Time that the agent takes to traverse a link;
    protected final int timeToPass; 
    // Array of link ids where the agent will go.
    protected final int[] plan;
    // Current position in plan.
    protected int planIndex;

    public Agent(int id, Link start, int[] plan) {
        this.id = id;
        this.timeToPass = 1; // TODO - check default values.
        this.plan = plan;
        this.linkFinishTime = this.timeToPass + start.timeToPass();
    }

    public void insert(int time, Link nextHop) {
        planIndex++;
        linkFinishTime = time + this.timeToPass + nextHop.timeToPass();
    }

    public int id() {
        return this.id;
    }

    public int linkFinishTime() {
        return this.linkFinishTime;
    }

    public int timeToPass() {
        return this.timeToPass;
    }

    public int planIndex() {
        return this.planIndex;
    }

    public int[] plan() {
        return this.plan;
    }

}