package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.io.IOUtils;

import java.io.*;

public final class JSONTest {
    void testAgentSerialization() throws IOException {
        ObjectMapper om = new ObjectMapper();
        ObjectWriter agentWriter = om.writerFor(Agent.class);
        ObjectReader agentReader = om.readerFor(Agent.class);

        Plan plan = new Plan(new byte[]{(byte) 0, (byte) 0});
        Agent agentBefore = new Agent(String.valueOf(0), plan, 1, 2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        agentBefore.streamAsJson(baos, agentWriter);
        Agent agentAfter = Agent.fromJson(baos.toByteArray(), agentReader);
        assert agentAfter.current_travel_time == 1 : agentAfter.current_travel_time;
        assert agentAfter.time_to_pass_link == 2 : agentAfter.time_to_pass_link;
        assert agentAfter.pollPlan() == (byte) 0;
        assert agentAfter.getPlan().size() == 1;
    }

    void testLoadingFromJSON() throws IOException, NodeException {
        ObjectMapper om = new ObjectMapper();
        ObjectReader worldReader = om.readerFor(World.class);

        File file = new File("test.json");
        InputStream is = new FileInputStream(file);
        World world = World.fromJson(IOUtils.toByteArray(is), worldReader);

        Agent agent = world.getNodes().get(1).getOutgoingLink((byte)0).get(0);
        assert agent.current_travel_time == 9;
        assert agent.time_to_pass_link == 10;
        assert world.getNodes().get(1).getOutgoingLink((byte)0).queueLength() == 1;
        world.tick(1);
        assert agent.current_travel_time == 0;
        assert world.getNodes().get(1).getOutgoingLink((byte)0).queueLength() == 0;
    }
}
