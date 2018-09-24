package ch.ethz.systems.nqsim;

import java.nio.ByteBuffer;
import java.util.*;

import mpi.*;

public final class Communicator {
    private static int buffer_size = 100000000;
    private Map<Integer, Map<Integer, List<Agent>>> sending_agents_by_node_idx_by_rank;
    private byte[] receive_buffer;
    private ByteBuffer send_buffer;
    private Map<Integer, Integer> local_node_idx_by_global_idx;
    private Map<List<Integer>, Integer> global_node_idx_by_local_idx_and_rank;
    private Map<Integer, List<CapacityMessageIngredients>> capacity_message_ingredients_by_rank;
    private int my_rank = -1;
    private int num_ranks = -1;

    public Communicator(String[] args) throws MPIException {
        try {
            MPI.Init(args);
            MPI.COMM_WORLD.setErrhandler(MPI.ERRORS_RETURN);
        }
        catch (NoClassDefFoundError e) {
            //ignore <- user has no MPI, we try and make it work on single process
        }
        this.sending_agents_by_node_idx_by_rank = new HashMap<>();
        this.local_node_idx_by_global_idx = new HashMap<>();
        this.global_node_idx_by_local_idx_and_rank = new HashMap<>();
        this.capacity_message_ingredients_by_rank = new HashMap<>();
        this.receive_buffer = new byte[buffer_size];
        this.send_buffer = ByteBuffer.allocateDirect(buffer_size);
    }

    public void shutDown() throws MPIException {
        try {
            MPI.Finalize();
        }
        catch (NoClassDefFoundError e) {
            //ignore
        }
    }

    public void prepareAgentForTransmission(Agent agent, int rank, int global_node_idx) {
        Map<Integer, List<Agent>> agents_by_node_idx = this.sending_agents_by_node_idx_by_rank
            .computeIfAbsent(rank, k -> new HashMap<>());
        List<Agent> agents_for_rank_and_node_idx = agents_by_node_idx
            .computeIfAbsent(global_node_idx, k -> new LinkedList<>());
        agents_for_rank_and_node_idx.add(agent);
    }

    public void addLink(Node sourceNode, Link link, int sourceNodeIdx, int outgoingLinkIdx) throws MPIException {
        if (link.getAssignedRank() == this.getMyRank()) {
            return;
        }
        List<CapacityMessageIngredients> capacity_message_factories = this.capacity_message_ingredients_by_rank
            .computeIfAbsent(sourceNode.getAssignedRank(), k -> new LinkedList<>());
        capacity_message_factories.add(new CapacityMessageIngredients(sourceNode, link, sourceNodeIdx, outgoingLinkIdx));
        System.out.println(String.format(
            "rank %d: added link %s to capacity messages from source node %d; node rank %d, link rank %d",
            this.getMyRank(),
            link.getId(),
            sourceNodeIdx,
            sourceNode.getAssignedRank(),
            link.getAssignedRank()
        ));
    }

