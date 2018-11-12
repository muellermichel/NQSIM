package ch.ethz.systems.nqsim2;

public class World {

    // Current timestamp
    protected int time;
    private Realm[] realms;

    public World(Realm[] realms) {
        this.realms = realms;
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
}