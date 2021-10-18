package es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel;

import java.util.List;

public class BRDVCost {

    private List<TransitionCost> transitionCosts;
    private Integer defaultCost;
    private Boolean defaultSingle;

    public BRDVCost(List<TransitionCost> transitionCosts, Integer defaultCost, Boolean defaultSingle) {
        this.transitionCosts = transitionCosts;
        this.defaultCost = defaultCost;
        this.defaultSingle = defaultSingle;
    }

    public List<TransitionCost> getTransitionCosts() {
        return transitionCosts;
    }

    public void setTransitionCosts(List<TransitionCost> transitionCosts) {
        this.transitionCosts = transitionCosts;
    }

    public Integer getDefaultCost() {
        return defaultCost;
    }

    public void setDefaultCost(Integer defaultCost) {
        this.defaultCost = defaultCost;
    }

    public Boolean getDefaultSingle() {
        return defaultSingle;
    }

    public void setDefaultSingle(Boolean defaultSingle) {
        this.defaultSingle = defaultSingle;
    }

    @Override
    public String toString() {
        return "BRDVCost{" +
                "transitionCosts=" + transitionCosts +
                ", defaultCost=" + defaultCost +
                ", defaultSingle=" + defaultSingle +
                '}';
    }
}
