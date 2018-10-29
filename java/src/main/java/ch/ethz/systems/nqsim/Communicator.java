package ch.ethz.systems.nqsim;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import mpi.*;

public final class Communicator {
    private static int buffer_size = 100000000;
//    private static int buffer_size = 100;
    private Map<Integer, Map<Integer, Map<Byte, List<Agent>>>> sending_agents_by_link_idx_by_node_idx_by_rank;
    private ByteBuffer receive_buffer;
    private ByteBuffer send_buffer;
    private Map<Integer, Integer> local_node_idx_by_global_idx;
    private Map<Integer, Integer> global_node_idx_by_local_idx_and_rank;
    private Map<Integer, List<CapacityMessageIngredients>> capacity_message_ingredients_by_rank;
    private int my_rank = -1;
    private int num_ranks = -1;
    public int num_ranks_override = -1;

    public Communicator(String[] args) throws MPIException {
        try {
            System.out.println("attempting to initialize MPI");
            MPI.Init(args);
            System.out.println("finished initializing");
            MPI.COMM_WORLD.setErrhandler(MPI.ERRORS_RETURN);
            this.receive_buffer = MPI.newByteBuffer(buffer_size);
            this.send_buffer = MPI.newByteBuffer(buffer_size);
        }
        catch (NoClassDefFoundError|UnsatisfiedLinkError e) {
            //ignore <- user has no MPI, we try and make it work on single process
        }
        resetData();
    }

    public void resetData() {
        this.sending_agents_by_link_idx_by_node_idx_by_rank = new HashMap<>();
        this.local_node_idx_by_global_idx = new HashMap<>();
        this.global_node_idx_by_local_idx_and_rank = new HashMap<>();
        this.capacity_message_ingredients_by_rank = new HashMap<>();
    }

    public void shutDown() throws MPIException {
        try {
            MPI.Finalize();
        }
        catch (NoClassDefFoundError e) {
            //ignore
        }
    }

