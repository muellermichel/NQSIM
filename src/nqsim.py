import logging, random
import jsonpickle
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

logging.basicConfig(level=logging.INFO)
random.seed(1)
try:
	# chinese_capital = ChineseCapital(50)
	logging.info("loading chinese capital")
	chinese_capital = ChineseCapital(187, link_length=1000)
	logging.info("constructing world")
	world = World(sum(chinese_capital.node_board, []))
	logging.info("adding agents")
	# chinese_capital.add_agents(3000000)

	logging.info("writing json file")
	oneway = jsonpickle.encode(
		world.world_with_numerically_routed_agents(),
		unpicklable=False
	)
	with open("chinese_capital.json", "w") as f:
		f.write(oneway)

	# logging.info("running sim")
	# world.tick(1)

	# # logging.info("writing json file")
	# # oneway = jsonpickle.encode(
	# # 	world.world_with_numerically_routed_agents(),
	# # 	unpicklable=False
	# # )
	# # with open("chinese_capital_t1.json", "w") as f:
	# # 	f.write(oneway)

	# # write_snapshot(0, world)
	# # world = load_from_snapshot(0)
	# for time in range(0,49,1):
	# 	# world.plot()
	# 	# world.print(nodes_per_row=20)
	# 	# write_snapshot(time, world)
	# 	world.tick(1)
	# # world.plot()
	# # write_snapshot(time, world)

	# logging.info("writing result json file")
	# oneway = jsonpickle.encode(
	# 	world.world_with_numerically_routed_agents(),
	# 	unpicklable=False
	# )
	# with open("chinese_capital_result.json", "w") as f:
	# 	f.write(oneway)

except NodeException as e:
	print(e)
	print(world)



