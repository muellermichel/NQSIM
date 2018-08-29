package ch.ethz.systems.nqsim;

import java.util.List;
import java.util.ListIterator;

public final class World {
    private int t;
    private List<Node> nodes;

    public World(List<Node> nodes) {
        this.nodes = nodes;
        this.t = 0;
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
