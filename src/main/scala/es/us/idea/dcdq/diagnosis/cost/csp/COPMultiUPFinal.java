package es.us.idea.dcdq.diagnosis.cost.csp;

import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.BRDVCost;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.TransitionCost;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.solution.CodifiedMultiOutputSolution;
import es.us.idea.dcdq.diagnosis.cost.csp.codified.solution.CodifiedSolution;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Collectors;

public class COPMultiUPFinal {

    public static CodifiedMultiOutputSolution runCop(int[][] OUP, int[][] TUP, int[] R, BRDVCost[] CMOD, int[] domainBounding) {
        final int NOB = OUP.length;
        final int N = OUP[0].length; // TODO find better way to get N
        final int NTA = TUP.length;


        Model model = new Model("DC/DQ - Multiple");

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
                int[] thisTup = TUP[iTup];
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
                            model.and(model.arithm(t, "!=", currentObserved), model.and(notTo.toArray(new Constraint[0]))),
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
        solver.printStatistics();
        Solution solution = solver.findOptimalSolution(totalCost, Model.MINIMIZE);

        //Solution solutions = solver.findAllOptimalSolutions(totalCost, Model.MINIMIZE);
        //System.out.println(model.toString());
        //System.out.println(solution.toString());
        //System.out.println(solution.getIntVal(totalCost));

        //int[][] tSol = Arrays.stream(T).map(x -> Arrays.stream(x).map(solution::getIntVal)).collect(Collectors.toList());

        //int[][] tSol = Arrays.stream(T).map(x -> Arrays.stream(x).map(solution::getIntVal).collect(Collectors.toList())).collect(Collectors.toList());

        Integer[][] tSol = Arrays.stream(T).map(x -> Arrays.stream(x).map(solution::getIntVal).toArray(Integer[]::new)).toArray(Integer[][]::new);
        Integer[][] ctSol = Arrays.stream(CT).map(x -> Arrays.stream(x).map(solution::getIntVal).toArray(Integer[]::new)).toArray(Integer[][]::new);


        // System.out.println(model.toString());
        // System.out.println(solution.toString());
        // System.out.println(solution.getIntVal(totalCost));

        return new CodifiedMultiOutputSolution(tSol, solution.getIntVal(totalCost), ctSol);

    }



}
