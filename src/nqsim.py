import logging, random
from node import NodeException
from world import World
from chinese_capital import ChineseCapital

def write_snapshot(time, world):
	logging.info("saving snapshot")
	with open("snapshot_%i" %(time),"w") as f:
		f.write(repr(world))
	logging.info("saving done")

def load_from_snapshot(time):
	logging.info("loading from snapshot")
	from agent import Agent
	from link import Link
	from node import Node
	from collections import deque
	with open("snapshot_%i" %(time),"r") as f:
		return eval(f.read())
	logging.info("loading done")

logging.basicConfig(level=logging.WARN)
random.seed(1)
try:
	# chinese_capital = ChineseCapital(50)
	chinese_capital = ChineseCapital(20, link_length=500)
	world = World(sum(chinese_capital.node_board, []))
	chinese_capital.add_agents(100000)
	# write_snapshot(0, world)
	# world = load_from_snapshot(0)
	for time in range(0,10000,100):
		# world.plot()
		world.print(nodes_per_row=20)
		# write_snapshot(time, world)
		world.tick(10)
	# world.plot()
	write_snapshot(time, world)
except NodeException as e:
	print(e)
	print(world)



