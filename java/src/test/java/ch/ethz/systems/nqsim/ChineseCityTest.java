package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ListIterator;

public final class ChineseCityTest {
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

    public static void checkAgainstReference(World world, String filename) throws IOException {
        ObjectMapper om = new ObjectMapper();
        ObjectReader worldReader = om.readerFor(World.class);
        File reference_file = new File(filename);
        InputStream reference_is = new FileInputStream(reference_file);
        World reference_world = World.fromJson(IOUtils.toByteArray(reference_is), worldReader);
        compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.getLength() == ref_link.getLength() :
                    String.format("node %d, link%d(%s): %d vs. %d length",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.getLength(),
                            ref_link.getLength()
                    );
        });
        compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.getFreeFlowVelocity() == ref_link.getFreeFlowVelocity() :
                    String.format("node %d, link%d(%s): %d vs. %d ff velocity",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.getFreeFlowVelocity(),
                            ref_link.getFreeFlowVelocity()
                    );
        });
        compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
            assert link.getFreeFlowTravelTime() == ref_link.getFreeFlowTravelTime() :
                    String.format("node %d, link%d(%s): %d vs. %d ff ttime",
                            node_idx,
                            link_idx,
                            link.getId(),
                            link.getFreeFlowTravelTime(),
                            ref_link.getFreeFlowTravelTime()
                    );
        });
        compareAllLinks(world, reference_world, (Link link, Link ref_link, int node_idx, int link_idx) -> {
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

    public static void main(String[] args) throws IOException, NodeException {
        ObjectMapper om = new ObjectMapper();
        ObjectReader worldReader = om.readerFor(World.class);

        File file = new File("chinese_capital_3M_187x187.json");
        InputStream is = new FileInputStream(file);
        World world = World.fromJson(IOUtils.toByteArray(is), worldReader);

        for (int time=0; time < 50; time += 1) {
            world.tick(1);
        }
        checkAgainstReference(world,"chinese_capital_3M_187x187_result.json");
    }
}
