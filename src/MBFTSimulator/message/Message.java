package MBFTSimulator.message;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import MBFTSimulator.Simulator;

public class Message {
	
	public static final int REQUEST = 0;		
	
	public static final int PREPREPARE = 1;
	public static final int PREPARE = 2;
	public static final int COMMIT = 3;

	public static final int REPLY = 4;

	public static final int OrderingP = 5;
	public static final int OrderingV = 6;
	public static final int MessageD = 7;
	public static final int MessageC = 8;
	public static final int MessageVote = 9;
	public static final int MessageDecision = 10;
	public static final int CommitV = 11;
	public static final int CommitD = 12;
	
	public static final long REQMSGLEN = 36;	//Request消息的大小(bytes),可按实际情况设置

	public static final long OPMSGLEN = 4;
	public static final long OVMSGLEN = 4;

	public static final long MessageDMSGLEN = 64;
	public static final long MessageCMSGLEN = 64;

	public static final long MessageVoteMSGLEN = 8;
	public static final long MessageDecisionMSGLEN = 36;

	public static final long CommitVMSGLEN = 8;
	public static final long CommitDMSGLEN = 36;

	public static final long PPRMSGLEN = 4 + REQMSGLEN;		//RrePrepare消息的大小
	
	public static final long PREMSGLEN = 64;				//Prepare消息的大小
	
	public static final long COMMSGLEN = 64;				//Commit消息的大小
	
	public static final long REPMSGLEN = 16;				//Reply消息的大小

	
	public static Comparator<Message> cmp = new Comparator<Message>(){
		/**
		* 两个Massage消息的比较方法：根据消息接收时间进行比较
		* @param:
		* @return:
		* @author: Dongxu Zhu
		*/
		@Override
		public int compare(Message c1, Message c2) {
			if ((c1.rcvtime - c2.rcvtime)==0){
				return (int)(c1.type - c2.type);
			}
			return (int) (c1.rcvtime - c2.rcvtime);

		}
	};
	
	public int type;				//消息类型	
	
	public int sndId;				//消息发送端id

	public int rcvId;  				//消息接收端id
	
	public double rcvtime;  			//消息接收时间
	
	public long len;				//消息大小


	/**
	* Massage消息的构造方法。Massage为基类，其规定了所有消息都必须要有的内容！
	* @author: Dongxu Zhu
	*/
	public Message(int sndId, int rcvId, double rcvtime) {
		this.sndId = sndId;
		this.rcvId = rcvId;
		this.rcvtime = rcvtime;
	}

//	消息的打印方法，用于打印自己。需要外部传入一个自己的标签tag
//	若SHOWDETAILINFO为false，则不显示
	public void print(String tag) {
		if(!Simulator.SHOWDETAILINFO) return;
		String prefix = "【"+tag+"】";
		System.out.println(prefix+toString());
	}
	
	public static long accumulateLen(Set<Message> set) {
		long len = 0L;
		if(set != null) {
			for(Message m : set) {
				len += m.len;
			}
		}
		return len;
	}
	
	public static long accumulateLen(Map<Integer, Set<Message>> map) {
		long len = 0L;
		if(map != null) {
			for(Integer n : map.keySet()) {
				len += accumulateLen(map.get(n));
			}
		}
		return len;
	}
	
	public Message copy(int rcvId, double rcvtime) {
//		生成一个与自己发送者相同，但接受者与接收时间不相同的消息！
		return new Message(sndId, rcvId, rcvtime);
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof Message) {
        	Message msg = (Message) obj;
//			判断一个Massage是否与自己相同：消息类型、发送者、接受者、接收时间都相同！
            return (type == msg.type && sndId == msg.sndId && rcvId == msg.rcvId && rcvtime == msg.rcvtime);
        }
        return super.equals(obj);
    }
        
    public int hashCode() {
//		将自身转化成hash，包含4部分内容：类型、发送者、接受者、接收时间
        String str = "" + type + sndId + rcvId + rcvtime;
        return str.hashCode();
    }

	/**
	 * 消息的字符串化方法，将自身转化成字符串，包含4部分内容：类型、发送者、接受者、接收时间
	 * @param:
	 * @return:
	 * @author: Dongxu Zhu
	 */
    public String toString() {
		String[] typeName = {"Request","PrePrepare","Prepare","Commit","Reply"
				,"OrderingP","OrderingV","MessageD","MessageC","MessageVote","MessageDecision","CommitV","CommitD"};
		return "消息类型:"+typeName[type]+";发送者id:"
				+sndId+";接收者id:"+rcvId+";消息接收时间戳:"+rcvtime+";";
    }

}
