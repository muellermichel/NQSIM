package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import mpi.*;

public final class ChineseCity {
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

    public static void main(String[] args) throws IOException, NodeException, LinkException, ChineseCityException, CommunicatorException {
        MPI.Init(args);
        int my_rank = MPI.COMM_WORLD.Rank();
        int num_ranks = MPI.COMM_WORLD.Size();
        int num_rank_rows = (int)Math.sqrt(num_ranks);
        int num_rank_cols = num_ranks / num_rank_rows;
        if (num_rank_rows * num_rank_cols != num_ranks) {
            throw new ChineseCityException(num_ranks + " is not a valid number of ranks");
        }
        System.out.println(String.format(
            "%s: Initializing rank %d of %d; world to be divided into %dx%d worlds",
            MPI.Get_processor_name(),
            my_rank,
            num_ranks,
            num_rank_rows,
            num_rank_cols
        ));

        ObjectMapper om = new ObjectMapper();
        ObjectReader worldReader = om.readerFor(World.class);

        World world = null;
        System.out.println(my_rank + ": loading network");
        File file = new File("chinese_capital_187x187.json");
        InputStream is = new FileInputStream(file);
        World complete_world = World.fromJson(IOUtils.toByteArray(is), worldReader);
        Communicator communicator = new Communicator();
        complete_world.communicator = communicator;
        int num_total_nodes = complete_world.getNodes().size();
        int edge_length = (int)Math.sqrt(num_total_nodes);
        if (edge_length*edge_length != num_total_nodes) {
            throw new ChineseCityException(num_total_nodes + "does not have integer square root");
        }
        int num_cell_rows = (int)Math.ceil(edge_length/(double)num_rank_rows);
        int num_cell_cols = (int)Math.ceil(edge_length/(double)num_rank_cols);
        System.out.println(String.format(my_rank + ": world loaded with %d agents, %d links, %d nodes",
                World.sumOverAllLinks(complete_world, Link::queueLength),
                World.sumOverAllLinks(complete_world, link -> 1),
                num_total_nodes
        ));
        for (int rank_row = 0; rank_row < num_rank_rows; rank_row++) {
            for (int rank_col = 0; rank_col < num_rank_cols; rank_col++) {
                List<Node> curr_nodes = new ArrayList<>();
                int local_node_idx = 0;
                int assigned_rank = rank_row * num_rank_rows + rank_col;
                for (int local_row = 0; local_row < num_cell_rows && rank_row * num_cell_rows + local_row < edge_length; local_row++) {
                    for (int local_col = 0; local_col < num_cell_cols && rank_col * num_cell_cols + local_col < edge_length; local_col++) {
                        int global_node_index = (rank_row*num_cell_rows+local_row)*edge_length + rank_col*num_cell_cols + local_col;
                        Node curr_node = complete_world.getNodes().get(global_node_index);
                        curr_node.setAssignedRank(assigned_rank);
                        curr_nodes.add(curr_node);
                        communicator.setPairOfLocalAndGlobalNodeIndices(
                            global_node_index,
                            local_node_idx,
                            assigned_rank
                        );
                        local_node_idx += 1;
                    }
                }
                if (my_rank == assigned_rank) {
                    world = new World(curr_nodes);
                    world.communicator = communicator;
                }
            }
        }
        if (world == null) {
            throw new ChineseCityException(my_rank + ": no local world initialized");
        }
        if (my_rank == 0) {
            System.out.println("adding agents on rank 0");
            complete_world.addRandomAgents(3000000);
        }

//        for (int time=0; time < 600; time += 1) {
//            world.tick(1);
//        }
//        System.out.println(String.format("world finished with %d agents, %d routed",
//                World.sumOverAllLinks(world, Link::queueLength),
//                World.sumOverAllNodes(world, Node::getRouted)
//        ));
//        checkAgainstReference(world,"chinese_capital_3M_187x187_result.json");
    }
}
