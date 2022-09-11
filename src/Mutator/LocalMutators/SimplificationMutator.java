package Mutator.LocalMutators;

import Mutator.LocalMutator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import utils.util;

import java.util.Random;

public class SimplificationMutator extends LocalMutator {
    private static final SimplificationMutator instance = new SimplificationMutator();
    public static SimplificationMutator v() {
        return instance;
    }
    private SimplificationMutator() {}

    public final Random random = new Random();

    @Override
    public boolean transform(Body body, Unit u) {
        setUp(body);
        if (!preWork(u)){
            return false;
        }
        generateZeroLocals(1);
        generateOneLocals(1);
        Stmt stmt = (Stmt) u;
        substituteOperand(stmt);
        substituteExpr(stmt);
        return true;
    }

    public boolean isNum(Type t){
        if (!(t instanceof PrimType)){
            return false;
        }else {
            return !((t instanceof BooleanType)||(t instanceof CharType));
        }
    }

    @Override
    public boolean canApply(Unit u) {
        if (!(u instanceof JAssignStmt)) {
            return false;
        }
        JAssignStmt s = (JAssignStmt) u;
        if (!(s.getLeftOp() instanceof JimpleLocal)){
            return false;
        }

        if (!((s.getLeftOp().getType() instanceof PrimType)&&!(s.getLeftOp().getType() instanceof CharType))){
            return false;
        }
        return true;
    }

    public void substituteExpr(Stmt stmt) {
        if (!(stmt instanceof JAssignStmt) || !(((JAssignStmt) stmt).getLeftOp() instanceof JimpleLocal))
            return;
        Value lval = ((JAssignStmt) stmt).getLeftOp();
        Local toUse;
        AssignStmt ass = null;
        Unit succOf = units.getSuccOf(stmt);
        int rand = Math.abs(random.nextInt());
        if (rand%4<2) {
            toUse = info.zeroLocals.get(util.randomChoice(info.zeroLocals.size()));
        } else {
            toUse = info.oneLocals.get(util.randomChoice(info.oneLocals.size()));
        }

        toUse = castTo(toUse, lval.getType(), succOf, true);

        switch (rand % 4) {
            case 0:
                ass = Jimple.v().newAssignStmt(lval, Jimple.v().newAddExpr(lval, toUse));
                break;
            case 1:
                ass = Jimple.v().newAssignStmt(lval, Jimple.v().newSubExpr(lval, toUse));
                break;
            case 2:
                ass = Jimple.v().newAssignStmt(lval, Jimple.v().newMulExpr(lval, toUse));
                break;
            case 3:
                ass = Jimple.v().newAssignStmt(lval, Jimple.v().newDivExpr(lval, toUse));
        }
        units.insertBefore(ass, succOf);
    }


    public void addOrSubSpec(Stmt ss, Boolean add) {
        JAssignStmt s = (JAssignStmt) ss;
        BinopExpr addExpr = (BinopExpr) s.getRightOp();

        int n1 = Math.abs(random.nextInt());

        Value op1 = addExpr.getOp1();
        Value op2 = addExpr.getOp2();

        int rand = Math.abs(random.nextInt());
        if (op1 instanceof JimpleLocal) {
            Local l1 = newLocal("aos"+info.newLocalCount++,n1, true, op1.getType());
            if (op2 instanceof JimpleLocal) {
                Local l2 = newLocal("aos"+info.newLocalCount++,add ? n1 : -n1, true, op1.getType());
                JAssignStmt ass1 = new JAssignStmt(op1, new JSubExpr(op1, l1));
                JAssignStmt ass2 = new JAssignStmt(op2, new JAddExpr(op2, l2));

                units.insertBefore(ass1, ss);
                units.insertBefore(ass2, ss);
            } else {
                addOrSubOne(ss, op1, op1, rand, l1);
            }
        } else if (op2 instanceof JimpleLocal) {
            Local l2 = newLocal("aos"+info.newLocalCount++,n1, true, op1.getType());
            addOrSubOne(ss, op1, op2, rand, l2);
        }
    }

