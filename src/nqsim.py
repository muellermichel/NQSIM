import logging
from node import NodeException
from world import World
from chinese_capital import ChineseCapital

logging.basicConfig(level=logging.DEBUG)
try:
	chinese_capital = ChineseCapital(3)
	world = World(sum(chinese_capital.node_board, []))
	chinese_capital.add_agents(1000)
	world.plot()
	for _ in range(10):
		world.tick(10)
		world.plot()
except NodeException as e:
	print(e)
	print(world)



