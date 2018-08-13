import unittest, logging
from collections import deque

from world import World
from node import Node
from link import Link
from agent import Agent
from chinese_capital import ChineseCapital

class TestNQSIMTwoNodes(unittest.TestCase):
	def setUp(self):
		link = Link(0, 100, 10)
		self.nodes = [Node([], [link]), Node([link], [])]

	def test_single_agent(self):
		agent = Agent()
		self.nodes[0].outgoing_links[0].add(agent)
		world = World(self.nodes)
		for time in range(9):
			world.tick(1)
		self.assertEqual(agent.current_travel_time, 9)
		self.assertEqual(agent.time_to_pass_link, 10)
		self.assertEqual(len(self.nodes[0].outgoing_links[0]), 1)
		world.tick(1)
		self.assertEqual(agent.current_travel_time, 0)
		self.assertEqual(len(self.nodes[0].outgoing_links[0]), 0)

	def test_full_capacity(self):
		for _ in range(10):
			self.nodes[0].outgoing_links[0].add(Agent())
		self.assertEqual(self.nodes[0].outgoing_links[0].q[-1].time_to_pass_link, 10)
		self.nodes[0].outgoing_links[0].add(Agent())
		self.assertEqual(self.nodes[0].outgoing_links[0].q[-1].time_to_pass_link, 20)
		for _ in range(9):
			self.nodes[0].outgoing_links[0].add(Agent())
		self.assertFalse(self.nodes[0].outgoing_links[0].is_accepting)

class TestNQSIMThreeNodes(unittest.TestCase):
	def setUp(self):
		link1 = Link(1, 100, 10)
		link2 = Link(2, 100, 10)
		self.nodes = [Node([], [link1]), Node([link1], [link2]), Node([link2], [])]

	def test_single_agent(self):
		agent = Agent([2])
		self.nodes[0].outgoing_links[0].add(agent)
		world = World(self.nodes)
		for time in range(9):
			world.tick(1)
		self.assertEqual(agent.current_travel_time, 9)
		self.assertEqual(agent.time_to_pass_link, 10)
		self.assertEqual(len(self.nodes[0].outgoing_links[0]), 1)
		world.tick(1)
		self.assertEqual(agent.current_travel_time, 0)
		self.assertEqual(len(self.nodes[0].outgoing_links[0]), 0)
		self.assertEqual(len(self.nodes[1].outgoing_links[0]), 1)
		for time in range(9):
			world.tick(1)
		self.assertEqual(agent.current_travel_time, 9)
		self.assertEqual(agent.time_to_pass_link, 10)
		self.assertEqual(len(self.nodes[1].outgoing_links[0]), 1)
		world_copy = eval(repr(world))
		agent_copy = world_copy.nodes[1].outgoing_links[0].q[0]
		self.assertEqual(agent_copy.current_travel_time, 9)
		self.assertEqual(agent_copy.time_to_pass_link, 10)
		self.assertEqual(len(world_copy.nodes[1].outgoing_links[0]), 1)
		world.tick(1)
		self.assertEqual(agent.current_travel_time, 0)
		self.assertEqual(len(self.nodes[1].outgoing_links[0]), 0)
		world_copy.tick(1)
		self.assertEqual(agent_copy.current_travel_time, 0)
		self.assertEqual(len(world_copy.nodes[1].outgoing_links[0]), 0)

class TestChineseCapital(unittest.TestCase):
	def test_repr(self):
		chinese_capital = ChineseCapital(2)
		world = World(sum(chinese_capital.node_board, []))
		chinese_capital.add_agents(1)
		eval(repr(world))

if __name__ == '__main__':
	logging.basicConfig(level=logging.INFO)
	unittest.main()