    private void addOrSubOne(Stmt ss, Value op1, Value op2, int rand, Local l2) {
        Local zero = info.zeroLocals.get(rand % info.zeroLocals.size());
        zero = castTo(zero, op1.getType(), ss, true);
        JAssignStmt ass1 = new JAssignStmt(op2, new JAddExpr(op2, zero));
        switch (rand % 3) {
            case 0:
                break;
            case 1:
                ass1 = new JAssignStmt(op2, new JSubExpr(op2, zero));
                break;
            case 2:
                ass1 = new JAssignStmt(op2, new JMulExpr(op2, l2));
                break;
        }
        units.insertBefore(ass1, ss);
    }

    public void mulOrDivSpec(Stmt ss, Boolean mul) {
        JAssignStmt s = (JAssignStmt) ss;
        BinopExpr mulExpr = (BinopExpr) s.getRightOp();
        Value lval = s.getLeftOp();

        int n1 = Math.abs(random.nextInt()) % 10 + 1;
        int n2 = Math.abs(random.nextInt()) % 10 + 1;

        Value op1 = mulExpr.getOp1();
        Value op2 = mulExpr.getOp2();
        if (op1 instanceof JimpleLocal) {
            Local l1 = newLocal("mod"+info.newLocalCount++,n1, true, op1.getType());
            Local l2 = newLocal("mod"+info.newLocalCount++,0, false, op1.getType());
            if (op2 instanceof JimpleLocal) {
                //            lval = op1 * op2 -> lval = (op1 + l1) * (op2 + l3); lval -= l1 * op2 + l3 * op1 + l1 * l3;
                Local l3 = newLocal("mod"+info.newLocalCount++,n2, true, op1.getType());
                Local l4 = newLocal("mod"+info.newLocalCount++,0, false, op1.getType());
                Local l5 = newLocal("mod"+info.newLocalCount++,0, false, op1.getType());
                JAssignStmt ass1 = new JAssignStmt(op1, new JAddExpr(op1, l1));
                JAssignStmt ass2 = new JAssignStmt(l2, new JMulExpr(l1, op2));

                JAssignStmt ass3 = new JAssignStmt(op2, new JAddExpr(op2, l3));
                JAssignStmt ass4 = new JAssignStmt(l4, new JMulExpr(l3, op1));

                JAssignStmt ass5 = new JAssignStmt(l5, new JMulExpr(l1, l3));

                JAssignStmt ass6 = new JAssignStmt(lval, new JSubExpr(lval, l2));
                JAssignStmt ass7 = new JAssignStmt(lval, new JSubExpr(lval, l4));
                JAssignStmt ass8 = new JAssignStmt(lval, new JSubExpr(lval, l5));

                units.insertBefore(ass1, ss);
                units.insertBefore(ass3, ss);

                units.insertAfter(ass6, ss);
                units.insertAfter(ass7, ss);
                units.insertAfter(ass8, ss);

                units.insertAfter(ass2, ss);
                units.insertAfter(ass4, ss);
                units.insertAfter(ass5, ss);
            } else {
//                lval = op1 * op2 -> lval = (op1 + l1) * op2; lval -= l1 * op2
                mulOrDivOne(ss, lval, op1, op2, l1, l2);
            }
        } else if (op2 instanceof JimpleLocal) {
//            lval = op1 * op2 -> lval = op1 * (op2 + l1); lval -= l1 * op1
            Local l1 = newLocal("mod"+info.newLocalCount++,n1, true, op1.getType());
            Local l2 = newLocal("mod"+info.newLocalCount++,0, false, op1.getType());
            mulOrDivOne(ss, lval, op2, op1, l1, l2);
        }
    }

    private void mulOrDivOne(Stmt ss, Value lval, Value op1, Value op2, Local l1, Local l2) {
        JAssignStmt ass1 = new JAssignStmt(op1, new JAddExpr(op1, l1));
        JAssignStmt ass2 = new JAssignStmt(l2, new JMulExpr(l1, op2));
        JAssignStmt ass3 = new JAssignStmt(lval, new JSubExpr(lval, l2));
        units.insertBefore(ass1, ss);
        units.insertAfter(ass3, ss);
        units.insertAfter(ass2, ss);
    }

    public void substituteOperand(Stmt stmt) {
        if (stmt instanceof JAssignStmt) {
            Value right = ((JAssignStmt) stmt).getRightOp();
            if (right instanceof JAddExpr)
                addOrSubSpec(stmt, true);
            else if (right instanceof JSubExpr)
                addOrSubSpec(stmt, false);
            else if (right instanceof JMulExpr)
                mulOrDivSpec(stmt, true);
        }
    }
}
