package es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel;

public class TransitionCost {

    private Integer from;
    private Integer to;
    private Integer cost;
    private Boolean single;

    public TransitionCost(Integer from, Integer to, Integer cost, Boolean single) {
        this.from = from;
        this.to = to;
        this.cost = cost;
        this.single = single;
    }

    public Boolean getSingle() {
        return single;
    }

    public void setSingle(Boolean single) {
        this.single = single;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "TransitionCost{" +
                "from=" + from +
                ", to=" + to +
                ", cost=" + cost +
                ", single=" + single +
                '}';
    }
}
