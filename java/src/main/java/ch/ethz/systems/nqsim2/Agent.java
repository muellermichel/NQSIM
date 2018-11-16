package ch.ethz.systems.nqsim2;

import java.io.Serializable;

public class Agent implements Serializable {
    private static final long serialVersionUID = -6805724658054818824L;
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

    public Agent(int id, int[] plan) {
        this.id = id;
        this.timeToPass = 1; // TODO - check default values.
        this.plan = plan;
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

    public void planIndex(int index) {
        this.planIndex = index;
    }

    public int[] plan() {
        return this.plan;
    }

}