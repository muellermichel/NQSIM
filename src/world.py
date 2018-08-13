import logging

class World(object):
	def __init__(self, nodes, outgoing_link_ids_by_node_index={}):
		self.nodes = nodes
		links_by_id = {l.id:l for l in sum((n.incoming_links for n in nodes), [])}
		for node_index, link_ids in outgoing_link_ids_by_node_index.items():
			for link_id in link_ids:
				self.nodes[node_index].add_outgoing_link(links_by_id[link_id])
		self.t = 0

	def __repr__(self):
		outgoing_link_ids_by_node_index = {c:[l.id for l in n.outgoing_links] for c,n in enumerate(self.nodes)}
		return "%s(%r, %r)" %(self.__class__.__name__, self.nodes, outgoing_link_ids_by_node_index)

	def tick(self, delta_t):
		for node in self.nodes:
			node.tick(delta_t)
		for node in self.nodes:
			node.route()
		self.t += delta_t
		logging.info("time=%is" %(self.t))

	def plot(self):
		import pydot
		graph = pydot.Dot(graph_type='digraph', splines='ortho', label="t=%i" %(self.t))
		for index, node in enumerate(self.nodes):
			graph.add_node(pydot.Node(index))
		startnode_by_link_id = {}
		endnode_by_link_id = {}
		links_by_id = {}
		for node_index, node in enumerate(self.nodes):
			for l in node.outgoing_links:
				startnode_by_link_id[l.id] = node_index
			for l in node.incoming_links:
				endnode_by_link_id[l.id] = node_index
				links_by_id[l.id] = l
		for link_id, link in links_by_id.items():
			link_occuppancy = len(link.q)/link.jam_capacity
			color_component = int(255*link_occuppancy)
			weight = max(int(5*link.free_flow_velocity/(120*0.277)),1)
			graph.add_edge(pydot.Edge(
				startnode_by_link_id[link_id],
				endnode_by_link_id[link_id],
				color='#%02x%02x%02x' % (color_component, 0, 0),
				penwidth=weight
			))
		graph.write_png('%i.png' %(self.t))