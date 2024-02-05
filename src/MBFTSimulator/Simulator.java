package MBFTSimulator;

import java.util.*;

import MBFTSimulator.message.Message;
import MBFTSimulator.replica.*;

/**
 * @ClassName: Simulator
 * @Description:
 * @Author: Dongxu Zhu
 **/
public class Simulator {

	public static int RN = 8;  						//replicas节点的数量(rn)

	public static final int FN = 1;							//恶意节点的数量

	public static final int CN = 10;						//客户端数量3

	public static int INFLIGHT = 1000; 					//最多同时处理多少请求

	public static int REQNUM = 2000;					//请求消息总数量

	public static final int BASEDLYBTWRP = 20;				//节点之间的基础网络时延(单位0.1毫秒)

	public static final int DLYRNGBTWRP = 2;				//节点间的网络时延扰动范围(单位0.1毫秒)

	public static final int BASEDLYBTWRPANDCLI = 75;		//节点与客户端之间的基础网络时延(单位0.1毫秒)

	public static final int DLYRNGBTWRPANDCLI = 10;			//节点与客户端之间的网络时延扰动范围(单位0.1毫秒)

	public static final int BANDWIDTH = 150000;			//节点间网络的额定带宽(bytes)(超过后时延呈指数级上升),1M=1000000

	public static final double FACTOR = 1.5;				//超出额定负载后的指数基数
	//处理一条信息的时间0.01ms
	public static final double processMsgTime = 0.01;
	//排序阶段delta
	public static final int orderingDelta = 100; 		// 10ms

	public static final boolean SHOWDETAILINFO = false;		//是否显示完整的消息交互过程

	//消息优先队列（按消息计划被处理的时间戳排序）
	public static Queue<Message> msgQue = null;
	//正在网络中传播的消息的总大小
	public static Queue<Message> inFlyMsgQue = null;
	public static long inFlyMsgLen = 0;
	public static double TimeStamp = 0;
	public static final double inFlyMsgWindowsTime = 1; //1.5ms

	//初始化节点之间的基础网络时延以及节点与客户端之间的基础网络时延
	public static int[][] netDlys = netDlyBtwRpInit(RN);

	public static int[][] netDlysToClis = netDlyBtwRpAndCliInit(RN, CN);

	public static int[][] netDlysToNodes = Utils.flipMatrix(netDlysToClis);

	public static MBFT_1_2_2_Replica[] reps = null;

	public static long timestamp = 0; //	模拟器当前系统时间

	public Simulator() {
	}

	public Simulator(int RN, int INFLIGHT, int REQNUM) {
		Simulator.RN = RN;
		Simulator.INFLIGHT = INFLIGHT;
		Simulator.REQNUM = REQNUM;

		Simulator.msgQue = new PriorityQueue<>(Message.cmp);
		//正在网络中传播的消息的总大小
		Simulator.inFlyMsgQue = new PriorityQueue<>(Message.cmp);
		Simulator.inFlyMsgLen = 0;
		Simulator.TimeStamp = 0;

		Simulator.netDlys = netDlyBtwRpInit(RN);

		Simulator.netDlysToClis = netDlyBtwRpAndCliInit(RN, CN);

		Simulator.netDlysToNodes = Utils.flipMatrix(netDlysToClis);

		Simulator.reps = new MBFT_1_2_2_Replica[RN];
	}

