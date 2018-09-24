package ch.ethz.systems.nqsim;

import mpi.*;

import java.nio.ByteBuffer;

public class HelloMpi
{
	public static void main(String args[]) throws Exception
	{
		int me,size;
		args=MPI.Init(args);
        MPI.COMM_WORLD.setErrhandler(MPI.ERRORS_RETURN);

//		me = MPI.COMM_WORLD.Rank();
//		size = MPI.COMM_WORLD.Size();
//		System.out.println(MPI.Get_processor_name()+": Hello World from "+me+" of "+size);

		me = MPI.COMM_WORLD.getRank();
		size = MPI.COMM_WORLD.getSize();
		System.out.println(MPI.getProcessorName()+": Hello World from "+me+" of "+size);

		if (me == 0) {
			byte[] bytes = new byte[1];
			bytes[0] = 9;
//			Request req = MPI.COMM_WORLD.Isend(
//					bytes,
//					0,
//					bytes.length,
//					MPI.BYTE,
//					1,
//					10
//			);
            ByteBuffer byteBuffer = MPI.newByteBuffer(1);
            byteBuffer.put(bytes);
            Request req = MPI.COMM_WORLD.isSend(byteBuffer, 0, MPI.BYTE, 1, 10);
//			while(true) {
//			    Status status = req.Test();
//			    if (status != null) {
////                    System.out.println(status.Get_count(MPI.BYTE) + " elements sent");
//                    System.out.println(status.Get_count(MPI.BYTE) + " elements sent");
//                    break;
//                }
//                Thread.sleep(5);
//            }
            req.waitFor();
            req.free();
		}
		else if (me == 1) {
			byte[] bytes = new byte[1];
//			Status status = MPI.COMM_WORLD.Recv(bytes, 0, bytes.length, MPI.BYTE, 0, 10);
            Status status = MPI.COMM_WORLD.recv(bytes, 0, MPI.BYTE, 0, 10);
			System.out.println(String.format(
				"rank %d has received %d",
				me,
				bytes[0]
			));
		}
		MPI.Finalize();
	}
}
