package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JSONTest {
    void testAgentSerialization() throws IOException {
        ObjectMapper om = new ObjectMapper();
        ObjectWriter agentWriter = om.writerFor(Agent.class);
        ObjectReader agentReader = om.readerFor(Agent.class);

        Plan plan = new Plan((byte) 0, (byte) 0);
        Agent agentBefore = new Agent(0, plan, 1, 2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        agentBefore.streamAsJson(baos, agentWriter);
        Agent agentAfter = Agent.fromJson(baos.toByteArray(), agentReader);
        assert agentAfter.current_travel_time == 1 : agentAfter.current_travel_time;
        assert agentAfter.time_to_pass_link == 2 : agentAfter.time_to_pass_link;
        assert agentAfter.pollPlan() == (byte) 0;
        assert agentAfter.getPlan().size() == 1;
    }
}
