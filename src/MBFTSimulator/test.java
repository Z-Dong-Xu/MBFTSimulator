package MBFTSimulator;

import java.util.PriorityQueue;
import java.util.Queue;

import MBFTSimulator.message.Message;
import MBFTSimulator.message.PrePrepareMsg;
import MBFTSimulator.message.RequestMsg;
import MBFTSimulator.replica.DemoMBFTReplica;


public class test {
	
	public static void main(String[] args) {
		
		Queue<PrePrepareMsg> executeQ = new PriorityQueue<>(DemoMBFTReplica.nCmp);
		executeQ.add(new PrePrepareMsg(3, 2, null, 0, 0, 0, 0));
		executeQ.add(new PrePrepareMsg(3, 4, null, 0, 0, 0, 0));
		executeQ.add(new PrePrepareMsg(3, 3, null, 0, 0, 0, 0));
		while(!executeQ.isEmpty()) {
			System.out.println(executeQ.poll().n);
		}

		Queue<Message> msgQue = new PriorityQueue<>(Message.cmp);

		msgQue.add(new PrePrepareMsg(3, 2, null, 0, 0, 0, 1));
		msgQue.add(new PrePrepareMsg(3, 2, null, 0, 0, 0, 5));
		msgQue.add(new RequestMsg("Message",2, 1, 1, 0, 10));
		msgQue.add(new RequestMsg("Message", 1, 1, 1, 0,  5));

		while(!msgQue.isEmpty()) {
			System.out.println(msgQue.poll());
		}
	}
}
