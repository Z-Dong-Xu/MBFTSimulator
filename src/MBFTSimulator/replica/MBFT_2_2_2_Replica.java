package MBFTSimulator.replica;/**
 * author 93937
 * date 2023-02-04 16:41
 **/

import MBFTSimulator.Client;
import MBFTSimulator.Simulator;
import MBFTSimulator.Utils;
import MBFTSimulator.message.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: MBFT_2_2_2_Replica
 * @Description:
 * @Author: Dongxu Zhu
 **/
public class MBFT_2_2_2_Replica extends Replica{

    //正在处理的状态位
    public static final int PROCESSING = 0;
    //稳定标志位
    public static final int STABLE = 1;

    public double msgClearTime = 0;
    public long onProcseeingMsgNum = 0;

    public String receiveTag = "Receive";

    public String sendTag = "Send";
    //当前节点的id
    public int id;
    //当前视图编号
    public int v;
    //消息处理序列号
    public int n;   // n给主节点使用

    //与其他节点的网络延迟
    public int netDlys[];
    //与客户端的网络延迟
    public int netDlyToClis[];
    //当前正在处理的请求是否超时（如果超时了不会再发送任何消息）
    public boolean isTimeOut;
    //消息缓存<type, <msg>>:type消息类型;
    public HashMap<Integer, ProcessingCache> msgCache;
    //request请求状态
    public Map<Message, Integer> reqStats;

    public static Comparator<PrePrepareMsg> nCmp = new Comparator<PrePrepareMsg>(){
        @Override
        public int compare(PrePrepareMsg c1, PrePrepareMsg c2) {
            return (int) (c1.n - c2.n);
        }
    };

    public static long orderedNum = 0;


    public MBFT_2_2_2_Replica(int id, int[] netDlys, int[] netDlyToClis) {
        this.id = id;
        this.netDlys = netDlys;
        this.netDlyToClis = netDlyToClis;
        this.msgCache = new HashMap<>();
        this.reqStats = new HashMap<>();
    }

