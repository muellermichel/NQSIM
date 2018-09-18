package ch.ethz.systems.nqsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ThreeNodesTest {
    private List<Node> nodes;

    void setUp() {
        Link link1 = new Link(100, 10);
        Link link2 = new Link(100, 10);
        Node node1 = new Node();
        Node node2 = new Node();
        Node node3 = new Node();
        node1.addOutgoingLink(link1); //0->0=link1
        node2.addIncomingLink(link1); //1<-0=link1
        node2.addOutgoingLink(link2); //1->0=link2
        node3.addIncomingLink(link2); //2<-0=link2
        this.nodes = new ArrayList<>(Arrays.asList(node1, node2, node3));
    }

    void testSingleAgent() throws NodeException, LinkException, InterruptedException, ExceedingBufferException, CommunicatorException {
        Agent agent = new Agent(new Plan(new byte[] {(byte) 0}));
        this.nodes.get(0).getOutgoingLink((byte)0).add(agent);
        World world = new World(this.nodes);
        for (int time = 0; time < 9; time++) {
            world.tick(1);
        }
        assert agent.current_travel_time == 9;
        assert agent.time_to_pass_link == 10;
        assert this.nodes.get(0).outgoingQueueLength((byte) 0) == 1;
        world.tick(1);
        assert agent.current_travel_time == 0;
        assert agent.time_to_pass_link == 10;
        assert this.nodes.get(0).outgoingQueueLength((byte) 0) == 0;
        assert this.nodes.get(1).outgoingQueueLength((byte) 0) == 1;
        for (int time = 0; time < 9; time++) {
            world.tick(1);
        }
        assert agent.current_travel_time == 9;
        assert agent.time_to_pass_link == 10;
        assert this.nodes.get(1).outgoingQueueLength((byte) 0) == 1;
    }
}
