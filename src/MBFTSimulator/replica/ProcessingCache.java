package MBFTSimulator.replica;/**
 * author 93937
 * date 2023-02-03 17:34
 **/

import MBFTSimulator.message.Message;

import java.util.Objects;

/**
 * @ClassName: Cache
 * @Description:
 * @Author: Dongxu Zhu
 **/
public class ProcessingCache {

    Message requestMsg;
    int index_num = 0;
    int OP_N = 0;
    int OV_N = 0;
    int prePrepare_N = 0;
    int prePare_N = 0;
    int commit_N = 0;
    int messageD_N = 0;
    int messageC_N = 0;
    int messageVote_N = 0;
    int messageDecsion_N = 0;
    int commitV_N = 0;
    int commitD_N = 0;

    public ProcessingCache() {
    }

    public ProcessingCache(Message requestMsg, int index_num, int OP_N, int OV_N, int prePrepare_N, int prePare_N, int commit_N) {
        this.requestMsg = requestMsg;
        this.index_num = index_num;
        this.OP_N = OP_N;
        this.OV_N = OV_N;
        this.prePrepare_N = prePrepare_N;
        this.prePare_N = prePare_N;
        this.commit_N = commit_N;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingCache that = (ProcessingCache) o;
        return index_num == that.index_num && OP_N == that.OP_N && OV_N == that.OV_N && prePrepare_N == that.prePrepare_N && prePare_N == that.prePare_N && commit_N == that.commit_N && Objects.equals(requestMsg, that.requestMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestMsg, index_num, OP_N, OV_N, prePrepare_N, prePare_N, commit_N);
    }

    @Override
    public String toString() {
        return "ProcessingCache{" +
                "requestMsg=" + requestMsg +
                ", index_num=" + index_num +
                ", OP_N=" + OP_N +
                ", OV_N=" + OV_N +
                ", prePrepare_N=" + prePrepare_N +
                ", prePare_N=" + prePare_N +
                ", commit_N=" + commit_N +
                '}';
    }
}
