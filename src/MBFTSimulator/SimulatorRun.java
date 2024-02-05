package MBFTSimulator;/**
 * author 93937
 * date 2023-02-06 14:24
 **/

import java.util.*;

/**
 * @ClassName: SimulatorRun
 * @Description:
 * @Author: Dongxu Zhu
 **/
public class SimulatorRun {


    public static void main(String[] args) {
        List<Double> Res_delay = new ArrayList<>();
        List<Double> Res_tps = new ArrayList<>();
        int [] INFlying = new int [ ]{1,5,15,20,30,40,50,100,200,300,400,500,600,800,1000};
        int i = 0;
        while(i < INFlying.length){
            Simulator simulator = new Simulator(16,INFlying[i],2*INFlying[i]);
            Map<String,Double> res;
            res = simulator.run();
            System.out.println(res);
            Res_delay.add(i,res.get("ave_delay"));
            Res_tps.add(i,res.get("tps"));
            i++;
        }
        System.out.println(Res_delay);
        System.out.println(Res_tps);
    }

}
