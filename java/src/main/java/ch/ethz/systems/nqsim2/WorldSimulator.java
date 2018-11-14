package ch.ethz.systems.nqsim2;

public class WorldSimulator {

    public static void main(String[] args) throws Exception {
        System.out.println("Populating world");
        World world = new SquareWorldGenerator(512, 10000000, 3600/60).generateWorld(1);
        System.out.println("Populating world finished");
        System.out.println("Running world");
        for (int i = 0; i < 3600/60; i++) {
            long time = System.currentTimeMillis();
            int routed = world.tick(60);
            time = System.currentTimeMillis() - time; 
            System.out.println(
                "Time = " + world.time() + ": " +
                "routed " + routed + " agents in " +
                time + "ms " +
                "(" + 60*1000/time + " s/r)");
        }
        System.out.println("Running world finished");
    }
}