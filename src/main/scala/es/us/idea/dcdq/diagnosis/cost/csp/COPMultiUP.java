package es.us.idea.dcdq.diagnosis.cost.csp;

import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.BRDVCost;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.TransitionCost;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.solution.CodifiedSolution;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class COPMultiUP {

    public static void main(String[] args) {
        System.out.println("Hello world");

        Integer[][] OUP = new Integer[][]{
                new Integer[]{1, 1, 0, 0},
                new Integer[]{1, 0, 1, 0},
                new Integer[]{1, 1, 1, 0},
                new Integer[]{1, 0, 0, 1},
                new Integer[]{1, 0, 1, 1},
        };

        Integer[] R = new Integer[] {100, 200, 300, 400, 500};

        Integer[][] TUP = new Integer[][]{
                new Integer[]{1, 1, 1, 1},
                new Integer[]{1, 1, 1, 0},
                //new Integer[]{1, 1, 0, 0}
        };

        final int NOB = OUP.length;
        final int N = 4;
        final int NTA = TUP.length;

        BRDVCost[] CMOD = new BRDVCost[]{
                new BRDVCost(new ArrayList<>(Arrays.asList(new TransitionCost(0, 1, 5, true))), 10, true),
                new BRDVCost(new ArrayList<>(Arrays.asList(new TransitionCost(0, 1, 2, false))), 10, true),
                new BRDVCost(new ArrayList<>(Arrays.asList(new TransitionCost(0, 1, 2, false))), 10, true),
                new BRDVCost(new ArrayList<>(Arrays.asList(new TransitionCost(0, 1, 7, true))), 10, true)
        };


        Model model = new Model("my problem");

        // dim1 = num OUP, dim2 = num BRDV
        IntVar[][] T = model.intVarMatrix("Transitions", NOB, N, 0, 10);
        // each position = 1 cost for that transition. OJO!! Está traspuesta respecto a T!! (cada fila = 1 BRDV!!!!)
        IntVar[][] CT = model.intVarMatrix("TransitionsCosts", N, NOB, 0, IntVar.MAX_INT_BOUND);
        // each position = 1 BRDV
        IntVar[] C = model.intVarArray("BRDVCosts",N, 0, IntVar.MAX_INT_BOUND);

        // constraints to bound the domain of each T
        for(int iT=0; iT < T.length; iT ++) {
            IntVar[] t = T[iT];
            Constraint[] or = new Constraint[TUP.length];
            for(int iTup = 0; iTup<TUP.length; iTup++) {
                Integer[] thisTup = TUP[iTup];
                Constraint[] and = new Constraint[thisTup.length];
                for(int i=0;i<thisTup.length;i++) {
                    and[i] = model.arithm(t[i], "=", thisTup[i]);
                }
                or[iTup] = model.and(and);
            }
            model.or(or).post();
        }

        // constraints for the cost
        for(int n = 0; n<N; n++) {
            //IntVar c = C[n]; // cost for this brdv
            // filter simple and global costs
            BRDVCost brdvCost = CMOD[n];
            List<TransitionCost> simple = brdvCost.getTransitionCosts().stream().filter(TransitionCost::getSingle).collect(Collectors.toList());
            List<TransitionCost> global = brdvCost.getTransitionCosts().stream().filter(x -> !x.getSingle()).collect(Collectors.toList());

            // Paso 1 agrupo por observed value para esta brdv
            // esta brdv
            // Integer[] oupToThisBrdv = new Integer[];
            Map<Integer, List<IntVar>> transitionsByObserved = new HashMap<>();

            int finalN = n;
            for(Integer i: Arrays.stream(OUP).map(x -> x[finalN]).collect(Collectors.toSet()))
                transitionsByObserved.put(i, new ArrayList<>());

            for(int ob=0; ob<NOB; ob++ ) {
                Integer currentObserved = OUP[ob][n];
                IntVar t = T[ob][n];

                List<IntVar> alreadyProcessed = transitionsByObserved.get(currentObserved);
                List<TransitionCost> globalThisVal = global.stream().filter(x -> x.getFrom().equals(currentObserved)).collect(Collectors.toList());
                List<TransitionCost> simpleThisVal = simple.stream().filter(x -> x.getFrom().equals(currentObserved)).collect(Collectors.toList());

                List<Constraint> notTo = new ArrayList<>();

                // Compare t to already processed target values for this observed value
                Constraint[] orThisValAlreadyProcessed = new Constraint[alreadyProcessed.size()];
                for(int i = 0; i<alreadyProcessed.size(); i++)
                    orThisValAlreadyProcessed[i] = model.arithm(t, "!=", alreadyProcessed.get(i));

                // Generate and post constraints for global vals
                for(TransitionCost tc: globalThisVal) {

                    if(orThisValAlreadyProcessed.length > 0){
                        model.ifThen(
                                model.and(
                                        model.arithm(t, "=", tc.getTo()),
                                        model.arithm(t, "!=", currentObserved),
                                        model.or(orThisValAlreadyProcessed)
                                ),
                                model.arithm(CT[n][ob], "=", tc.getCost())
                        );
                    } else {
                        model.ifThen(
                                model.and(
                                        model.arithm(t, "=", tc.getTo()),
                                        model.arithm(t, "!=", currentObserved)
                                ),
                                model.arithm(CT[n][ob], "=", tc.getCost())
                        );
                    }

                    notTo.add(model.arithm(t, "!=", tc.getTo()));
                }

                if(globalThisVal.size() > 0)
                    alreadyProcessed.add(t);

                // Generate and post constraints for simple vals
                for(TransitionCost tc: simpleThisVal) {
                    model.ifThen(
                            model.and(
                                    model.arithm(t, "=", tc.getTo()),
                                    model.arithm(t, "!=", currentObserved)
                            ),
                            model.arithm(CT[n][ob], "=", tc.getCost() * R[ob])
                    );
                    notTo.add(model.arithm(t, "!=", tc.getTo()));
                }

                // Default
                if(notTo.size() > 0)
                    model.ifThen(
                            model.and(model.arithm(t, "!=", currentObserved), model.or(notTo.toArray(new Constraint[0]))),
                            model.arithm(CT[n][ob], "=", brdvCost.getDefaultCost() * R[ob])
                    );
                else
                    model.ifThen(
                            model.and(model.arithm(t, "!=", currentObserved)),
                            model.arithm(CT[n][ob], "=", brdvCost.getDefaultCost() * R[ob])
                    );
                model.ifThen(model.arithm(t, "=", currentObserved), model.arithm(CT[n][ob], "=", 0));
            }
            model.sum(CT[n], "=", C[n]).post();
        }

        // variable objetivo
        IntVar totalCost = model.intVar("Total Cost", IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
        model.sum(C, "=", totalCost).post();
        model.setObjective(Model.MINIMIZE, totalCost);

        Solver solver = model.getSolver();
        Solution solution = solver.findOptimalSolution(totalCost, Model.MINIMIZE);
        //Solution solutions = solver.findAllOptimalSolutions(totalCost, Model.MINIMIZE);
        System.out.println(model.toString());
        System.out.println(solution.toString());
        System.out.println(solution.getIntVal(totalCost));

        Arrays.stream(T).map(x -> Arrays.stream(x).map(solution::getIntVal).collect(Collectors.toList())).forEach(p -> System.out.println(p));

        //return new CodifiedSolution(Arrays.stream(brdv).map(solution::getIntVal).mapToInt(i->i).toArray(), solution.getIntVal(totalCost));

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
