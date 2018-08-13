from collections import deque

class Agent(object):
	def __init__(self, plan=[], current_travel_time=0, time_to_pass_link=0):
		self.plan = deque(plan)
		self.plan_copy = list(plan)
		self.current_travel_time = current_travel_time
		self.time_to_pass_link = time_to_pass_link

	def __repr__(self):
		return "%s(%r, %i, %i)" %(self.__class__.__name__, self.plan, self.current_travel_time, self.time_to_pass_link)

	def __str__(self):
		return "ag{o:%r; c:%s; t:%i}" %(self.plan_copy, self.plan, self.current_travel_time)

	def tick(self, delta_t):
		self.current_travel_time += delta_t