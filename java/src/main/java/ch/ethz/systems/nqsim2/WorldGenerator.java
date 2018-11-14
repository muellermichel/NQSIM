package ch.ethz.systems.nqsim2;

import java.util.ArrayList;

public abstract class WorldGenerator {

    class Vertex {
        public int id;
        public ArrayList<Edge> edges;

        public Vertex(int id, ArrayList<Edge> edges) {
            this.id = id;
            this.edges = edges;
        }

        @Override
        public String toString() {
            return String.format("\nvertex tid=%d edges=%s", id, edges);
        }
    }

    class Edge {
        public int id;
        public int srcVertex;
        public int dstVertex;

        public Edge(int id, int srcVertex, int dstVertex) {
            this.id = id;
            this.srcVertex = srcVertex;
            this.dstVertex = dstVertex;
        }

        @Override
        public String toString() {
            return String.format("\n\tedge id=%d srcVertex=%d dstVertex=%d", 
                id, srcVertex, dstVertex);
        }
    }

    // Adjacency list graph.
    protected ArrayList<Vertex> vertexes;
    protected ArrayList<Edge> edges;
    // Array of plans. 
    protected ArrayList<ArrayList<Edge>> plans;
    // Translation from global Link ids to partition/realm.
    // linkGlobalToPartition[<global Link id>] -> partition/realm id.
    private ArrayList<Integer> linkGlobalToPartition;
    // Translation from global Link id to partition/realm internal id.
    // linkGlobalToLocal[<global Link id>] -> local Link id.
    private ArrayList<Integer> linkGlobalToLocal;
    // Array of realms (indexed by realm id).
    private ArrayList<Realm> realms;
    // Array of agents (indexed by agent id).
    private ArrayList<Agent> agents;

    // Should be implemented by specific world generators.
    public abstract void setupGraph();
    public abstract void setupPlans();

    // Trivial implementation splits Links with ids next to each other to the
    // same realm. The Links are split approx. evenly to different realms.
    public ArrayList<Integer> setupPartitions(int numPartitions) {
        int numLinks = edges.size();
        int linksPerPartition = numLinks / numPartitions;
        int remainder = numLinks % numPartitions;
        ArrayList<Integer> link2realm = new ArrayList<>(numLinks);

        for (int i = 0; i < numLinks - remainder; i++) {
            link2realm.add(i, i / linksPerPartition);
        }
        for (int i = numLinks - remainder; i < numLinks; i++) {
            link2realm.add(i, numPartitions - 1);
        }
        return link2realm;
    }

    private ArrayList<Realm> setupRealms(int numRealms) {
        ArrayList<Realm> realms = new ArrayList<>(numRealms);
        ArrayList<Integer> localCounters = new ArrayList<>(numRealms);
        ArrayList<ArrayList<Link>> localLinks = new ArrayList<>(numRealms);
        ArrayList<ArrayList<Link>> localInLinks = new ArrayList<>(numRealms);
        ArrayList<ArrayList<Link>> localOutLinks = new ArrayList<>(numRealms);
        linkGlobalToLocal = new ArrayList<>(edges.size());

        // Initialize data structures for each Realm.
        for (int i = 0; i < numRealms; i++) {
            localCounters.add(i, 0);
            localLinks.add(i, new ArrayList<>());
            localInLinks.add(i, new ArrayList<>());
            localOutLinks.add(i, new ArrayList<>());
        }

        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            // Array of Edges that can be accessed from this Edge.
            ArrayList<Edge> nextEdges = vertexes.get(edge.dstVertex).edges;

            // The origin Realm (owner Realm).
            int fromRealm = linkGlobalToPartition.get(i);
            
            // The destination Realm.
            int toRealm = linkGlobalToPartition.get(nextEdges.get(0).id);
            
            // Id of the Link inside the Realm.
            int id = localCounters.get(fromRealm);

            // Number of slots in the Link.
            int capacity = 512; // TODO - get real number for this.

            Link link = new Link(id, fromRealm, toRealm, capacity);

            // Saving the convertion between a global id and a local id.
            linkGlobalToLocal.add(i, id);

            // Add Link to Realm
            localLinks.get(fromRealm).add(link);

            // If Link leads to a diff Realm, add it to data structures.
            if (fromRealm != toRealm) {
                localOutLinks.get(fromRealm).add(link);
                localInLinks.get(toRealm).add(link);
            }

            // Update counter.
            localCounters.set(fromRealm, id + 1);
        }

        // Initialize Realms.
        for (int i = 0; i < numRealms; i++) {
            realms.add(
                i,
                new Realm(
                    i,
                    localLinks.get(i).toArray(new Link[localLinks.size()]), 
                    localInLinks.get(i).toArray(new Link[localInLinks.size()]), 
                    localOutLinks.get(i).toArray(new Link[localOutLinks.size()])));
        }

        return realms;
    }

    private ArrayList<Agent> setupAgents() {
        ArrayList<Agent> agents = new ArrayList<>(plans.size());
        // Convert plan, from global Link id to Realm-specific ids.
        for (int i = 0; i < plans.size(); i++) {
            int[] plan = new int[plans.get(i).size()];
            for (int j = 0; j < plans.get(i).size(); j++) {
                plan[j] = linkGlobalToLocal.get(plans.get(i).get(j).id);
            }


            // Install agent in the initial Link.
            int realmid = linkGlobalToPartition.get(plans.get(i).get(0).id);
            int linkid = plan[0];
            Link link = realms.get(realmid).links()[linkid];
            Agent a = new Agent(i, link, plan);
            link.push(a);

            // Add Agent to list of Agents. 
            agents.add(i, a);
        }
        return agents;
    }

    public World generateWorld(int numRealms) {
        setupGraph();
        setupPlans();
        linkGlobalToPartition = setupPartitions(numRealms);
        realms = setupRealms(numRealms);
        agents = setupAgents();
        return new World(
            realms.toArray(new Realm[realms.size()]), 
            agents.toArray(new Agent[agents.size()]));
    }

}