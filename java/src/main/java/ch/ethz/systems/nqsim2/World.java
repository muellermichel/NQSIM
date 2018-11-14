package ch.ethz.systems.nqsim2;

public class World {

    // Current timestamp.
    private int time;
    // Reamls that compose this World.
    private final Realm[] realms;
    // Agents that circulate within the World.
    private final Agent[] agents;

    public World(Realm[] realms, Agent[] agents) {
        this.realms = realms;
        this.agents = agents;
    }
    
    // Updates all links and agents. Returns the number of routed agents.
    public int tick(int delta) {
        int routed = 0;
        time += delta;

        // Process all realms. // TODO - process realms in parallel!
        for (Realm realm : realms) {
            routed += realm.tick(delta);
        }

        return routed;
    }

    public int time() {
        return this.time;
    }

    public Realm realm(int id) {
        return realms[id];
    }

    public Realm[] realms() {
        return realms;
    }

    public Agent[] agents() {
        return agents;
    }
}