import constants
from collections import deque

class LinkException(Exception):
	pass

class Link(object):
	def __init__(self, identifier, length, free_flow_velocity, queue=[]):
		self.id = str(identifier)
		self.length = length
		self.free_flow_velocity = free_flow_velocity
		self.q = deque(queue)
		self.free_flow_capacity = length / (max(
			constants.JAM_AGENT_LENGTH,
			constants.FREE_FLOW_AGENT_LENGTH_PER_KPH*free_flow_velocity
		))
		self.jam_capacity = length / constants.JAM_AGENT_LENGTH
		self.free_flow_travel_time = length / free_flow_velocity
		self.jam_travel_time = length / constants.JAM_VELOCITY

	def __str__(self):
		return "%s{l%i, cap%i, v%i, n%i}" %(
			self.id,
			self.length,
			self.free_flow_capacity,
			self.free_flow_velocity,
			len(self.q)
		)

	def __repr__(self):
		return "%s('%s', %i, %i, %r)" %(
			self.__class__.__name__,
			self.id,
			self.length,
			self.free_flow_velocity,
			self.q
		)

	def __len__(self):
		return len(self.q)

	def __bool__(self):
		return True

	@property
	def is_accepting(self):
		if len(self.q) < self.free_flow_capacity:
			return True
		if len(self.q) < self.jam_capacity:
			return True
		return False

	def peak(self):
		if len(self.q) == 0:
			return None
		current_agent = self.q[0]
		return current_agent

	def add(self, agent):
		if len(self.q) < self.free_flow_capacity:
			agent.time_to_pass_link = self.free_flow_travel_time
			self.q.append(agent)
			return
		if len(self.q) < self.jam_capacity:
			agent.time_to_pass_link = self.jam_travel_time
			self.q.append(agent)
			return
		raise LinkException("link full")

	def remove_first_waiting(self):
		to_be_removed = self.peak()
		if not to_be_removed:
			raise LinkException("no agent waiting on link %s" %(self.id))
		popped = self.q.popleft()
		if not popped is to_be_removed:
			raise LinkException("this should not happen: peak is not first waiting on link %s" %(self.id))
		popped.current_travel_time = 0
		return popped

	def tick(self, delta_t):
		for agent in self.q:
			agent.tick(delta_t)
