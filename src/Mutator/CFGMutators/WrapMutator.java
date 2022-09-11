package Mutator.CFGMutators;

import Mutator.CFGMutator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import utils.util;

public class WrapMutator extends CFGMutator {
    private static final WrapMutator instance = new WrapMutator();
    public static WrapMutator v() {
        return instance;
    }
    private WrapMutator() {}

    public int loopNum = 1;
    public int depth = 2;

    public boolean transform_loop(Body body, Unit u) {
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

    private void identityGoto(Unit u, boolean condition) {
        Local cond;
        if (condition) {
            cond = info.oneLocals.get((int) (Math.random() * info.oneLocals.size()));
        } else {
            cond = info.zeroLocals.get((int) (Math.random() * info.zeroLocals.size()));
        }
        NopStmt nopStmt = Jimple.v().newNopStmt();
        units.insertAfter(nopStmt, u);
        if (condition) {
            /*
             *                            stmt
             *      stmt  =>  (goAfter2)  if true goto goBack
             *                (goAfter)   if true goto succ
             *      succ      (begin)     stmt
             *                (goBack)    if true goto goAfter
             *                (succ)      succ
             */
            Unit succ = units.getSuccOf(u);
            IfStmt goAfter = Jimple.v().newIfStmt(Jimple.v().newEqExpr(cond, IntConstant.v(1)), succ);
            IfStmt goBack = Jimple.v().newIfStmt(Jimple.v().newEqExpr(cond, IntConstant.v(1)), goAfter);
            IfStmt goAfter2 = Jimple.v().newIfStmt(Jimple.v().newEqExpr(cond, IntConstant.v(1)), goBack);
            units.insertBefore((Unit) u.clone(), u);
            units.insertBeforeNoRedirect(goAfter2, u);
            units.insertBeforeNoRedirect(goAfter, u);
            units.insertAfter(goBack, u);
        } else {

            Local newLocal = info.zeroLocals.get(util.randomChoice(info.zeroLocals.size()));
//            Local newLocal = newLocal(0, true);
            Unit succOf = units.getSuccOf(u);
            IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(newLocal, IntConstant.v(0)), succOf);
            units.insertBefore((Unit) u.clone(), u);
            units.insertBeforeNoRedirect(ifStmt, u);
        }
    }

    public boolean transform_branch(Body body, Unit u) {
        generateZeroLocals(1);
        generateOneLocals(1);
        int time = 2;
        while (time-- > 0) {
            identityGoto(u, random.nextBoolean());
        }
        return true;
    }

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
        boolean res = false;
        for (int i = 0; i < depth; i++) {
            if (random.nextBoolean()) {
                res |= transform_branch(body, u);
            } else {
                res |= transform_loop(body, u);
            }
        }
        return res;
    }
}
