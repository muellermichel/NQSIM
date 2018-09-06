import random, math, logging
from collections import deque
from node import Node
from link import Link, LinkException
from agent import Agent

class ChineseCapitalException(Exception):
	pass

class ReducedRandom(object):
	def __init__(self, randomness):
		self.random_numbers = [random.random() for _ in range(randomness)]
		self.next_idx = 0
		self.randomness = randomness

	def random(self):
		result = self.random_numbers[self.next_idx]
		self.next_idx += 1
		if self.next_idx == self.randomness:
			self.next_idx = 0
		return result

#generate a chessboard-like city
class ChineseCapital(object):
	def __init__(self, edge_length=8, link_length=100):
		def get_link_board_horicontal(suffix):
			return [[
				Link("r%ic%i-%s" %(r, c, suffix), link_length, free_flow_velocity=random.choice(velocities_kmh)*0.277)
				for c in range(edge_length - 1)
			] for r in range(edge_length)]

		def get_link_board_vertical(suffix):
			return [[
				Link("r%ic%i-%s" %(r, c, suffix), link_length, free_flow_velocity=random.choice(velocities_kmh)*0.277)
				for c in range(edge_length)
			] for r in range(edge_length - 1)]

		node_board = [[Node() for _ in range(edge_length)] for _ in range(edge_length)]
		velocities_kmh = [30,40,50,60,80,100]
		logging.info("setting up link boards")
		link_board_horicontal = get_link_board_horicontal(">")
		backlink_board_horicontal = get_link_board_horicontal("<")
		link_board_vertical = get_link_board_vertical("^")
		backlink_board_vertical = get_link_board_vertical("v")

		logging.info("setting up nodes")
		for row_index, row in enumerate(node_board):
			for col_index, node in enumerate(row):
				if row_index > 0:
					node.add_incoming_link(link_board_vertical[row_index-1][col_index])
					node.add_outgoing_link(backlink_board_vertical[row_index-1][col_index])
				if col_index > 0:
					node.add_incoming_link(link_board_horicontal[row_index][col_index-1])
					node.add_outgoing_link(backlink_board_horicontal[row_index][col_index-1])
				if row_index < edge_length - 1:
					node.add_outgoing_link(link_board_vertical[row_index][col_index])
					node.add_incoming_link(backlink_board_vertical[row_index][col_index])
				if col_index < edge_length - 1:
					node.add_outgoing_link(link_board_horicontal[row_index][col_index])
					node.add_incoming_link(backlink_board_horicontal[row_index][col_index])
			logging.info("%3.2fpercent" %(row_index / edge_length*100))

		logging.info("setting up randomness")
		self.random = ReducedRandom(edge_length*edge_length*2)
		self.node_board = node_board
		self.link_board_horicontal = link_board_horicontal
		self.backlink_board_horicontal = backlink_board_horicontal
		self.link_board_vertical = link_board_vertical
		self.backlink_board_vertical = backlink_board_vertical
		self.agents = []
		self.edge_length = edge_length

	def add_agents(self, num_agents):
		def next_horicontal(curr_row_id, curr_col_id, end_col_id):
			if curr_col_id < end_col_id:
				return 1, "r%ic%i->" %(curr_row_id, curr_col_id)
			return -1, "r%ic%i-<" %(curr_row_id, curr_col_id - 1)

		def next_vertical(curr_row_id, curr_col_id, end_row_id):
			if curr_row_id < end_row_id:
				return 1, "r%ic%i-^" %(curr_row_id, curr_col_id)
			return -1, "r%ic%i-v" %(curr_row_id - 1, curr_col_id)

		def make_plan(start_row_id, start_col_id, end_row_id, end_col_id):
			curr_row_id = start_row_id
			curr_col_id = start_col_id
			plan = []
			while curr_row_id != end_row_id or curr_col_id != end_col_id:
				if curr_row_id == end_row_id:
					change, link_id = next_horicontal(curr_row_id, curr_col_id, end_col_id)
					curr_col_id += change
					plan.append(link_id)
					continue
				if curr_col_id == end_col_id:
					change, link_id = next_vertical(curr_row_id, curr_col_id, end_row_id)
					curr_row_id += change
					plan.append(link_id)
					continue
				if self.random.random() < 0.5:
					change, link_id = next_horicontal(curr_row_id, curr_col_id, end_col_id)
					curr_col_id += change
					plan.append(link_id)
					continue
				change, link_id = next_vertical(curr_row_id, curr_col_id, end_row_id)
				curr_row_id += change
				plan.append(link_id)
			return plan

		links_by_id = {}
		logging.info("gathering links")
		for idx, row in enumerate(self.node_board):
			for node in row:
				for link in node.incoming_links:
					links_by_id[link.id] = link
			if idx % (len(self.node_board) / 100) == 0:
				logging.info("%3.2fpercent" %(idx / len(self.node_board)*100))
		logging.info("making agent plans")
		total_num_link_exceptions = 0
		for num in range(num_agents):
			num_link_exceptions = 0
			while True:
				start_row_id = int(self.edge_length * self.random.random())
				start_col_id = int(self.edge_length * self.random.random())
				end_row_id = int(self.edge_length * self.random.random())
				end_col_id = int(self.edge_length * self.random.random())
				plan = deque(make_plan(start_row_id, start_col_id, end_row_id, end_col_id))
				if len(plan) == 0:
					continue
				first_link = links_by_id[plan.popleft()]
				try:
					first_link.add(Agent(plan, identifier=num))
				except LinkException:
					num_link_exceptions += 1
					total_num_link_exceptions += 1
					if num_link_exceptions > 9:
						raise ChineseCapitalException(
							"reduce agents or increase capacity - for agent %i at least 10 attempts to find a free link failed" %(num)
						)
				break
			if num % max(math.floor(num_agents / 100), 1) == 0:
				logging.info("%3.2fpercent" %(num / num_agents*100))
		logging.info("agent plans done; number of link exceptions that lead to rerolls: %i" %(total_num_link_exceptions))


