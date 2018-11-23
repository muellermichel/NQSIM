package ch.ethz.systems.nqsim2;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class WorldSimulator {

    // Logging and Debugging information
    private static PrintWriter[] logfiles;
    private static String worldpath;
    private static int numrealms;
    private static int numsteps;
    private static int timestep; 
    public static Map<LinkInternal, Integer> globalIdByLink;
    private static World world;
    private static CyclicBarrier barrier;

    static class RealmRunner implements Runnable {

        private Realm realm;

        public RealmRunner(Realm realm) {
            this.realm = realm;
        }

        @Override
        public void run() {
            for (int j = 0; j < numsteps; j++) {
                try {
                    realm.tick(timestep);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void barrier(int time, int realmid) throws Exception {
        barrier.await();
    }

    public static boolean log(int time, int realmid, String s) {
        logfiles[realmid].write(String.format("[ time = %d ] %s\n", time, s));
        return true;
    }

    public static void main(String[] args) throws Exception {
        worldpath = args[0];
        numrealms = Integer.parseInt(args[1]);
        timestep = Integer.parseInt(args[2]);
        numsteps = Integer.parseInt(args[3]);
        barrier = new CyclicBarrier(numrealms);

        // Deserializing the world.
        System.out.println("Deerializing world to " + worldpath + ".ser");
        world = World.deserialize(worldpath + ".ser");
        globalIdByLink = world.globalIdByLink();
        System.out.println("Finished deserializing world.");

        // Preparing log files.
        logfiles = new PrintWriter[world.realms().length];
        for (int i = 0; i < logfiles.length; i++) {
            logfiles[i] = new PrintWriter(
                new FileWriter(
                    String.format("%s-realm-%d.log", worldpath, i)));
        }

        System.out.println("Running world");
        // Create workers.
        List<Thread> workers = new ArrayList<>(numrealms);
        for (int i = 0; i < numrealms; i++) {
            Thread worker = new Thread(new RealmRunner(world.realm(i)));
            workers.add(i, worker);
        }
        // Launch workers.
        for (Thread worker : workers) {
            worker.start();
        }
        // Joining workers.
        for (Thread worker : workers) {
            worker.join();
        }
        System.out.println("Running world finished");

        // Closing all log files.
        for (int i = 0; i < logfiles.length; i++) {
            logfiles[i].close();
        }
    }
}