__precompile__(true)

"""
Main module for NQSIM, a proof of concept for a node-based version of QSIM, later potentially ported or integrated with MATSim
"""

module NQSIM
using DataStructures

q = Queue(Int64)
enqueue!(q, 1)
enqueue!(q, 2)
enqueue!(q, 3)
println("hello world", dequeue!(q), dequeue!(q), q)
end