    public void prepareAgentForTransmission(Agent agent, int rank, int global_node_idx, byte incoming_link_idx) {
        Map<Integer, Map<Byte, List<Agent>>> agents_by_link_idx_by_node_idx = this.sending_agents_by_link_idx_by_node_idx_by_rank
            .computeIfAbsent(rank, k -> new HashMap<>());
        Map<Byte, List<Agent>> agents_by_link_idx = agents_by_link_idx_by_node_idx
            .computeIfAbsent(global_node_idx, k -> new HashMap<>());
        List<Agent> agents = agents_by_link_idx
            .computeIfAbsent(incoming_link_idx, k -> new LinkedList<>());
        agents.add(agent);
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
        Map<Integer, Map<Byte, List<Agent>>> agents_by_link_idx_by_node_idx = this.sending_agents_by_link_idx_by_node_idx_by_rank.remove(rank);
        long byte_length = 0;
        if (agents_by_link_idx_by_node_idx != null) {
            byte_length = 8 * agents_by_link_idx_by_node_idx.size(); // allocate integer for each node idx plus integer for the number of links represented per node
            for (Map<Byte, List<Agent>> agents_by_link_idx : agents_by_link_idx_by_node_idx.values()) {
                byte_length += 5 * agents_by_link_idx.size(); // allocate byte for each link idx per node plus an integer to represent length of agents per link per node
                for (List<Agent> agents : agents_by_link_idx.values()) {
                    for (Agent agent:agents) {
                        byte_length += agent.byteLength(); // allocate space for each agent
                    }
                }
            }
        }
        if (byte_length > buffer_size) {
            throw new ExceedingBufferException(String.valueOf(byte_length));
        }
        byte[] bytes = new byte[(int)byte_length];
        if (agents_by_link_idx_by_node_idx != null) {
            int offset = 0;
            for (Map.Entry<Integer, Map<Byte, List<Agent>>> entry : agents_by_link_idx_by_node_idx.entrySet()) {
                Integer node_idx = entry.getKey();
                Map<Byte, List<Agent>> agents_by_link_idx = entry.getValue();
                Helper.intToByteArray(node_idx, bytes, offset);
                offset += 4;
                Helper.intToByteArray(agents_by_link_idx.size(), bytes, offset);
                offset += 4;
                for (Map.Entry<Byte, List<Agent>> link_entry : agents_by_link_idx.entrySet()) {
                    byte link_idx = link_entry.getKey();
                    List<Agent> agents = link_entry.getValue();
                    bytes[offset] = link_idx;
                    offset += 1;
                    Helper.intToByteArray(agents.size(), bytes, offset);
                    offset += 4;
                    for (Agent agent : agents) {
                        agent.serializeToBytes(bytes, offset);
                        offset += agent.byteLength();
//                        if (agent.getId().equals("3495")) {
//                            System.out.println("3495 appended to send to node " + node_idx + ". plan:" + agent.getPlan().toString());
//                        }
                    }
                    //                System.out.println(String.format(
                    //                    "rank %d: preparing to send %d agents to node %d on rank %d",
                    //                    this.getMyRank(),
                    //                    agents.size(),
                    //                    node_idx,
                    //                    rank
                    //                ));
                }
            }
        }
//        System.out.println(String.format(
//            "rank %d: sending %d bytes to %d",
//            this.getMyRank(),
//            byte_length,
//            rank
//        ));
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

    public Request sendRawBytesNB(int rank, byte[] bytes) throws MPIException {
        return this.nonBlockingSend(bytes, rank, 12);
    }

    public byte[] receiveRawBytes(int rank) throws MPIException {
        return this.receive(rank, 12);
    }

    public void communicateEventLog() throws MPIException, IOException, WorldException {
        List<Request> send_requests = new LinkedList<>();
        int my_rank = this.getMyRank();
        int num_ranks = this.getNumberOfRanks();
        if (my_rank != 0) {
            System.out.println(my_rank + " sending event log");
            send_requests.add(this.sendRawBytesNB(
                0,
                EventLog.toJson().getBytes("UTF-8")
            ));
        }
        else {
            for (int rank = 1; rank < num_ranks; rank++) {
                System.out.println(my_rank + " receiving event log from " + rank);
                EventLog.mergeJson(new String(receiveRawBytes(rank), "UTF-8"));
            }
        }
        this.waitAll(send_requests);
        System.out.println(my_rank + " sending done");
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
        byte[] bytes = this.receive(rank, 11);
        int offset = 0;
//        while (offset < status.Get_count(MPI.BYTE)) {
        while (offset < bytes.length) {
            int global_node_idx = Helper.intFromByteArray(bytes, offset);
            offset += 4;
            byte outgoing_link_idx = bytes[offset];
            offset += 1;
            int availableCapacity = Helper.intFromByteArray(bytes, offset);
            offset += 4;
            world_to_update.getNodes().get(this.getLocalNodeIdxFromGlobalIdx(global_node_idx))
                .getOutgoingLink(outgoing_link_idx)
                .setCommunicatedCapacity(availableCapacity);
        }
    }

    public void updateWorldFromRank(int rank, World world_to_update, World complete_world, int t) throws IndexOutOfBoundsException, CommunicatorException, MPIException {
//        Status status = MPI.COMM_WORLD.Recv(this.receive_buffer, 0, buffer_size, MPI.BYTE, rank, 10);
        byte[] bytes = this.receive(rank, 10);
//        System.out.println(String.format(
//            "rank %d: %d bytes received from %d",
//            this.getMyRank(),
//            status.getCount(MPI.BYTE),
//            rank
//        ));
        int offset = 0;
//        while (offset < status.Get_count(MPI.BYTE)) {
        while (offset < bytes.length) {
            int global_node_idx = Helper.intFromByteArray(bytes, offset);
            offset += 4;
            int num_links = Helper.intFromByteArray(bytes, offset);
            offset += 4;
            if (global_node_idx < 0 || num_links < 0) {
                throw new CommunicatorException(String.format("invalid message received; %d; %d", global_node_idx, num_links));
            }
            int node_idx = this.getLocalNodeIdxFromGlobalIdx(global_node_idx);
            Node node = world_to_update.getNodes().get(node_idx);
            if (node == null) {
                throw new IndexOutOfBoundsException("no node with index " + node_idx);
            }
            for (int link_message_idx = 0; link_message_idx < num_links; link_message_idx++) {
                byte link_idx = bytes[offset];
                offset += 1;
                int num_agents = Helper.intFromByteArray(bytes, offset);
                offset += 4;
                if (link_idx < 0 || num_agents < 0) {
                    throw new CommunicatorException(String.format("invalid message received; %d; %d", link_idx, num_agents));
                }
                Agent firstAgent = null;
                for (int idx = 0; idx < num_agents; idx++) {
                    Agent currentAgent = Agent.deserializeFromBytes(bytes, offset);
                    offset += currentAgent.byteLength();
                    if (firstAgent == null) {
                        firstAgent = currentAgent;
                    }
                    try {
                        node.getIncomingLink(link_idx).add(currentAgent, t);
                    }
                    catch (NodeException|LinkException e) {
                        throw new CommunicatorException(String.format(
                                "trying to transfer agent %s%s from rank %d to %d, global node %s(%s), local node %s(%s), incoming link idx %d failed: %s",
                                currentAgent.getId(),
                                currentAgent.getPlan().toString(),
                                rank,
                                this.getMyRank(),
                                global_node_idx,
                                complete_world.getNodes().get(global_node_idx).toString(),
                                node_idx,
                                node.toString(),
                                link_idx,
                                e.getMessage()
                        ));
                    }
                }
//                System.out.println(String.format(
//                    "transfered %d agents (first:%s,tt:%d,lt:%d) from rank %d to %d (node %d global, %d local, incoming link %d)",
//                    num_agents,
//                    firstAgent.getId(),
//                    firstAgent.current_travel_time,
//                    firstAgent.time_to_pass_link,
//                    rank,
//                    this.getMyRank(),
//                    global_node_idx,
//                    node_idx,
//                    link_idx
//                ));
            }
        }
    }

    public void communicateAgents(World world_to_update, World complete_world, int t) throws InterruptedException, ExceedingBufferException, CommunicatorException, NodeException, MPIException {
        List<Request> send_requests = new LinkedList<>();
        int my_rank = this.getMyRank();
        int num_ranks = this.getNumberOfRanks();
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            send_requests.add(this.sendAgentsNB(rank));
        }
        for (int rank = 0; rank < num_ranks; rank++) {
            if (rank == my_rank) {
                continue;
            }
            this.updateWorldFromRank(rank, world_to_update, complete_world, t);
        }
        this.waitAll(send_requests);
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
            getHashCode(local_idx, local_rank),
            global_idx
        );
        assert this.global_node_idx_by_local_idx_and_rank.get(
            getHashCode(local_idx, local_rank)
        ) == global_idx;
    }

    public int getLocalNodeIdxFromGlobalIdx(int global_idx) {
        Integer result = this.local_node_idx_by_global_idx.get(global_idx);
        if (result == null) {
            return -1;
        }
        return result;
    }

    private static int getHashCode(int local_idx, int local_rank) {
        return Arrays.deepHashCode(new Integer[]{local_idx, local_rank});
    }

    public int getGlobalNodeIdxFromLocalIdx(int local_idx, int local_rank) {
        Integer result = this.global_node_idx_by_local_idx_and_rank.get(getHashCode(local_idx, local_rank));
        if (result == null) {
            return -1;
        }
        return result;
    }

    //this is rather inefficient and should only be used for testing
    public int getRankFromGlobalIdx(int global_idx) throws CommunicatorException, MPIException {
        int local_idx = this.getLocalNodeIdxFromGlobalIdx(global_idx);
        for (int rank=0; rank < this.getNumberOfRanks(); rank++) {
            int stored_global_node_idx = this.getGlobalNodeIdxFromLocalIdx(local_idx, rank);
            if (stored_global_node_idx == global_idx) {
                return rank;
            }
//            if (stored_global_node_idx != -1) {
//                throw new CommunicatorException(String.format(
//                    "invalid mapping: %d->%d->%d (rank %d)",
//                    global_idx,
//                    local_idx,
//                    stored_global_node_idx,
//                    rank
//                ));
//            }
        }
        throw new CommunicatorException("no rank assigned for global idx " + global_idx);
    }

    public void validateDecomposition(World world, World complete_world) throws CommunicatorException, NodeException, MPIException {
        for (int idx=0; idx < complete_world.getNodes().size(); idx++) {
            int local_idx = this.getLocalNodeIdxFromGlobalIdx(idx);
            if (local_idx == -1) {
                throw new CommunicatorException("Global index " + idx + " has no local mapping");
            }
            int local_rank = this.getRankFromGlobalIdx(idx);
            if (local_rank == this.getMyRank()) {
                Node global_node = complete_world.getNodes().get(idx);
                Node local_node = world.getNodes().get(local_idx);
                if (global_node.getIncomingLinks().size() != local_node.getIncomingLinks().size()) {
                    throw new CommunicatorException(String.format(
                        "not same number of links for global node %d vs local node %d",
                        idx,
                        local_idx
                    ));
                }
                for (byte link_idx=0; link_idx < global_node.getIncomingLinks().size(); link_idx++) {
                    Link global_link = global_node.getIncomingLink(link_idx);
                    Link local_link = local_node.getIncomingLink(link_idx);
                    if (
                        !global_link.getId().equals(local_link.getId())
                    || global_link.getFreeFlowTravelTime() != local_link.getFreeFlowTravelTime()
                    ) {
                        throw new CommunicatorException(
                            "global and local links not the same: "
                            + global_link.toString()
                            + " vs "
                            + local_link.toString()
                        );
                    }
                }
            }
        }
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
        if (requests.size() == 0) {
            return;
        }
        Request.waitAll(requests.toArray(new Request[requests.size()]));
        for (Request req : requests) {
            req.free();
        }
    }

    public byte[] receive(int rank, int tag) throws MPIException {
        Status status = MPI.COMM_WORLD.recv(this.receive_buffer, buffer_size, MPI.BYTE, rank, tag);
        byte[] bytes = new byte[status.getCount(MPI.BYTE)];
        this.receive_buffer.position(0);
        this.receive_buffer.get(bytes);
        return bytes;
    }

    public Request nonBlockingSend(byte[] bytes, int rank, int tag) throws MPIException {
        ByteBuffer byteBuffer = MPI.newByteBuffer(bytes.length);
        byteBuffer.put(bytes);
        return MPI.COMM_WORLD.iSend(byteBuffer, bytes.length, MPI.BYTE, rank, tag);
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

    public int getNumberOfRanks() throws  MPIException {
        return this.getNumberOfRanks(true);
    }

    public int getNumberOfRanks(boolean allow_override) throws MPIException {
        if (allow_override && num_ranks_override > 0) {
            return num_ranks_override;
        }
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
