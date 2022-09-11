package Mutator.CFGMutators;

import Mutator.CFGMutator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.*;

public class InlineMutator extends CFGMutator {
    private static final InlineMutator instance = new InlineMutator();
    public static InlineMutator v() {
        return instance;
    }
    private InlineMutator() {}

    private boolean st;
    private boolean init;

//    private inlineTransformer() {
//    }
//
//    public static inlineTransformer v() {
//        return instance;
//    }

    public SootMethodRefImpl encapInFunc(JAssignStmt stmt) {
        Value rightOp = stmt.getRightOp();

        ArrayList<Type> paraList = new ArrayList<>();
        ArrayList<ValueBox> useBoxes = getVals(stmt);

        useBoxes.forEach(e -> {
            if (e.getValue() instanceof NullConstant) {
                paraList.add(RefType.v("java.lang.Object"));
            } else {
                paraList.add(e.getValue().getType());
            }
        });

        SootMethod newMethod = new SootMethod("inline" + info.newFuncCount++, paraList, rightOp.getType(), st?9:1);
        mainClass.addMethod(newMethod);

        Body body = Jimple.v().newBody(newMethod);
        newMethod.setActiveBody(body);

        if (!st) {
            Local thisLocal = Jimple.v().newLocal("r0", RefType.v(mainClass.getName()));
            body.getLocals().add(thisLocal);
            body.getUnits().addFirst(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef((RefType) thisLocal.getType())));
        }

        int index = 0, para = 0;
        for (ValueBox vb : useBoxes) {
            Value v = vb.getValue();
            if (v instanceof Immediate) {
                JimpleLocal newLocal;
                if (v instanceof JimpleLocal)
                    newLocal = (JimpleLocal) v.clone();
                else {
                    Type type = v.getType();
                    if (v instanceof NullConstant) {
                        type = RefType.v("java.lang.Object");
                    }
                    newLocal = (JimpleLocal) Jimple.v().newLocal("para" + para++, type);
                }
                body.getLocals().add(newLocal);
                body.getUnits().addLast(Jimple.v().newIdentityStmt(newLocal, Jimple.v().newParameterRef(newLocal.getType(), index++)));
                if (rightOp.equals(v))
                    rightOp = newLocal;
                for (ValueBox valueBox:stmt.getUseBoxes()){
                    if (valueBox.getValue().equals(v))
                        valueBox.setValue(newLocal);
                }
            }
        }

        Local rt = Jimple.v().newLocal("rt", rightOp.getType());
        body.getLocals().add(rt);

        AssignStmt assignStmt = Jimple.v().newAssignStmt(rt, rightOp);
        ReturnStmt returnStmt = Jimple.v().newReturnStmt(rt);

        body.getUnits().addLast(assignStmt);
        body.getUnits().addLast(returnStmt);

        SootMethodRefImpl sootMethodRef = new SootMethodRefImpl(mainClass, newMethod.getName(), paraList, rightOp.getType(), st);
        return sootMethodRef;
    }

    @Override
    public boolean transform(Body body, Unit u) {
        if (!preWork(u)){
            return false;
        }
        setUp(body);
        st = body.getMethod().isStatic();
        init = body.getMethod().getName().equals("<init>");
        st |= init;

        if (u instanceof JAssignStmt) {
            JAssignStmt s = (JAssignStmt) u;

            if (!(s.getLeftOp() instanceof Immediate))
                return false;

            SootMethodRefImpl sootMethodRef = encapInFunc((JAssignStmt) s.clone());

            ArrayList<ValueBox> useBoxes = getVals(s);
            ArrayList<Value> paraList = new ArrayList<>();
            for (ValueBox vb:useBoxes){
                Value value = vb.getValue();
                if (value instanceof Immediate)
                    paraList.add(value);
                else {
                    Local local = newLocal("inl",0, false, IntType.v());
                    AssignStmt assignStmt = Jimple.v().newAssignStmt(local, value);
                    units.insertBefore(assignStmt, u);
                    paraList.add(local);
                }
            }

            InvokeExpr invokeExpr = st?Jimple.v().newStaticInvokeExpr(sootMethodRef, paraList) : Jimple.v().newVirtualInvokeExpr(body.getThisLocal(), sootMethodRef, paraList);

            s.setRightOp(invokeExpr);
        }
        return true;
    }

    @Override
    public boolean canApply(Unit u) {
        if (!(u instanceof JAssignStmt)){
            return false;
        }

        JAssignStmt as = (JAssignStmt) u;
        Value rightOp = as.getRightOp();

        if (rightOp instanceof JInvokeStmt && rightOp.toString().contains("void <init>")){
            return false;
        }

        if (rightOp instanceof JNewExpr){
            return false;
        }
        return true;
    }
}
