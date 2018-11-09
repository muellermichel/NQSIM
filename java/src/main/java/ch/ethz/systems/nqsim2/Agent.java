package ch.ethz.systems.nqsim2;

public class Agent {
    // Id of the link (index for World.agents).
    protected int id;
    // Timestamp of when the agent will be ready to exit link.
    protected int linkFinishTime;
    // Time that the agent takes to traverse a link;
    protected int timeToPass; // TODO - check default values.
    // Array of link ids where the agent will go.
    protected int[] plan;
    // Current position in plan.
    protected int planIndex;

    public Agent(int id, int[] plan) {
        this.id = id;
        this.timeToPass = 1;
        this.plan = plan;
    }

    public void insert(int time, int linkTimeToPass) {
        planIndex++;
        linkFinishTime = time + this.timeToPass + linkTimeToPass;
    }

}