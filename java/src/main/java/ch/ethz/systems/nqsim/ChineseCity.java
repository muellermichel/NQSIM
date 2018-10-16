package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import mpi.MPIException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

public final class ChineseCity {
    public World world;
    public World complete_world;

    public static void checkAgainstReference(World world, String filename) throws IOException {
        ObjectMapper om = new ObjectMapper();
        ObjectReader worldReader = om.readerFor(World.class);
        File reference_file = new File(filename);
        InputStream reference_is = new FileInputStream(reference_file);
        World reference_world = World.fromJson(IOUtils.toByteArray(reference_is), worldReader);
        World.compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.getLength() == ref_link.getLength() :
                    String.format("node %d, link%d(%s): %d vs. %d length",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.getLength(),
                            ref_link.getLength()
                    );
        });
        World.compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.getFreeFlowVelocity() == ref_link.getFreeFlowVelocity() :
                    String.format("node %d, link%d(%s): %d vs. %d ff velocity",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.getFreeFlowVelocity(),
                            ref_link.getFreeFlowVelocity()
                    );
        });
        World.compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.getFreeFlowTravelTime() == ref_link.getFreeFlowTravelTime() :
                    String.format("node %d, link%d(%s): %d vs. %d ff ttime",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.getFreeFlowTravelTime(),
                            ref_link.getFreeFlowTravelTime()
                    );
        });
        World.compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.queueLength() == ref_link.queueLength() :
                    String.format("node %d, link%d(%s): %d vs. %d agents",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.queueLength(),
                            ref_link.queueLength()
                    );
        });
    }

    public ChineseCity(String[] args, String input_file_path) throws ChineseCityException, MPIException, IOException, CommunicatorException, NodeException {
        System.out.println("one");
        Communicator communicator = new Communicator(args);
        int my_rank = communicator.getMyRank();
        int num_ranks = communicator.getNumberOfRanks();
        int num_rank_rows = (int)Math.sqrt(num_ranks);
        int num_rank_cols = num_ranks / num_rank_rows;
        if (num_rank_rows * num_rank_cols != num_ranks) {
            throw new ChineseCityException(num_ranks + " is not a valid number of ranks");
        }
        System.out.println(String.format(
                "%s: Initializing rank %d of %d; world to be divided into %dx%d worlds",
                communicator.getProcessorName(),
                my_rank,
                num_ranks,
                num_rank_rows,
                num_rank_cols
        ));

        ObjectMapper om = new ObjectMapper();
        ObjectReader worldReader = om.readerFor(World.class);
        System.out.println(my_rank + ": loading network");
        File file = new File(input_file_path);
        InputStream is = new FileInputStream(file);
        World complete_world = World.fromJson(IOUtils.toByteArray(is), worldReader);
        complete_world.communicator = communicator;
        int num_total_nodes = complete_world.getNodes().size();
        System.out.println(String.format(my_rank + ": world loaded with %d agents, %d links, %d nodes",
                World.sumOverAllLinks(complete_world, Link::queueLength),
                World.sumOverAllLinks(complete_world, link -> 1),
                num_total_nodes
        ));

        World world = null;
        int edge_length = (int)Math.sqrt(num_total_nodes);
        if (edge_length*edge_length != num_total_nodes) {
            throw new ChineseCityException(num_total_nodes + "does not have integer square root");
        }
        int my_row = my_rank / num_rank_cols;
        int my_col = my_rank - my_row * num_rank_cols;
        int num_cell_rows;
        int num_cell_cols;
        int max_num_cell_rows = (int)Math.ceil(edge_length/(double)num_rank_rows);
        int max_num_cell_cols = (int)Math.ceil(edge_length/(double)num_rank_cols);
        Map<Integer, Node> node_map = new HashMap(); //just to do sanity checks
        ListIterator<Node> node_iterator = complete_world.getNodes().listIterator();
        while(node_iterator.hasNext()) {
            int idx = node_iterator.nextIndex();
            Node node = node_iterator.next();
            node_map.put(idx, node);
        }
        for (int rank_row = 0; rank_row < num_rank_rows; rank_row++) {
            for (int rank_col = 0; rank_col < num_rank_cols; rank_col++) {
                List<Node> curr_nodes = new ArrayList<>();
                int local_node_idx = 0;
                int assigned_rank = rank_row * num_rank_cols + rank_col;
                if (rank_col == num_rank_cols - 1) {
                    num_cell_cols = (int)Math.floor(edge_length/(double)num_rank_cols);
                }
                else {
                    num_cell_cols = max_num_cell_cols;
                }
                if (rank_row == num_rank_rows - 1) {
                    num_cell_rows = (int)Math.floor(edge_length/(double)num_rank_rows);
                }
                else {
                    num_cell_rows = max_num_cell_rows;
                }
                for (int local_row = 0; local_row < num_cell_rows; local_row++) {
                    for (int local_col = 0; local_col < num_cell_cols; local_col++) {
                        int global_node_index = (rank_row*max_num_cell_rows+local_row)*edge_length + rank_col*max_num_cell_cols + local_col;
                        Node curr_node = complete_world.getNodes().get(global_node_index);
                        node_map.remove(global_node_index);
                        curr_node.setAssignedRank(assigned_rank);
                        curr_nodes.add(curr_node);
                        if (communicator.getLocalNodeIdxFromGlobalIdx(global_node_index) != -1) {
                            throw new ChineseCityException(String.format(
                                    "%d: global node index %d already assigned: %d",
                                    my_rank,
                                    global_node_index,
                                    communicator.getLocalNodeIdxFromGlobalIdx(global_node_index)
                            ));
                        }
                        communicator.setPairOfLocalAndGlobalNodeIndices(
                                global_node_index,
                                local_node_idx,
                                assigned_rank
                        );
                        if (global_node_index == 16550) {
                            System.out.println(String.format(
                                "%d: c.node 16550, l.node %d link 1: %d lt",
                                my_rank,
                                local_node_idx,
                                complete_world.getNodes().get(16550).getIncomingLinks().get(1).getFreeFlowTravelTime()
                            ));
                        }
                        local_node_idx += 1;
                    }
                }
                if (my_rank == assigned_rank) {
                    world = new World(curr_nodes);
                    world.communicator = communicator;
                    System.out.println(String.format(
                        "%d: local world loaded with %d nodes, %d of %d rows x %d cells, %d of %d cols x %d cells",
                        my_rank,
                        curr_nodes.size(),
                        my_row,
                        num_rank_rows,
                        num_cell_rows,
                        my_col,
                        num_rank_cols,
                        num_cell_cols
                    ));
                }
            }
        }
        if (!node_map.isEmpty()) {
            throw new ChineseCityException(my_rank + ": " + node_map.size() + " nodes have not been assigned to ranks");
        }
        if (world == null) {
            throw new ChineseCityException(my_rank + ": no local world initialized");
        }
        communicator.validateDecomposition(world, complete_world);
//        if (my_rank == complete_world.getNodes().get(16550).getAssignedRank()) {
//            int local_16550 = communicator.getLocalNodeIdxFromGlobalIdx(16550);
//            if (local_16550 < 0) {
//                throw new ChineseCityException("blub");
//            }
//            System.out.println(String.format(
//                "%d: c.node 16550, l.node %d link 1: %d lt; local: %d lt",
//                my_rank,
//                local_16550,
//                complete_world.getNodes().get(16550).getIncomingLinks().get(1).getFreeFlowTravelTime(),
//                world.getNodes().get(local_16550).getIncomingLinks().get(1).getFreeFlowTravelTime()
//            ));
//        }
        node_iterator = world.getNodes().listIterator();
        while(node_iterator.hasNext()) {
            int local_node_index = node_iterator.nextIndex();
            Node node = node_iterator.next();
            ListIterator<Link> link_iterator = node.getOutgoingLinks().listIterator();
            while (link_iterator.hasNext()) {
                int idx = link_iterator.nextIndex();
                communicator.addLink(
                        node,
                        link_iterator.next(),
                        world.communicator.getGlobalNodeIdxFromLocalIdx(local_node_index, communicator.getMyRank()),
                        idx
                );
            }
        }
        this.complete_world = complete_world;
        this.world = world;
        if (this.complete_world.getNodes().size() >= 8366 - 1) {
            System.out.println(String.format(
                    "%d: c.node 8366 link 1: %d lt",
                    my_rank,
                    this.complete_world.getNodes().get(8366).getIncomingLinks().get(1).getFreeFlowTravelTime()
            ));
        }
        if (this.complete_world.getNodes().size() >= 16550 - 1) {
            System.out.println(String.format(
                    "%d: c.node 16550 link 1: %d lt",
                    my_rank,
                    this.complete_world.getNodes().get(16550).getIncomingLinks().get(1).getFreeFlowTravelTime()
            ));
        }
        if (this.world.getNodes().size() >= 8366 - 1) {
            System.out.println(String.format(
                    "%d: node 8366 link 1: %d lt",
                    my_rank,
                    this.world.getNodes().get(8366).getIncomingLinks().get(1).getFreeFlowTravelTime()
            ));
        }
        if (this.world.getNodes().size() >= 16550 - 1) {
            System.out.println(String.format(
                    "%d: node 16550 link 1: %d lt",
                    my_rank,
                    this.world.getNodes().get(16550).getIncomingLinks().get(1).getFreeFlowTravelTime()
            ));
        }
    }

    public void initializeRandomAgents(int num_agents) throws MPIException, WorldException, NodeException, LinkException, InterruptedException, ExceedingBufferException, CommunicatorException {
        if (this.world.communicator.getMyRank() == 0) {
            System.out.println("adding " + num_agents + " agents on rank 0");
            this.complete_world.addRandomAgents(num_agents);
        }
        for (Node node : this.complete_world.getNodes()) {
            node.computeCapacities();
        }
        this.world.communicator.communicateAgents(world, this.complete_world);
    }

    public void run() throws WorldException, CommunicatorException, ExceedingBufferException, InterruptedException, MPIException {
        long start = System.currentTimeMillis();
        for (int time=0; time < 600; time += 1) {
            this.world.tick(1, this.complete_world);
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(String.format("rank %d: world finished with %d agents, %d routed, avg. s/r %6.4f",
            this.world.communicator.getMyRank(),
            World.sumOverAllLinks(world, Link::queueLength),
            World.sumOverAllNodes(world, Node::getRouted),
            600 / (time / (double) 1000)
        ));
//        ListIterator<Node> node_iterator = complete_world.getNodes().listIterator();
//        while (node_iterator.hasNext()) {
//            int node_idx = node_iterator.nextIndex();
//            Node node = node_iterator.next();
//            ListIterator<Link> link_iterator = node.getIncomingLinks().listIterator();
//            while (link_iterator.hasNext()) {
//                int link_idx = link_iterator.nextIndex();
//                Link link = link_iterator.next();
//                if (link.queueLength() > 0) {
//                    System.out.println(String.format(
//                        "rank %d, node %d, link %d: %d agents",
//                        this.world.communicator.getMyRank(),
//                        node_idx,
//                        link_idx,
//                        link.queueLength()
//                    ));
//                }
//            }
//        }
//        checkAgainstReference(world,"chinese_capital_3M_187x187_result.json");
        this.world.communicator.shutDown();
    }
}
