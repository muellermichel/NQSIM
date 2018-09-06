package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.*;

public final class World {
    private int t;
    private List<Node> nodes;

    public static World fromJson(byte[] jsonData, ObjectReader or) throws IOException {
        return or.readValue(jsonData);
    }

    @JsonCreator
    public World(
            @JsonProperty("nodes") List<Node> nodes,
            @JsonProperty("t") int t,
            @JsonProperty("outgoing_link_ids_by_node_index") Map<String, List<String>> outgoing_link_ids_by_node_index
    ) {
        this.nodes = nodes;
        this.t = t;

        Map<String, Link> links_by_id = new HashMap<>();
        for (Node n:this.nodes) {
            for (Link l:n.getIncomingLinks()) {
                links_by_id.put(l.getId(), l);
            }
        }
        for (Map.Entry<String, List<String>> entry : outgoing_link_ids_by_node_index.entrySet()) {
            int node_index = Integer.parseInt(entry.getKey());
            List<String> link_ids = entry.getValue();
            for (String link_id:link_ids) {
                this.nodes.get(node_index).addOutgoingLink(links_by_id.get(link_id));
            }
        }
    }

    public World(List<Node> nodes) {
        this(nodes, 0, new HashMap<>());
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void tick(int delta_t) throws NodeException {
        long start = System.currentTimeMillis();
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
        this.t += 1;
        long time = System.currentTimeMillis() - start;
        System.out.println(String.format(
                "time=%ds,real for %ds:%6.4fs,s/r:%6.4f",
                this.t,
                delta_t,
                time / (double)1000,
                delta_t / (time / (double)1000)
        ));
    }
}