    /**
     * Demo_MBFT的消息分发处理函数
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void msgProcess(Message msg) {
        msg.print(receiveTag); // 在SHOWDETAILINFO=false时不起作用

        if(msgClearTime == 0 ){
            msgClearTime = msg.rcvtime;
        }
        onProcseeingMsgNum++;
//		根据消息类型进行分发处理
        switch(msg.type) {
            case Message.REQUEST:
//			节点收到请求消息时：
                receiveRequest(msg);
                break;
            case Message.OrderingP:
                receiveOrderingP(msg);
                break;
            case Message.OrderingV:
                receiveOrderingV(msg);
                break;
            case Message.MessageD:
//			节点收到主节点发送的预准备消息时：
                receiveMessageD(msg);
                break;
            case Message.MessageC:
//			节点收到其他节点发来的准备消息时：
                receiveMessageC(msg);
                break;
            case Message.COMMIT:
//			节点收到其他节点发来的提交消息时：
                receiveCommit(msg);
                break;
            default:
                System.out.println("【Error】消息类型错误！");
                return;
        }

    }

    /**
     * 主节点接受到请求消息的处理，因为客户端发送请求的目标永远是主节点，所以只有主节点会进入这个函数
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void receiveRequest(Message msg) {
        if(msg == null) return;
        RequestMsg reqMsg = (RequestMsg)msg;
        if(reqStats.containsKey(msg)){
            return;
        }
        //主节点才会收到请求消息
        n++;
        if(!hasRequset(n)){
            addMessageToCache(reqMsg,n);
            Message orderingPMsg = new OrderingPMsg(v, n, reqMsg, id, id, id, reqMsg.rcvtime);
            addMessageToCache(orderingPMsg, n);
            Simulator.sendMsgToOthers(orderingPMsg, id, sendTag);
        }

    }

    /**
     * 主节点发出：排序提议消息，发送给集群所有节点
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void receiveOrderingP(Message msg){
        if(isTimeOut) return;
        OrderingPMsg orderingPMsg = (OrderingPMsg)msg;
        Message req = orderingPMsg.m;
        int req_v = orderingPMsg.v;
        int req_n = orderingPMsg.n;
        int i = orderingPMsg.i;

        if(hasRequset(req_n)){
            addMessageToCache(msg,req_n);
            //生成当前orderingVote消息
            Message orderingVMsg = new OrderingVMsg(req_v, req_n, req, id, id, id, msg.rcvtime);
//		    2. 将本OV消息发送到主节点消息广播
            Simulator.sendMsgToReplica(orderingVMsg,id,getPriId(),sendTag);
        }else {
            addMessageToCache(req,req_n);
            addMessageToCache(msg,req_n);
            //生成当前orderingVote消息
            Message orderingVMsg = new OrderingVMsg(req_v, req_n, orderingPMsg.m, id, id, id, msg.rcvtime);
//		2. 将本OV消息发送到主节点消息广播
            Simulator.sendMsgToReplica(orderingVMsg,id,getPriId(),sendTag);
        }

    }

    /**
     * 各节点向主节点发送：排序投票消息
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void receiveOrderingV(Message msg){
        if(isTimeOut) return;
        OrderingVMsg orderingVMsg = (OrderingVMsg)msg;
        Message req = orderingVMsg.m;
        int req_v = orderingVMsg.v;
        int req_n = orderingVMsg.n;

        if(hasRequset(req_n)){
            addMessageToCache(orderingVMsg, req_n);
            if(ordered(req_n)){
                MessageDMsg messageDMsg = new MessageDMsg(req_v, req_n, orderingVMsg.m, id, id, id, orderingVMsg.rcvtime + Simulator.orderingDelta);
                Simulator.sendMsgToOthers(messageDMsg, id, sendTag);
                addMessageToCache(messageDMsg,req_n);
                if(Simulator.SHOWDETAILINFO){
                    orderedNum++;
                    System.out.println("排序结束,发送第"+orderedNum+"预准备消息"+messageDMsg.toString()+"---"+orderingVMsg.m.rcvtime);

                }
            }
        }else {
            addMessageToCache(req,req_n);
            addMessageToCache(msg,req_n);
        }

    }

    /**
     * 主节点收集到足够的排序投票后，发出预准备消息
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void receiveMessageD(Message msg) {
        if(isTimeOut) return;
        MessageDMsg messageDMsg = (MessageDMsg)msg;
        Message req = messageDMsg.m;
        int req_v = messageDMsg.v;
        int req_n = messageDMsg.n;
        int i = messageDMsg.i;

        if(hasRequset(req_n)){
            addMessageToCache(msg,req_n);
            Message messageCMsg = new MessageCMsg(req_v, req_n, req, id, id, id, msg.rcvtime);
//		    1.将本prepare加入当前节点的msgCache中的prepare集合
            addMessageToCache(messageCMsg,req_n);
//		    2. 将本prepare消息广播
            Simulator.sendMsgToOthers(messageCMsg, id, sendTag);
        }else {
            addMessageToCache(req,req_n);
            addMessageToCache(msg,req_n);
            Message messageCMsg = new MessageCMsg(req_v, req_n, req, id, id, id, msg.rcvtime);
//		    1.将本prepare加入当前节点的msgCache中的prepare集合
            addMessageToCache(messageCMsg,req_n);
//		    2. 将本prepare消息广播
            Simulator.sendMsgToOthers(messageCMsg, id, sendTag);
        }

    }

    /**
     * 各节点收到主节点的预准备消息后，广播准备消息
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void receiveMessageC(Message msg) {
        if(isTimeOut) return;
        MessageCMsg messageCMsg = (MessageCMsg)msg;
        Message req = messageCMsg.m;
        int req_v = messageCMsg.v;
        int req_n = messageCMsg.n;

        if(hasRequset(req_n)){

            addMessageToCache(msg,req_n);
            if(prepared(req_n)) {
                CommitMsg cm = new CommitMsg(req_v, req_n, req, id, id, id, msg.rcvtime);
                Simulator.sendMsgToOthers(cm, id, sendTag);
                addMessageToCache(cm, req_n);
            }

        }else {
            addMessageToCache(req,req_n);
            addMessageToCache(msg,req_n);
        }

    }

    /**
     * 各节点收集到2f个准备消息后，广播commit消息
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public void receiveCommit(Message msg) {
        if(isTimeOut) return;
        CommitMsg commitMsg = (CommitMsg)msg;
        RequestMsg req = (RequestMsg) commitMsg.m;
        int req_v = commitMsg.v;
        int req_n = commitMsg.n;

        if(hasRequset(req_n)){
            addMessageToCache(msg,req_n);
            long delay = Simulator.getNetDelay(Simulator.inFlyMsgLen,netDlyToClis[Client.getCliArrayIndex(req.c)]);
            ReplyMsg rm = new ReplyMsg(req_v, req.t, req.c, id, "result", id, req.c, msg.rcvtime + delay);
            if(commited(req_n)) {
                Simulator.sendMsg(rm, sendTag);
                reqStats.put(req, STABLE);
            }
        }else{
            addMessageToCache(req,req_n);
            addMessageToCache(msg,req_n);
        }

    }


    /**
     * 主节点用于判断一个消息是否已经收集到2f+1个排序投票
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public boolean ordered(int req_n) {
        ProcessingCache cache = msgCache.get(req_n);

        if (cache == null) return false;

        if( cache.OV_N >= (2 * Utils.getMaxTorelentNumber(Simulator.RN)+1)  ) {

            return true;
        }
        return false;
    }

    /**
     * 节点用于判断一个消息是否已经收集到2f个准备消息
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public boolean prepared(int req_n) {
        ProcessingCache cache = msgCache.get(req_n);

        if (cache == null) return false;

        if(cache.messageC_N >= 2 * Utils.getMaxTorelentNumber(Simulator.RN)) {

            return true;
        }
        return false;
    }

    /**
     * 节点用于判断一个消息是否已经收集到2f+1个commit消息
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    public boolean commited(int req_n) {
        ProcessingCache cache = msgCache.get(req_n);

        if (cache == null) return false;

        if(cache.commit_N >= (2 * Utils.getMaxTorelentNumber(Simulator.RN)+1)  ) {

            return true;
        }
        return false;
    }

    public int getPriId() {
        return v % Simulator.RN;
    }

    private boolean hasRequset(int n){
        return msgCache.containsKey(n);
    }

    /**
     * 用于各个节点内部消息的管理存储
     * @param:
     * @return:
     * @author: Dongxu Zhu
     */
    private void addMessageToCache(Message msg, int n) {
        ProcessingCache processingCache = msgCache.get(n);
        if (processingCache == null ){
            if(msg.type == Message.REQUEST) {
                ProcessingCache cache = new ProcessingCache();
                cache.requestMsg = msg;
                cache.index_num = n;
                msgCache.put(n, cache);
                reqStats.put(msg,PROCESSING);
            }
        }else {
            switch(msg.type) {
                case Message.OrderingP:
                    processingCache.OP_N++;
                    break;
                case Message.OrderingV:
                    processingCache.OV_N++;
                    break;
                case Message.PREPREPARE:
                    processingCache.prePrepare_N++;
                    break;
                case Message.PREPARE:
                    processingCache.prePare_N++;
                    break;
                case Message.COMMIT:
                    processingCache.commit_N++;
                    break;
                case Message.MessageD:
                    processingCache.messageD_N++;
                    break;
                case Message.MessageC:
                    processingCache.messageC_N++;
                    break;
                case Message.MessageVote:
                    processingCache.messageVote_N++;
                    break;
                case Message.MessageDecision:
                    processingCache.messageDecsion_N++;
                    break;
                case Message.CommitV:
                    processingCache.commitV_N++;
                    break;
                case Message.CommitD:
                    processingCache.commitD_N++;
                    break;
                default:
                    System.out.println("重复Request！");
            }
        }
    }

}
