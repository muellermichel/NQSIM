package ch.ethz.systems.nqsim2;

import java.util.ArrayList;
import java.util.Random;

public class SquareWorldGenerator extends WorldGenerator {

    private int edgeSize;
    private int numAgents;
    private int numSteps;
    private int linkCapacity;
    private Random rand;

    public SquareWorldGenerator(int edgeSize, int numAgents, int numSteps) {
        this.edgeSize = edgeSize;
        this.numAgents = numAgents;
        this.numSteps = numSteps;
        this.linkCapacity = 512;
        rand = new Random(0);
    }

    private int mod(int a, int b) {
        int c = a % b;
        return (c < 0) ? c + b : c;
    }

    public void setupEdges() {
        int nvertexes = edgeSize * edgeSize;
        int edgesPerVertex = 4;
        int nedges = nvertexes*edgesPerVertex;
        edges = new ArrayList<Edge>(nedges);
        for (Vertex v : vertexes) {
            for (Edge e : v.edges) {
                edges.add(e.id, e);
            }
        }
    }

    public void setupVertexes() {
        int nvertexes = edgeSize * edgeSize;
        int edgesPerVertex = 4;
        vertexes = new ArrayList<Vertex>(nvertexes);

        for (int i = 0; i < nvertexes; i++) {
            ArrayList<Edge> outgoing = new ArrayList<>(edgesPerVertex);
            Edge up    = new Edge(i * edgesPerVertex + 0, i, mod(i - edgeSize, nvertexes)); 
            Edge down  = new Edge(i * edgesPerVertex + 1, i, mod(i + edgeSize, nvertexes)); 
            Edge left  = new Edge(i * edgesPerVertex + 2, i, edgeSize * (i/edgeSize) + mod(i - 1, edgeSize)); 
            Edge right = new Edge(i * edgesPerVertex + 3, i, edgeSize * (i/edgeSize) + mod(i + 1, edgeSize)); 
            outgoing.add(0, up);
            outgoing.add(1, down);
            outgoing.add(2, left);
            outgoing.add(3, right);
            vertexes.add(i, new Vertex(i, outgoing));
        }

    }

    @Override
    public void setupGraph() {
        setupVertexes();
        setupEdges();
    }

    @Override
    public void setupPlans() {
        int numLinks = edges.size();
        ArrayList<Integer> startDistribution = new ArrayList<>(numLinks);
        plans = new ArrayList<>(numAgents);

        // Initialize initial distribution with zeroes.
        for (int i = 0; i < numLinks; i++) {
            startDistribution.add(i, 0);
        }

        // Initialize all plans with the initial link.
        for (int i = 0; i < numAgents; i++) {
            ArrayList<Edge> plan = new ArrayList<>(numSteps);
            while(true) {
                int initial = rand.nextInt(numLinks);
                if (startDistribution.get(initial) <  linkCapacity) {
                    plan.add(0, edges.get(initial));
                    break;
                }
            }
            plans.add(i, plan);
        }

        // Complete plans.
        for (int i = 0; i < numAgents; i++) {
            ArrayList<Edge> plan = plans.get(i);
            for (int j = 1; j < numSteps; j++) {
                Edge prevEdge = plan.get(j - 1);
                Vertex v = vertexes.get(prevEdge.dstVertex);
                Edge nextEdge = v.edges.get(rand.nextInt(v.edges.size())); 
                plan.add(j, nextEdge);
            }
        }
    }

}