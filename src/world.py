class World(object):
	def __init__(self, nodes):
		self.nodes = nodes

	def __repr__(self):
		return "%s(%r, %r)" %(self.__class__.__name__, self.nodes)

	def tick(self, delta_t):
		for node in self.nodes:
			node.tick(delta_t)
		for node in self.nodes:
			node.route()