import logging

class World(object):
	def __init__(self, nodes, outgoing_link_ids_by_node_index={}):
		self.__setstate__({
			"nodes": nodes,
			"outgoing_link_ids_by_node_index": outgoing_link_ids_by_node_index,
			"t": 0
		})

	def __repr__(self):
		return "%s(%r, %r)" %(self.__class__.__name__, self.nodes, self.outgoing_link_ids_by_node_index)

	def __getstate__(self):
		return {
			"nodes": self.nodes,
			"outgoing_link_ids_by_node_index": self.outgoing_link_ids_by_node_index,
			"t": self.t
		}

	def __setstate__(self, state):
		self.nodes = state["nodes"]
		self.t = state["t"]
		self.setup_outgoing_links(state["outgoing_link_ids_by_node_index"])

	@property
	def outgoing_link_ids_by_node_index(self):
		return {c:[l.id for l in n.outgoing_links] for c,n in enumerate(self.nodes)}

	def setup_outgoing_links(self, outgoing_link_ids_by_node_index):
		links_by_id = {l.id:l for l in sum((n.incoming_links for n in self.nodes), [])}
		for node_index, link_ids in outgoing_link_ids_by_node_index.items():
			for link_id in link_ids:
				self.nodes[int(node_index)].add_outgoing_link(links_by_id[link_id])

	def tick(self, delta_t):
		for node in self.nodes:
			node.tick(delta_t)
		for node in self.nodes:
			node.route()
		self.t += delta_t
		logging.info("time=%is" %(self.t))

	def print(self, nodes_per_row=5):
		import sys, colors
		sys.stderr.write("======================================================\n")
		sys.stderr.write(colors.blue("world@t=%i\n" %(self.t)))
		string_representations = []
		max_incoming_agents = 0
		for idx, node in enumerate(self.nodes):
			num_incoming_agents = 0
			for link in node.incoming_links:
				num_incoming_agents += len(link)
			if num_incoming_agents > max_incoming_agents:
				max_incoming_agents = num_incoming_agents
		sys.stderr.write(colors.blue("max incoming=%i\n" %(max_incoming_agents)))
		for idx, node in enumerate(self.nodes):
			num_incoming_agents = 0
			for link in node.incoming_links:
				num_incoming_agents += len(link)
			total_jam_capacity = sum(l.jam_capacity for l in node.outgoing_links)
			total_freeflow_capacity = sum(l.free_flow_capacity for l in node.outgoing_links)
			color_component = 0
			if num_incoming_agents <= total_freeflow_capacity:
				color_component = int((num_incoming_agents / total_freeflow_capacity) * 5) + 28
			else:
				color_component = - int(((num_incoming_agents - total_freeflow_capacity) / (total_jam_capacity - total_freeflow_capacity)) * 5) + 129
			string_representations.append(
				colors.color(
					"(%s):%s" %(
						str(idx).zfill(len(str(len(self.nodes)))),
						str(num_incoming_agents).zfill(len(str(max_incoming_agents)))
					),
					color_component
				)
			)
		rows = []
		for c in range(0, len(self.nodes), nodes_per_row):
			rows.append(string_representations[c:c+nodes_per_row])
		for r in rows:
			sys.stderr.write(", ".join(r))
			sys.stderr.write("\n")
		sys.stderr.write("======================================================\n")
		sys.stderr.flush()

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