    public Request sendAgentsNB(int rank) throws ExceedingBufferException, MPIException {
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
        else {
            byte_length = 1;
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
        else {
            bytes[0] = -1;
        }
        System.out.println(String.format(
            "rank %d: sending %d bytes to %d",
            this.getMyRank(),
            byte_length,
            rank
        ));
        return this.nonBlockingSend(bytes, rank, 10);
//        return MPI.COMM_WORLD.Isend(
//                bytes,
//                0,
//                (int)byte_length,
//                MPI.BYTE,
//                rank,
//                10
//        );
    }

    public Request sendCapacitiesNB(int rank) throws ExceedingBufferException, LinkException, MPIException {
        List<CapacityMessageIngredients> capacity_message_factories = this.capacity_message_ingredients_by_rank.get(rank);
        long byte_length = 0;
        if (capacity_message_factories != null) {
            byte_length = 9 * capacity_message_factories.size();
        }
        if (byte_length > buffer_size) {
            throw new ExceedingBufferException(String.valueOf(byte_length));
        }
        byte[] bytes = new byte[(int)byte_length];
        if (capacity_message_factories != null) {
            int offset = 0;
            for (CapacityMessageIngredients fac : capacity_message_factories) {
                Helper.intToByteArray(fac.node_index, bytes, offset);
                offset += 4;
                bytes[offset] = (byte)fac.outgoing_link_index;
                offset += 1;
                Helper.intToByteArray(fac.link.availableCapacity(), bytes, offset);
                offset += 4;
            }
        }
        return this.nonBlockingSend(bytes, rank, 11);
//        return MPI.COMM_WORLD.Isend(
//                bytes,
//                0,
//                (int)byte_length,
//                MPI.BYTE,
//                rank,
//                11
//        );
    }

    public void updateLinkCapacitiesFromRank(int rank, World world_to_update) throws NodeException, MPIException {
//        Status status = MPI.COMM_WORLD.Recv(this.receive_buffer, 0, buffer_size, MPI.BYTE, rank, 11);
        Status status = this.receive(this.receive_buffer, rank, 11);
        int offset = 0;
//        while (offset < status.Get_count(MPI.BYTE)) {
        while (offset < status.getCount(MPI.BYTE)) {
            int global_node_idx = Helper.intFromByteArray(this.receive_buffer, offset);
            offset += 4;
            byte outgoing_link_idx = this.receive_buffer[offset];
            offset += 1;
            int availableCapacity = Helper.intFromByteArray(this.receive_buffer, offset);
            offset += 4;
            world_to_update.getNodes().get(this.getLocalNodeIdxFromGlobalIdx(global_node_idx))
                .getOutgoingLink(outgoing_link_idx)
                .setCommunicatedCapacity(availableCapacity);
        }
    }

    public void updateWorldFromRank(int rank, World world_to_update) throws IndexOutOfBoundsException, NodeException, CommunicatorException, MPIException {
//        Status status = MPI.COMM_WORLD.Recv(this.receive_buffer, 0, buffer_size, MPI.BYTE, rank, 10);
        Status status = this.receive(this.receive_buffer, rank, 10);
        int offset = 0;
//        while (offset < status.Get_count(MPI.BYTE)) {
        while (offset < status.getCount(MPI.BYTE)) {
            if (this.receive_buffer[offset] == -1) {
                break;
            }
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
                        this.getMyRank(),
                        node_idx,
                        routing_status
                    ));
                }
                System.out.println(String.format(
                    "transferred agent from rank %d to %d",
                    rank,
                    this.getMyRank()
                ));
            }
        }
    }

    public void communicateAgents(World world_to_update) throws InterruptedException, ExceedingBufferException, CommunicatorException, NodeException, MPIException {
        System.out.println(String.format("rank %d: starting to communicate agents",
            my_rank
        ));
        List<Request> send_requests = new LinkedList<>();
        int my_rank = this.getMyRank();
        int num_ranks = this.getNumberOfRanks();
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            send_requests.add(this.sendAgentsNB(rank));
        }
        System.out.println(String.format("rank %d: %d send requests ongoing, starting to receive agents",
            my_rank,
            send_requests.size()
        ));
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            System.out.println(String.format("rank %d: receiving from %d",
                my_rank,
                rank
            ));
            this.updateWorldFromRank(rank, world_to_update);
        }
        System.out.println(String.format("rank %d: waiting to send to %d recipients",
            my_rank,
            send_requests.size()
        ));
        this.waitAll(send_requests);
        System.out.println(String.format("rank %d: done waiting",
            my_rank
        ));
    }

    public void communicateCapacities(World world_to_update) throws NodeException, LinkException, MPIException, ExceedingBufferException {
        List<Request> send_requests = new LinkedList<>();
        int my_rank = this.getMyRank();
        int num_ranks = this.getNumberOfRanks();
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            send_requests.add(this.sendCapacitiesNB(rank));
        }
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            this.updateLinkCapacitiesFromRank(rank, world_to_update);
        }
        this.waitAll(send_requests);
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

    public void waitAll(List<Request> requests) throws MPIException {
        //note that we get exceptions when using FastMPJ's built-in wait function; incompatibility with current Java?
//        Request[] request_array = requests.toArray(new Request[requests.size()]);
//        while(true) {
//            Status[] status_array = Request.Testall(request_array);
//            if (status_array != null) {
//                break;
//            }
//            Thread.sleep(5);
//        }
        Request.waitAll(requests.toArray(new Request[requests.size()]));
        for (Request req : requests) {
            req.free();
        }
    }

    public Status receive(byte[] buf, int rank, int tag) throws MPIException {
        return MPI.COMM_WORLD.recv(buf, 0, MPI.BYTE, rank, tag);
    }

    public Request nonBlockingSend(byte[] bytes, int rank, int tag) throws MPIException {
        ByteBuffer byteBuffer = MPI.newByteBuffer(bytes.length);
        byteBuffer.put(bytes);
        return MPI.COMM_WORLD.iSend(byteBuffer, 0, MPI.BYTE, rank, tag);
    }

    public int getMyRank() throws MPIException {
        if (my_rank != -1) {
            return my_rank;
        }
        try {
//            my_rank = MPI.COMM_WORLD.Rank();
            my_rank = MPI.COMM_WORLD.getRank();
        }
        catch (NoClassDefFoundError e) {
            my_rank = 0;
        }
        return my_rank;
    }

    public int getNumberOfRanks() throws MPIException {
        if (num_ranks != -1) {
            return num_ranks;
        }
        try {
//            num_ranks = MPI.COMM_WORLD.Size();
            num_ranks = MPI.COMM_WORLD.getSize();
        }
        catch (NoClassDefFoundError e) {
            num_ranks = 1;
        }
        return num_ranks;
    }

    public String getProcessorName() throws MPIException {
        try {
//            return MPI.Get_processor_name();
            return MPI.getProcessorName();
        }
        catch (NoClassDefFoundError e) {
            return "localhost";
        }
    }
}
