package es.us.idea.dcdq.diagnosis.cost.csp;

import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.BRDVCost;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.TransitionCost;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.solution.CodifiedSolution;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class COPFinal {

    public static CodifiedSolution runCop(int[] thisBrdv, int[][] validBrdv, BRDVCost[] costModel, int[] domainBounding) {

        Model model = new Model("DC/DQ - Basic");

        IntVar[] brdv = model.intVarArray("TargetBRDVs", thisBrdv.length, 0, 100);

        IntVar[] cost = model.intVarArray("TransitionCosts", costModel.length, 0, 10);

        // Constraints for the domain of valid brdv
        Constraint[] or = new Constraint[validBrdv.length];

        for(int i=0; i<validBrdv.length; i++) {
            int[] iValidBrdv = validBrdv[i];
            Constraint[] and = new Constraint[iValidBrdv.length];
            for(int j=0; j<iValidBrdv.length; j++) {
                and[j] = model.arithm(brdv[j], "=", iValidBrdv[j]);
            }
            or[i] = model.and(and);
        }

        model.or(or).post();

        // Constraints for the costs
        for(int i=0; i<brdv.length; i++) { // recorro la lista de variables BRDV
            Integer fromThisValue = thisBrdv[i];
            BRDVCost iBRDVCost = costModel[i]; // obtengo los costes para esta BRDV
            List<TransitionCost> validTransitionCosts =
                    iBRDVCost.getTransitionCosts().stream().filter(x -> x.getFrom().equals(fromThisValue) && !x.getTo().equals(fromThisValue))
                            .collect(Collectors.toList());

            int finalI = i;
            //
            Constraint[] notTo = Stream.concat(
                    validTransitionCosts.stream().map(tc -> {
                        model.ifThen(model.arithm(brdv[finalI], "=", tc.getTo()), model.arithm(cost[finalI], "=", tc.getCost()));
                        return model.arithm(brdv[finalI], "!=", tc.getTo());
                    }),
                    Stream.of(model.arithm(brdv[finalI], "!=", fromThisValue)))
                    .toArray(Constraint[]::new);

            // Default cost: si la transicion no ha sido definida en el cost model && la transicion no es al mismo valor,
            // entonces establece el default cost
            model.ifThen(model.and(notTo), model.arithm(cost[finalI], "=", iBRDVCost.getDefaultCost()));
            // Si la transición es al mismo valor, entonces el coste es 0
            model.ifThen(model.arithm(brdv[i], "=", fromThisValue), model.arithm(cost[finalI], "=", 0));
        }

        // variable objetivo
        IntVar totalCost = model.intVar("Total Cost", IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
        model.sum(cost, "=", totalCost).post();
        model.setObjective(Model.MINIMIZE, totalCost);

        Solver solver = model.getSolver();
        Solution solution = solver.findOptimalSolution(totalCost, Model.MINIMIZE);
        // System.out.println(model.toString());
        // System.out.println(solution.toString());
        // System.out.println(solution.getIntVal(totalCost));

        int[] brdvSolution = new int[brdv.length];
        return new CodifiedSolution(Arrays.stream(brdv).map(solution::getIntVal).mapToInt(i->i).toArray(), solution.getIntVal(totalCost));

    }


    // Reification is a specific process which does not rely on classical constraints.
    // This is why ifThen, ifThenElse, ifOnlyIf and reification return void and do not need to be posted.

//    public static Constraint ifThenElseTransitionCosts(Model model, BRDVCost brdvCost, IntVar brdvVar, IntVar costVar,
//                                                       List<TransitionCost> transitionCosts) {
//        Optional<TransitionCost> transitionCostOpt = transitionCosts.stream().findFirst();
//        if(!transitionCostOpt.isPresent()) {
//            return model.arithm(costVar, "=", brdvCost.getDefaultCost());
//        } else {
//            TransitionCost transitionCost = transitionCostOpt.get();
//            return model.ifThenElse(
//                    model.arithm(brdvVar, "=", transitionCost.getTo()), // Si la variable brdv[i] es tal
//                    model.arithm(costVar, "=", transitionCost.getCost()), // Entonces el coste debe ser este
//                    ifThenElseTransitionCosts(model, brdvCost, brdvVar, costVar, tail(transitionCosts)) // Si no, comprueba siguiente
//            );
//        }
//    }
//
//    public static <T> List<T> tail(List<T> lst) {
//        if(lst.size()>1) return lst.subList(1, lst.size());
//        else return new ArrayList<>();
//    }

}
