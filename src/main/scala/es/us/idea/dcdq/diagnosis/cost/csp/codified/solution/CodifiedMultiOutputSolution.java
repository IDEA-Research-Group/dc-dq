package es.us.idea.dcdq.diagnosis.cost.csp.codified.solution;

import java.util.Arrays;

public class CodifiedMultiOutputSolution {

    private Integer[][] tUps;
    private int cost;
    private Integer[][] ct;

    public CodifiedMultiOutputSolution(Integer[][] tUps, int cost, Integer [][] ct) {
        this.tUps = tUps;
        this.cost = cost;
        this.ct = ct;
    }

    public Integer[][] gettUps() {
        return tUps;
    }

    public void settUps(Integer[][] tUps) {
        this.tUps = tUps;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setCt(Integer[][] ct) {
        this.ct = ct;
    }

    public Integer[] getBrdvCosts() {
        return Arrays.stream(ct).map(x -> Arrays.stream(x).reduce(0, Integer::sum)).toArray(Integer[]::new);
    }

    public Integer[] getUpCosts() {
        Integer[] res = new Integer[ct[0].length];
        for(int col = 0; col<ct[0].length; col++) {
            int sumCol = 0;
            for(int row = 0; row<ct.length; row++)
                sumCol += ct[row][col];
            res[col] = sumCol;
        }
        return res;
    }

    public Integer[][] getUpToBrdvCosts() {
        Integer[][] res = new Integer[ct[0].length][];
        for(int col = 0; col<ct[0].length; col++){
            res[col] = new Integer[ct.length];
            for(int row = 0; row<ct.length; row++)
                res[col][row] = ct[row][col];
        }
        return res;
    }
}
