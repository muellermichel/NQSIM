from world import World
from node import Node
from link import Link
from agent import Agent

world = World(
	agents=[
		Agent(), Agent(), Agent()
	],
	nodes=[]
)

for time in range(100):
	logging.message("time: %i" %(time))
	world.tick(1)

print(world)