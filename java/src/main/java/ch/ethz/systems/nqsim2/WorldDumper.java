package ch.ethz.systems.nqsim2;

public class WorldDumper {

    private static final int edgeSize = 2;
    private static final int numAgents = 10;
    private static final int numSteps = 2;
    private static final int numRealms = 2;

    public static void main(String[] args) {
        System.out.println("Populating world");
        World world = new SquareWorldGenerator(edgeSize, numAgents, numSteps).generateWorld(numRealms);
        System.out.println("Populating world finished");
        dumpWorld(world);
    }

    private static void dumpWorld(World world) {
        System.out.println("<world> time=" + world.time());
        for (Realm realm : world.realms()) {
            dumpRealm(realm);
        }
        for (Agent agent : world.agents()) {
            dumpAgent(agent);
        }
        System.out.println("</world>");
    }

    private static void dumpRealm(Realm realm) {
        System.out.println(String.format("\t<realm> time=%d id=%d",
            realm.time(), realm.id()));
        System.out.println("\t\t<links>");
        for (Link link : realm.links()) {
            dumpLink(link);
        }
        System.out.println("\t\t</links>");
        System.out.println("\t\t<inlinks>");
        for (Link link : realm.inLinks()) {
            System.out.println(String.format("\t\t\t<link> id=%d from=%d to=%d",
               link.id(), link.fromrealm(), link.torealm()));
        }
        System.out.println("\t\t</inlinks>");
        System.out.println("\t\t<outlinks>");
        for (Link link : realm.outLinks()) {
            System.out.println(String.format("\t\t\t<link> id=%d from=%d to=%d",
                link.id(), link.fromrealm(), link.torealm()));
        }
        System.out.println("\t\t</outlinks>");
        System.out.println("\t</realm>");
    }

    private static void dumpAgent(Agent agent) {
        System.out.println(String.format("\t<agent> id=%d linkFinishTime=%d timeToPass=%d planIndex=%d",
            agent.id(), agent.linkFinishTime(), agent.timeToPass(), 
            agent.planIndex()));
        System.out.print("\t\t<plan>");
        for (int edge : agent.plan()) {
            System.out.print(String.format(" %d ", edge));   
        }
        System.out.println("</plan>");
        System.out.println("\t</agent>");
    }

    public static void dumpLink(Link link) {
        System.out.println(String.format("\t\t\t<link> id=%d from=%d to=%d nextTime=%d free_capacity=%d free_time=%d jam_time=%d currentCapacity=%d",
            link.id(), link.fromrealm(), link.torealm(), link.nexttime(), 
            link.freeCapacity(), link.freeTime(), link.jamTime(), 
            link.currentCapacity()));
        System.out.print("\t\t\t\t<agents>");
        for (Agent a : link.queue()) {
            System.out.print(String.format(" %d ", a.id()));   
        }
        System.out.println("</agents>");
        System.out.println("\t\t\t</link>");
    }
}