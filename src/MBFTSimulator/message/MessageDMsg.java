package MBFTSimulator.message;

import java.util.Objects;

/**
 * @ClassName: MessageDMsg
 * @Description: 请求分发消息，类似Preprepare消息
 * @Author: Dongxu Zhu
 **/
public class MessageDMsg extends Message{

    public int v;

    public int n;

    public Message m;

    public int i;
    //v表示视图编号;n表示序列号;m表示request消息;i表示节点id
    public MessageDMsg(int v, int n, Message m, int i, int sndId, int rcvId, double rcvtime) {
        super(sndId, rcvId, rcvtime);
        this.type = MessageD;
        this.len = MessageDMSGLEN;
        this.v = v;
        this.n = n;
        this.m = m;
        this.i = i;
    }

    public Message copy(int rcvId, double rcvtime) {
        //m是浅复制，不过没有关系，不会修改它的值
        return new MessageDMsg(v, n, m, i, sndId, rcvId, rcvtime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OrderingPMsg) {
            OrderingPMsg msg = (OrderingPMsg) obj;
            return (v == msg.v && n == msg.n && i == msg.i && ((m == null && msg.m == null) || (m != null && m.equals(msg.m))));
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), v, n, m, i);
    }

    public String toString() {
        return super.toString() + "视图编号:"+v+";序列号:"+n;
    }
}
