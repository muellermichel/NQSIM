package ch.ethz.systems.nqsim2;

import mpi.MPI;

public class WorldSimulator {

    public static void log(int realmid, String s) {
        System.out.println(String.format("[%d] %s", realmid, s));
    }

    public static void main(String[] args) throws Exception {
        String worldpath = args[0];
        int timestep = Integer.parseInt(args[1]);
        int numsteps = Integer.parseInt(args[2]);

        Communicator comm;
        Realm realm;
        Agent[] agents;

        MPI.Init(new String[0]);
        int realmid = MPI.COMM_WORLD.getRank();

        System.out.println("Deerializing world to " + worldpath + ".ser");
        World world = World.deserialize(worldpath + ".ser");
        System.out.println("Finished deserializing world.");

        realm = world.realm(realmid);
        agents = world.agents();
        comm = new Communicator(realm, agents);
        world = null;

        System.out.println("Running world");
        for (int i = 0; i < numsteps; i++) {
            String dump = String.format("%s-%d", worldpath, i);
            System.out.println("Dumping realm to " + dump);
            WorldDumper.dumpRealm(dump, realm);
            System.out.println("Finished dumping realm.");

            long time = System.currentTimeMillis();
            int routed = realm.tick(timestep, comm);
            time = 1 + System.currentTimeMillis() - time; 
            log(realmid, 
                String.format("Time = %d routed %d agents in %d ms ( %d s/r)",
                    i*timestep, routed, time, timestep*1000/time));
        }
        System.out.println("Running world finished");
        MPI.Finalize();
    }
}