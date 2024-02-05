package MBFTSimulator;

import java.util.HashMap;
import java.util.Map;

import MBFTSimulator.message.Message;
import MBFTSimulator.message.ReplyMsg;
import MBFTSimulator.message.RequestMsg;

/**
 * @ClassName: Client
 * @Description:
 * @Author: Dongxu Zhu
 **/
public class Client {
	//正在处理，没有收到f+1个reply
	public static final int PROCESSING = 0;
	//已处理完成，已经收到了f+1个reply
	public static final int STABLE = 1;
	//本客户端编号
	public int id;
	//当前视图编号
	public int v;
	//用于保存本客户端发出的所有request请求的状态
	public Map<Long, Integer> reqStats;
	//用于保存本客户端发出的所有request请求的消息（删除已经达到stable状态的request消息）
	public Map<Long, Message> reqMsgs;
	//用于保存本客户端发出的所有request请求对应的reply消息（删除已经达到stable状态的reply消息）
	public Map<Long, Map<Integer, Message>> repMsgs;
	//累积确认时间
	public long accTime;
	//与各节点的基础网络时延
	public int netDlys[];
	
	public String receiveTag = "CliReceive";
	
	public String sendTag = "CliSend";
	
	public Client(int id, int[] netDlys) {
		this.id = id;
		this.netDlys = netDlys;
		reqStats = new HashMap<>();
		reqMsgs = new HashMap<>();
		repMsgs = new HashMap<>();
	}
	
	public void msgProcess(Message msg) {
		msg.print(receiveTag);
		switch(msg.type) {
		case Message.REPLY:
			receiveReply(msg);
			break;
		default:
			System.out.println("【Error】消息类型错误！");
		}
		
	}
	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public void sendRequest(long time) {
		//reqStats
//		用于判断本客户端是否已经在时间time发送过请求，若发送过，则在尝试在time+1ms时间发送；若没有则直接发送
		while(reqStats.containsKey(time)) {
			time=time + 50;
		}
//		本客户端计算主节点号，并将Request发送给主节点priId
		int priId = v % Simulator.RN;
//		构造Request消息，其中：time为发起请求的时间，rcvtime为主节点接收到本请求的时间，与网络状态有关
		long delay = Simulator.getNetDelay(Simulator.inFlyMsgLen,netDlys[priId]);
		Message requestMsg = new RequestMsg("Message", time, id, id, priId, time + delay);
//		本客户端发送当前构造好的请求requestMsg。本程序为将requestMsg消息加入Simulator中的msgQue消息队列，并修改当前占用带宽数
		Simulator.sendMsg(requestMsg, sendTag);
//		将本请求放入reqStats（Map<int，int>）中，key：本请求发出的时间（因为在某个客户端中，请求发出的时间即可唯一标识一个请求）；value：该请求当前的处理状态
		reqStats.put(time, PROCESSING);
//		将本请求放入reqMsgs（Map<int，Massage>）中，key：本请求发出的时间，value：本客户端在time时间发出的请求内容
		reqMsgs.put(time, requestMsg);
//		将本请求放入repMsgs（Map<int，Map<int，Massage>>）中，key：本请求发出的时间，value：该请求目前收到的reply消息集合
		repMsgs.put(time, new HashMap<>());

	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public void receiveReply(Message msg) {
		ReplyMsg repMsg = (ReplyMsg)msg;
		long t = repMsg.t;
		//如果这条reply消息对应的request消息不存在或者已经是stable状态，则忽略本reply消息
		if(!reqStats.containsKey(t) || reqStats.get(t) == STABLE) {
			return;
		}
		//本客户端将本条reply消息保存在对应位置
		saveReplyMsg(repMsg);
		//判断与本reply消息相对应的request消息是否已收到f+1条回复，若收到，则将该request的状态设为稳态！
		if(isStable(repMsg)) {
			v = repMsg.v;
//			计算累计时间
			accTime += repMsg.rcvtime - t;
//			若收到，则将该request的状态设为稳态！
			reqStats.put(t, STABLE);
//			清除垃圾
			reqMsgs.remove(t);
			repMsgs.remove(t);
			Simulator.timestamp = (long)repMsg.rcvtime;
			if (Simulator.SHOWDETAILINFO){
				System.out.println("当前时间"+repMsg.rcvtime/10+"ms--【Stable】客户端"+id+"在"+t/10
						+"时间请求的消息已经得到了f+1条reply，进入稳态，共耗时"+(repMsg.rcvtime - t)/10+"毫秒,此时占用带宽为"+Simulator.inFlyMsgLen+"B");

			}
		}
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public void saveReplyMsg(ReplyMsg msg) {
		Map<Integer, Message> rMap = repMsgs.get(msg.t);
		for(Integer i : rMap.keySet()) {
			if(i == msg.i && ((ReplyMsg)rMap.get(i)).v >= msg.v) {
				return;
			}
		}
		repMsgs.get(msg.t).put(msg.i, msg);
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public boolean isStable(ReplyMsg msg) {
		Map<Integer, Message> rMap = repMsgs.get(msg.t);
		int cnt = 0;
		for(Integer i : rMap.keySet()) {
			if(((ReplyMsg)rMap.get(i)).v == msg.v && ((ReplyMsg)rMap.get(i)).r == msg.r) {
				cnt++;
			}
		}
//		cnt为reply消息数量，若cnt大于等于f+1，则返回true
		if(cnt > Utils.getMaxTorelentNumber(Simulator.RN)) return true;
		return false;
	}

	/**
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public static int getCliId(int index) {
		return index * (-1) - 1;
	}

	/**
	* @param:
	* @return: 客户端序号
	* @author: Dongxu Zhu
	*/
	public static int getCliArrayIndex(int id) {
		return (id + 1) * (-1);
	}

	/**
	* 用于计算本客户端中，现有多少个request已经处理完毕，达到稳态
	* @param:
	* @return:
	* @author: Dongxu Zhu
	*/
	public int stableMsgNum() {
		int cnt = 0;
		if(reqStats == null) return cnt;
		for(long t : reqStats.keySet()) 
			if(reqStats.get(t) == STABLE) 
				cnt++;
		return cnt;
	}


}
