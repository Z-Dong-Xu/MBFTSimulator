package MBFTSimulator.message;

public class CommitMsg extends Message {
	
	public int v;				
	
	public int n;
	
	public int i;

	public Message m;

	//消息结构
	//<COMMIT, v, n, d, i>:v表示视图编号;n表示序列号;d表示request消息的摘要;i表示节点id
	public CommitMsg(int v, int n, Message m, int i, int sndId, int rcvId, double rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = COMMIT;
		this.len = COMMSGLEN;
		this.v = v;
		this.n = n;
		this.m = m;
		this.i = i;
	}

	
	public Message copy(int rcvId, double rcvtime) {
		return new CommitMsg(v, n, RequestMsg.copy((RequestMsg) this.m), i, sndId, rcvId, rcvtime);
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof CommitMsg) {
        	CommitMsg msg = (CommitMsg) obj;
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
