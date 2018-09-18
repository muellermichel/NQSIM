package ch.ethz.systems.nqsim;

public final class CapacityMessageIngredients {
    public Node source_node;
    public Link link;
    public int node_index;
    public int outgoing_link_index;

    public CapacityMessageIngredients(Node source_node, Link link, int node_index, int outgoing_link_index) {
        this.source_node = source_node;
        this.link = link;
        this.node_index = node_index;
        this.outgoing_link_index = outgoing_link_index;
    }
}
