from collections import deque

class Agent(object):
	def __init__(self, plan=[], current_travel_time=0, time_to_pass_link=0, identifier="unset"):
		self.__setstate__({
			"plan": deque(plan),
			"current_travel_time": current_travel_time,
			"time_to_pass_link": time_to_pass_link,
			"id": str(identifier)
		})

	def __repr__(self):
		return "%s(%r, %i, %i, '%s')" %(
			self.__class__.__name__,
			self.plan,
			self.current_travel_time,
			self.time_to_pass_link,
			self.id
		)

	def __getstate__(self):
		return {
			"plan": self.plan,
			"current_travel_time": self.current_travel_time,
			"time_to_pass_link": self.time_to_pass_link,
			"id": self.id
		}

	def __setstate__(self, state):
		self.__dict__.update(state)
		self.plan_copy = list(self.plan)

	def __str__(self):
		return "ag_%s{o:%r; c:%s; t:%i}" %(self.id, self.plan_copy, self.plan, self.current_travel_time)

	def tick(self, delta_t):
		self.current_travel_time += delta_t