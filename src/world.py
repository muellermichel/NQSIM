class World(object):
	def __init__(self, nodes, outgoing_link_ids_by_node_index={}):
		self.nodes = nodes
		links_by_id = {l.id:l for l in sum((n.incoming_links for n in nodes), [])}
		for node_index, link_ids in outgoing_link_ids_by_node_index.items():
			for link_id in link_ids:
				self.nodes[node_index].add_outgoing_link(links_by_id[link_id])

	def __repr__(self):
		outgoing_link_ids_by_node_index = {c:[l.id for l in n.outgoing_links] for c,n in enumerate(self.nodes)}
		return "%s(%r, %r)" %(self.__class__.__name__, self.nodes, outgoing_link_ids_by_node_index)

	def tick(self, delta_t):
		for node in self.nodes:
			node.tick(delta_t)
		for node in self.nodes:
			node.route()