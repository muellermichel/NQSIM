import logging

class NodeException(Exception):
	pass

class Node(object):
	def __init__(self, incoming_links=[], outgoing_links=[]):
		self.incoming_links = list(incoming_links)
		self.outgoing_links = list(outgoing_links)
		self.outgoing_links_by_identifier = {l.id:l for l in outgoing_links}

	def __str__(self):
		return "%s:%r" %(self.__class__.__name__, self.__dict__)

	def __repr__(self):
		return "%s(%r)" %(
			self.__class__.__name__,
			self.incoming_links
		)

	def add_incoming_link(self, link):
		self.incoming_links.append(link)

	def add_outgoing_link(self, link):
		self.outgoing_links.append(link)
		self.outgoing_links_by_identifier[link.id] = link

	def tick(self, delta_t):
		for link in self.incoming_links:
			link.tick(delta_t)

	def route(self):
		for link in self.incoming_links:
			current_agent = link.peak()
			while current_agent and current_agent.current_travel_time >= current_agent.time_to_pass_link:
				next_link_id = None
				if current_agent.plan:
					next_link_id = str(current_agent.plan.popleft())
					next_link = self.outgoing_links_by_identifier.get(next_link_id)
					if not next_link:
						raise NodeException("invalid plan for agent %s: link %s not available on node; outgoing: %s" %(
							current_agent,
							next_link_id,
							[str(l) for l in self.outgoing_links]
						))
					if not next_link.is_accepting:
						break
					next_link.add(current_agent)
				logging.debug("agent with plan %s has crossed over from link %s to %s" %(
					current_agent.plan,
					link.id,
					next_link_id
				))
				link.remove_first_waiting()
				current_agent = link.peak()
