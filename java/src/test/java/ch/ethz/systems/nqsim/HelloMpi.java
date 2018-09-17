package ch.ethz.systems.nqsim;

import mpi.*;
public class HelloMpi
{
	public static void main(String args[]) throws Exception
	{
		int me,size;
		args=MPI.Init(args);
		me = MPI.COMM_WORLD.Rank();
		size = MPI.COMM_WORLD.Size();
		System.out.println(MPI.Get_processor_name()+": Hello World from "+me+" of "+size);
		MPI.Finalize();
	}
}
