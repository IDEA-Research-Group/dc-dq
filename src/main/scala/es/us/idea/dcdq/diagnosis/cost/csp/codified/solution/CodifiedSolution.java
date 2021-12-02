package es.us.idea.dcdq.diagnosis.cost.csp.codified.solution;

public class CodifiedSolution {

    private int[] brdv;
    private int cost;

    public CodifiedSolution(int[] brdv, int cost) {
        this.brdv = brdv;
        this.cost = cost;
    }

    public int[] getBrdv() {
        return brdv;
    }

    public void setBrdv(int[] brdv) {
        this.brdv = brdv;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }
}
