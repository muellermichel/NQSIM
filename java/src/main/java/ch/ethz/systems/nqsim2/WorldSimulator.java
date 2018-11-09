package ch.ethz.systems.nqsim2;

public class WorldSimulator {

    public static void main(String[] args) throws Exception {
        System.out.println("Populating world");
        World world = new SquareWorld(256, 5000000, 256, 3600/60);
        System.out.println("Populating world finished");
        System.out.println("Running world");
        for (int i = 0; i < 3600/60; i++) {
            long time = System.currentTimeMillis();
            int routed = world.tick(60);
            time = System.currentTimeMillis() - time; 
            System.out.println(
                "Time = " + time + ": " +
                "routed " + routed + " agents in " +
                time + "ms " +
                "(" + 60*1000/time + " s/r)");
        }
        System.out.println("Running world finished");
    }
}