	public Map<String, Double> run() {
//		reps为Replica数组，用于保存节点对象

		long Max_inFlyMsgLen = 0;
		for(int i = 0; i < RN; i++) {
			int[] temp0 = netDlys[i];
			int[] temp1 = netDlysToClis[i];
			reps[i] = new MBFT_1_2_2_Replica(i, netDlys[i], netDlysToClis[i]);
		}
		//初始化CN个客户端
		Client[] clis = new Client[CN];
		for(int i = 0; i < CN; i++) {
			//客户端的编号设置为负数
			clis[i] = new Client(Client.getCliId(i), netDlysToNodes[i]);
		}

		Random rand = new Random(555);
		int requestNums = 0;
		for(int i = 0; i < Math.min(INFLIGHT, REQNUM); i++) {
//			随机选择客户端，发送请求信息。
			clis[rand.nextInt(CN)].sendRequest(0);
			requestNums++;
		}

//		按接收时间顺序消息处理，直至消息队列为空！
		while(!msgQue.isEmpty()) {
			Message msg = msgQue.poll();
			TimeStamp = msg.rcvtime;
			inFlyMsgQue.add(msg);
			inFlyMsgLen += msg.len;
			Message temp;
			while(inFlyMsgQue.peek()!= null && inFlyMsgQue.peek().rcvtime < (TimeStamp - inFlyMsgWindowsTime)){
				temp = inFlyMsgQue.poll();
				inFlyMsgLen -= temp.len;
			}

			switch(msg.type) {
			case Message.REPLY:
//				若msg的消息类型为REPLY或CLITIMEOUT，则交由对应客户端msg.rcvId处理
				clis[Client.getCliArrayIndex(msg.rcvId)].msgProcess(msg);
				break;
			default:
//				若msg的消息为发送给集群节点的，则交由对应集群节点msg.rcvId处理。
				reps[msg.rcvId].msgProcess(msg);
			}
			//如果还未达到稳定状态的request消息小于INFLIGHT，随机选择一个客户端发送请求消息。意思是保持正在处理的请求数恒定为INFLIGHT
			if(requestNums - getStableRequestNum(clis) < INFLIGHT && requestNums < REQNUM) {
				clis[rand.nextInt(CN)].sendRequest((long)msg.rcvtime);
				requestNums++;
			}
//			以处理完该消息，在网络开销中去除本消息
			if (inFlyMsgLen>Max_inFlyMsgLen){
				Max_inFlyMsgLen = inFlyMsgLen;
			}

		}

//		消息处理结束，消息队列已经为空
//		下面代码对本次通信模拟过程进行统计，获取tps和延时时间
		long totalTime = 0;
		long totalStableMsg = 0;
		for(int i = 0; i < CN; i++) {
			totalTime += clis[i].accTime;
			totalStableMsg += clis[i].stableMsgNum();
		}
		double tps = getStableRequestNum(clis)/(timestamp/10000.0);
		double ave_delay = totalTime/(totalStableMsg*10.0);
		System.out.println("【The end】消息平均确认时间为:"+ave_delay
				+"毫秒;消息吞吐量为:"+tps+"tps"+"节点数"+RN+ "并行数"+INFLIGHT +",最大带宽占用："+Max_inFlyMsgLen);
//		System.out.println("共处理消息："+totalStableMsg + ",耗时："+(timestamp/10.0)+"ms");
		Map<String,Double> res = new HashMap<>();
		res.put("ave_delay", ave_delay);
		res.put("tps", tps);

		return res;
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static int[][] netDlyBtwRpInit(int n){
		int[][] ltcs = new int[n][n];
		Random rand = new Random(999);
//		Random rand = new Random();
		for(int i = 0; i < n; ++i)
			for(int j = 0; j < n; ++j)
				if(i < j && ltcs[i][j] == 0) {
					ltcs[i][j] = BASEDLYBTWRP + rand.nextInt(DLYRNGBTWRP);
					ltcs[j][i] = ltcs[i][j];
				}
		return ltcs;
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static int[][] netDlyBtwRpAndCliInit(int n, int m){
		int[][] ltcs = new int[n][m];
		Random rand = new Random(666);
//		Random rand = new Random();
		for(int i = 0; i < n; i++)
			for(int j = 0; j < m; j++)
				ltcs[i][j] = BASEDLYBTWRPANDCLI + rand.nextInt(DLYRNGBTWRPANDCLI);
		return ltcs;
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static boolean[] byztDistriInit(int n, int f) {
		boolean[] byzt = new boolean[n];
		Random rand = new Random(111);
		while(f > 0) {
			int i = rand.nextInt(n);
			if(!byzt[i]) {
				byzt[i] = true;
				--f;
			}
		}
		return byzt;
	}
	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static void sendMsg(Message msg, String tag) {
		msg.print(tag);
		msgQue.add(msg);
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static void sendMsgToOthers(Message msg, int id, String tag) {
		for(int i = 0; i < RN; i++) {
			if(i != id) {
//				使用Massage类中的copy方法，生成消息内容不变，接受者不同的一个消息m
				long delay = getNetDelay(inFlyMsgLen,netDlys[id][i]);
				double delay_process = Math.max(0, reps[id].msgClearTime - msg.rcvtime + (reps[id].onProcseeingMsgNum* processMsgTime));

				if (delay_process == 0){
					reps[id].msgClearTime = msg.rcvtime;
					reps[id].onProcseeingMsgNum = 0;
				}
				if (id == 0){
					delay_process = delay_process*0.1;
				}
				Message m = msg.copy(i, msg.rcvtime + delay + delay_process);
				sendMsg(m, tag);
			}
		}
	}

	public static void sendMsgToReplica(Message msg, int sndId,int rcvId, String tag) {

//		使用Massage类中的copy方法，生成消息内容不变，接受者不同的一个消息m
		double delay = (double) getNetDelay(inFlyMsgLen,netDlys[sndId][rcvId]);
		double delay_process = Math.max(0, reps[sndId].msgClearTime - msg.rcvtime + (reps[sndId].onProcseeingMsgNum * processMsgTime));

		if (delay_process == 0){
			reps[sndId].msgClearTime = msg.rcvtime;
			reps[sndId].onProcseeingMsgNum = 0;
		}
		if (sndId == 0){
			delay_process = delay_process*0.1;
		}
		Message m = msg.copy(rcvId, msg.rcvtime + delay + delay_process);
		sendMsg(m, tag);

	}

	public static void sendMsgToOthers(Set<Message> msgSet, int id, String tag) {
		if(msgSet == null) {
			return;
		}
		for(Message msg : msgSet) {
			sendMsgToOthers(msg, id, tag);
		}
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static int getNetDelay(long inFlyMsgLen, int basedelay) {
//		若占用情况未超过上限，则实时延时=基础延时
		double RATIO = inFlyMsgLen/(double)BANDWIDTH;
		if(RATIO < 1) {
			return basedelay;
		}else {
//			return (int)(Math.pow(FACTOR, inFlyMsgLen/(double)(BANDWIDTH)) * basedelay);
			return (int)(Math.pow(RATIO, 5) * basedelay);
//			return (int)(Math.pow(FACTOR, RATIO-1) * basedelay);
		}
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static int getStableRequestNum(Client[] clis) {
		int num = 0;
		for(int i = 0; i < clis.length; i++) {
			num += clis[i].stableMsgNum();
		}
		return num;
	}
}