package ch.ethz.systems.nqsim;

// import org.junit.jupiter.api.Test;

import mpi.MPIException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import static org.junit.jupiter.api.Assertions.*;

public final class TwoNodesTest {
    private List<Node> nodes;

//    @org.junit.jupiter.api.BeforeEach
    void setUp() throws LinkException {
        Link link = new Link(100, 10);
        Node node1 = new Node();
        Node node2 = new Node();
        this.nodes = new ArrayList<>(Arrays.asList(node1, node2));
        node1.addOutgoingLink(link);
        node2.addIncomingLink(link);
    }

//    @Test
    void testSingleAgent() throws NodeException, WorldException, InterruptedException, ExceedingBufferException, CommunicatorException, MPIException {
        Agent agent = new Agent(new Plan(new byte[0]));
        this.nodes.get(1).addAgent(agent, (byte) 0);
        World world = new World(this.nodes);
        for (int time = 0; time < 9; time++) {
            world.tick(1);
        }
        assert agent.current_travel_time == 9;
        assert agent.time_to_pass_link == 10;
        assert this.nodes.get(0).outgoingQueueLength((byte) 0) == 1;
        world.tick(1);
        assert agent.current_travel_time == 0;
        assert this.nodes.get(0).outgoingQueueLength((byte) 0) == 0;
    }

    void testFullCapacity() throws NodeException, LinkException {
        for (int time = 0; time < 10; time++) {
            this.nodes.get(0).getOutgoingLink((byte)0).add(new Agent(new Plan(new byte[0])));
        }
        int queueLength = this.nodes.get(0).outgoingQueueLength((byte)0);
        assert this.nodes.get(0).getOutgoingLink((byte)0).get(
                queueLength - 1
        ).time_to_pass_link == 10;
        this.nodes.get(0).getOutgoingLink((byte)0).add(new Agent(new Plan(new byte[0])));
        int queueLength2 = this.nodes.get(0).outgoingQueueLength((byte)0);
        int time_to_pass = this.nodes.get(0).getOutgoingLink((byte)0).get(
                queueLength2 - 1
        ).time_to_pass_link;
        assert time_to_pass == 20 : time_to_pass;
        for (int time = 0; time < 9; time++) {
            this.nodes.get(0).getOutgoingLink((byte)0).add(new Agent(new Plan(new byte[0])));
        }
        this.nodes.get(0).computeCapacities();
        this.nodes.get(1).computeCapacities();
        assert !this.nodes.get(0).getOutgoingLink((byte)0).isAccepting();
    }
}