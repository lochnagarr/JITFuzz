package Mutator.CFGMutators;

import Mutator.CFGMutator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;

public class LoopMutator extends CFGMutator {
    private static final LoopMutator instance = new LoopMutator();
    public static LoopMutator v() {
        return instance;
    }
    private LoopMutator() {}

    public int loopNum = 1;

//    public addLoopTransformer() {
//    }
//
//    public static addLoopTransformer v() {
//        return instance;
//    }

    public boolean canApply(Unit u){
        if (u instanceof JInvokeStmt && u.toString().contains("void <init>")){
            return false;
        }

        for (ValueBox vb:u.getUseAndDefBoxes()){
            Value value = vb.getValue();
            if (value instanceof JNewExpr){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean transform(Body body, Unit u) {
        setUp(body);
        if (!preWork(u)){
            return false;
        }
//        add local var
        Local tmp = Jimple.v().newLocal("lp", IntType.v());
        body.getLocals().add(tmp);
        AssignStmt ass1 = Jimple.v().newAssignStmt(tmp, IntConstant.v(0));
        units.insertBefore(ass1, ((JimpleBody) body).getFirstNonIdentityStmt());

        NopStmt nopStmt = Jimple.v().newNopStmt();
        units.insertAfter(nopStmt, u);
        Unit succOf = body.getUnits().getSuccOf(u);
        AssignStmt inc1 = Jimple.v().newAssignStmt(tmp, Jimple.v().newAddExpr(tmp, IntConstant.v(1)));
        IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newGeExpr(tmp, IntConstant.v(loopNum-1)), succOf);
        GotoStmt gotoStmt = Jimple.v().newGotoStmt(ifStmt);
        units.insertBefore((Unit) u.clone(), u);
        units.insertBefore(ifStmt, u);
        units.insertAfter(gotoStmt, u);
        units.insertAfter(inc1, u);
        return true;

    }
}
