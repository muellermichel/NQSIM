package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class World {
    private int t;
    private List<Node> nodes;

    public static World fromJson(byte[] jsonData, ObjectReader or) throws IOException {
        return or.readValue(jsonData);
    }

    @JsonCreator
    public World(
            @JsonProperty("plan") List<Node> nodes,
            @JsonProperty("t") int t,
            @JsonProperty("outgoing_link_ids_by_node_index") Map<Integer, String> outgoing_link_ids_by_node_index
    ) {
        this.nodes = nodes;
        this.t = t;
    }

    public World(List<Node> nodes) {
        this(nodes, 0);
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void tick(int delta_t) throws NodeException {
        for (Node node:this.nodes) {
            node.tick(delta_t);
        }
        ListIterator<Node> node_iterator = this.nodes.listIterator();
        while (node_iterator.hasNext()) {
            int idx = node_iterator.nextIndex();
            Node node = node_iterator.next();
            try {
                node.route(idx);
            }
            catch (NodeException e) {
                throw new NodeException(String.format(
                    "node %d: %s",
                    idx,
                    e.getMessage()
                ));
            }
        }
    }
}
