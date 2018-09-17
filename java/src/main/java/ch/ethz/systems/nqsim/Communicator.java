package ch.ethz.systems.nqsim;

import java.util.*;

import mpi.*;

public final class Communicator {
    private static int buffer_size = 100000000;
    private Map<Integer, Map<Integer, List<Agent>>> sending_agents_by_node_idx_by_rank;
    private byte[] receive_buffer;
    private Map<Integer, Integer> local_node_idx_by_global_idx;
    private Map<List<Integer>, Integer> global_node_idx_by_local_idx_and_rank;
    private Map<Integer, Map<Integer, List<Integer>>> foreign_outgoing_link_indices_by_node_idx_by_sender_rank;
    private Map<Integer, Map<Integer, List<Integer>>> foreign_outgoing_link_indices_by_node_idx_by_receiver_rank;

    public Communicator() {
        this.sending_agents_by_node_idx_by_rank = new HashMap<>();
        this.local_node_idx_by_global_idx = new HashMap<>();
        this.global_node_idx_by_local_idx_and_rank = new HashMap<>();
        this.receive_buffer = new byte[buffer_size];
    }

    public void prepareAgentForTransmission(Agent agent, int rank, int global_node_idx) {
        Map<Integer, List<Agent>> agents_by_node_idx = this.sending_agents_by_node_idx_by_rank
            .computeIfAbsent(rank, k -> new HashMap<>());
        List<Agent> agents_for_rank_and_node_idx = agents_by_node_idx
            .computeIfAbsent(global_node_idx, k -> new LinkedList<>());
        agents_for_rank_and_node_idx.add(agent);
    }

    public void addLink(Node sourceNode, Link link, int sourceNodeIdx, int outgoingLinkIdx) {
        if (sourceNode.getAssignedRank() == link.getAssignedRank()) {
            return;
        }
        Map<Integer, List<Integer>> foreign_outgoing_link_indices_by_node_idx = this.foreign_outgoing_link_indices_by_node_idx_by_sender_rank
            .computeIfAbsent(link.getAssignedRank(), k -> new HashMap<>());
        List<Integer> foreign_outgoing_link_indices = foreign_outgoing_link_indices_by_node_idx
            .computeIfAbsent(sourceNodeIdx, k -> new LinkedList<>());
        foreign_outgoing_link_indices.add(outgoingLinkIdx);



        foreign_outgoing_link_indices_by_node_idx_by_sender_rank_by_receiving_rank
    }

    public Request nonBlockingSendToRank(int rank) throws ExceedingBufferException {
        Map<Integer, List<Agent>> agents_by_node_idx = this.sending_agents_by_node_idx_by_rank.get(rank);
        long byte_length = 0;
        if (agents_by_node_idx != null) {
            byte_length = 8 * agents_by_node_idx.size();
            for (List<Agent> agents : agents_by_node_idx.values()) {
                for (Agent agent:agents) {
                    byte_length += agent.byteLength();
                }
            }
        }
        if (byte_length > buffer_size) {
            throw new ExceedingBufferException(String.valueOf(byte_length));
        }
        byte[] bytes = new byte[(int)byte_length];
        if (agents_by_node_idx != null) {
            int offset = 0;
            for (Map.Entry<Integer, List<Agent>> entry : agents_by_node_idx.entrySet()) {
                Integer node_idx = entry.getKey();
                List<Agent> agents = entry.getValue();
                Helper.intToByteArray(node_idx, bytes, offset);
                offset += 4;
                Helper.intToByteArray(agents.size(), bytes, offset);
                offset += 4;
                for (Agent agent : agents) {
                    agent.serializeToBytes(bytes, offset);
                    offset += agent.byteLength();
                }
            }
        }
        return MPI.COMM_WORLD.Isend(
                bytes,
                0,
                (int)byte_length,
                MPI.BYTE,
                rank,
                10
        );
    }

    public void updateWorldFromRank(int rank, World world_to_update) throws IndexOutOfBoundsException, NodeException, CommunicatorException {
        Status status = MPI.COMM_WORLD.Recv(this.receive_buffer, 0, buffer_size, MPI.BYTE, rank, 10);
        int offset = 0;
        while (offset < status.Get_count(MPI.BYTE)) {
            int global_node_idx = Helper.intFromByteArray(this.receive_buffer, offset);
            offset += 4;
            int num_agents = Helper.intFromByteArray(this.receive_buffer, offset);
            offset += 4;
            if (global_node_idx < 0 || num_agents < 0) {
                throw new CommunicatorException(String.format("invalid message received; %d; %d", global_node_idx, num_agents));
            }
            int node_idx = this.getLocalNodeIdxFromGlobalIdx(global_node_idx);
            Node node = world_to_update.getNodes().get(node_idx);
            if (node == null) {
                throw new IndexOutOfBoundsException("no node with index " + node_idx);
            }
            for (int idx = 0; idx < num_agents; idx++) {
                Agent currentAgent = Agent.deserializeFromBytes(this.receive_buffer, offset);
                offset += currentAgent.byteLength();
                int routing_status = node.route_agent(currentAgent, node_idx, this);
                if (routing_status < 0) {
                    throw new CommunicatorException(String.format(
                        "transferring agent from rank %d to %d failed on node with index %d: %d",
                        rank,
                        MPI.COMM_WORLD.Rank(),
                        node_idx,
                        routing_status
                    ));
                }
            }
        }
    }

    public void communicateAll(World world_to_update) throws InterruptedException, ExceedingBufferException, NodeException, CommunicatorException {
        List<Request> send_requests = new LinkedList<>();
        int my_rank = this.getMyRank();
        int num_ranks = this.getNumberOfRanks();
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            send_requests.add(this.nonBlockingSendToRank(rank));
        }
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            this.updateWorldFromRank(rank, world_to_update);
        }
        for (Request request:send_requests) {
            request.wait();
        }
    }

    public void setPairOfLocalAndGlobalNodeIndices(int global_idx, int local_idx, int local_rank) throws CommunicatorException {
        if (this.local_node_idx_by_global_idx.containsKey(global_idx)) {
            throw new CommunicatorException("global node index already set: " + global_idx);
        }
        this.local_node_idx_by_global_idx.put(global_idx, local_idx);
        this.global_node_idx_by_local_idx_and_rank.put(
            Arrays.asList(local_idx, local_rank),
            global_idx
        );
    }

    public int getLocalNodeIdxFromGlobalIdx(int global_idx) {
        return this.local_node_idx_by_global_idx.get(global_idx);
    }

    public int getGlobalNodeIdxFromLocalIdx(int local_idx, int local_rank) {
        return this.global_node_idx_by_local_idx_and_rank.get(Arrays.asList(local_idx, local_rank));
    }

    public int getMyRank() {
        try {
            return MPI.COMM_WORLD.Rank();
        }
        catch (NoClassDefFoundError e) {
            return 0;
        }
    }

    public int getNumberOfRanks() {
        try {
            return MPI.COMM_WORLD.Size();
        }
        catch (NoClassDefFoundError e) {
            return 1;
        }
    }
}
