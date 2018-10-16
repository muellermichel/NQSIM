package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;
import mpi.MPIException;

import java.io.IOException;
import java.util.*;

public final class World {
    private int t;
    private List<Node> nodes;
    private  Map<String,Node> next_node_by_link_id;
    public Communicator communicator;

    public static void compareAllLinks(World world, World reference_world, LinkComparator operator) {
        ListIterator<Node> node_iterator = world.getNodes().listIterator();
        while (node_iterator.hasNext()) {
            int node_idx = node_iterator.nextIndex();
            Node node = node_iterator.next();
            ListIterator<Link> link_iterator = node.getIncomingLinks().listIterator();
            while (link_iterator.hasNext()) {
                int link_idx = link_iterator.nextIndex();
                Link link = link_iterator.next();
                Link reference_link = reference_world.getNodes()
                        .get(node_idx)
                        .getIncomingLinks()
                        .get(link_idx);
                operator.voidOp(link, reference_link, node_idx, link_idx);
            }
        }
    }

    public static long sumOverAllLinks(World world, LinkToIntOperator operator) {
        long sum = 0;
        for (Node node : world.getNodes()) {
            for (Link link : node.getIncomingLinks()) {
                sum += operator.intOp(link);
            }
        }
        return sum;
    }

    public static long sumOverAllNodes(World world, NodeToLongOperator operator) {
        long sum = 0;
        for (Node node : world.getNodes()) {
            sum += operator.longOp(node);
        }
        return sum;
    }

    public static void applyToAllNodes(World world, NodeOperator operator) {
        for (Node node : world.getNodes()) {
            operator.voidOp(node);
        }
    }

        public static World fromJson(byte[] jsonData, ObjectReader or) throws IOException {
        return or.readValue(jsonData);
    }

    @JsonCreator
    public World(
            @JsonProperty("nodes") List<Node> nodes,
            @JsonProperty("t") int t,
            @JsonProperty("outgoing_link_ids_by_node_index") Map<String, List<String>> outgoing_link_ids_by_node_index
    ) {
        this.nodes = nodes;
        this.t = t;

        Map<String, Link> links_by_id = new HashMap<>();
        ListIterator<Node> node_iterator = this.nodes.listIterator();
        while(node_iterator.hasNext()) {
            int idx = node_iterator.nextIndex();
            Node n = node_iterator.next();
            ListIterator<Link> link_iterator = n.getIncomingLinks().listIterator();
            while(link_iterator.hasNext()) {
                byte list_idx = (byte)link_iterator.nextIndex();
                Link l = link_iterator.next();
                links_by_id.put(l.getId(), l);
                l.setAssignedNodeIndex(idx);
                l.setAssignedIncomingLinkIdx(list_idx);
            }
        }
        for (Map.Entry<String, List<String>> entry : outgoing_link_ids_by_node_index.entrySet()) {
            int node_index = Integer.parseInt(entry.getKey());
            List<String> link_ids = entry.getValue();
            for (String link_id:link_ids) {
                this.nodes.get(node_index).addOutgoingLink(links_by_id.get(link_id));
            }
        }
        next_node_by_link_id = new HashMap<>();
        World.applyToAllNodes(this, node -> {
            for (Link link:node.getIncomingLinks()) {
                next_node_by_link_id.put(link.getId(), node);
            }
        });
    }

    public World(List<Node> nodes) {
        this(nodes, 0, new HashMap<>());
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void addRandomAgents(int numOfAgents) throws NodeException, LinkException, WorldException, MPIException {
        Map<Node,Integer> num_outgoing_links_by_node = new HashMap<>();
        World.applyToAllNodes(this, node -> {
            num_outgoing_links_by_node.put(node, node.getOutgoingLinks().size());
        });
        Random randomGenerator = new Random(1);
        for (int idx = 0; idx < numOfAgents; idx++) {
            int plan_length = randomGenerator.nextInt(40) + 20;
            int start_node_idx = randomGenerator.nextInt(this.nodes.size());
            Node current_node = this.nodes.get(start_node_idx);
            Node start_node = current_node;
            Link start_link = null;
            byte[] plan_bytes = new byte[plan_length];
            for (int leg_idx = 0; leg_idx < plan_length; leg_idx++) {
                byte next_link_idx = (byte) randomGenerator.nextInt(num_outgoing_links_by_node.get(current_node));
                Link link = current_node.getOutgoingLink(next_link_idx);
                if (start_link == null) {
                    start_link = link;
                }
                plan_bytes[leg_idx] = next_link_idx;
                current_node = next_node_by_link_id.get(link.getId());
            }
            Agent agent = new Agent(new Plan(plan_bytes));
            start_link.computeCapacity();
            if (start_node.route_agent(agent, start_node_idx, this.communicator) < 0) {
                throw new WorldException("node full, cannot add randomly anymore");
            }
        }
    }

    public void tick(int delta_t) throws WorldException, InterruptedException, ExceedingBufferException, CommunicatorException, MPIException {
        this.tick(delta_t, null);
    }

    public void tick(int delta_t, World complete_world) throws WorldException, InterruptedException, ExceedingBufferException, CommunicatorException, MPIException {
        long time;
        try {
            long start = System.currentTimeMillis();
            for (Node node : this.nodes) {
                node.tick(delta_t);
                node.computeCapacities();
            }
            if (this.communicator != null) {
                this.communicator.communicateCapacities(this);
            }
            ListIterator<Node> node_iterator = this.nodes.listIterator();
            while (node_iterator.hasNext()) {
                int idx = node_iterator.nextIndex();
                Node node = node_iterator.next();
                try {
                    node.route(idx, this.communicator);
                } catch (NodeException e) {
                    throw new WorldException(String.format(
                        "node %d: %s",
                        idx,
                        e.getMessage()
                    ));
                }
            }
            if (this.communicator != null) {
                this.communicator.communicateAgents(this, complete_world);
            }
            for (Node node : this.nodes) {
                node.finalizeTimestep();
            }
            this.t += 1;
            time = System.currentTimeMillis() - start;
        }
        catch (NodeException | LinkException e) {
            e.printStackTrace();
            throw new WorldException(String.format(
                "rank %d: %s",
                (this.communicator != null) ? this.communicator.getMyRank() : -1,
                e.getMessage()
            ));
        }
        if (this.communicator.getMyRank() == 0) {
            System.out.println(String.format(
                "time=%ds,real for %ds:%6.4fs,s/r:%6.4f",
                this.t,
                delta_t,
                time / (double) 1000,
                delta_t / (time / (double) 1000)
            ));
        }
    }
}
