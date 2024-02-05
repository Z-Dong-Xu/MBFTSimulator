package MBFTSimulator.message;

public class PrepareMsg extends Message {
	
	public int v;				
	
	public int n;			


	public Message m;
	
	public int i;

	//<PREPARE, v, n, d, i>:v表示视图编号;n表示序列号;d表示request消息的摘要;i表示节点id
	public PrepareMsg(int v, int n, Message m, int i, int sndId, int rcvId, double rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = PREPARE;
		this.len = PREMSGLEN;
		this.v = v;
		this.n = n;
		this.m = m;
		this.i = i;
	}

	
	public Message copy(int rcvId, double rcvtime) {
		return new PrepareMsg(v, n, RequestMsg.copy((RequestMsg) this.m), i, sndId, rcvId, rcvtime);
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof PrepareMsg) {
        	PrepareMsg msg = (PrepareMsg) obj;
            return (v == msg.v && n == msg.n);
        }
        return super.equals(obj);
    }
        
    public int hashCode() {
        String str = "" + v + n + m + i;
        return str.hashCode();
    }
    
    public String toString() {
    	return super.toString() + "视图编号:"+v+";序列号:"+n;
    }
}
