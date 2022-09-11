package Mutator.LocalMutators;

import Mutator.LocalMutator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.*;

/*
    class Test {
        public void test() {
            Object i0 = new Object();
        }
    }

jimple:
    $r1 = new java.lang.Object;
    specialinvoke $r1.<java.lang.Object: void <init>()>();
    i0 = $r1

    ||
    ||
   \  /
    \/

    class Test {
        Object object;
        public void test() {
            this.object = new Object();
            Object i0 = this.object;
        }
    }
 */

public class EscapeMutator extends LocalMutator {
    private static final EscapeMutator instance = new EscapeMutator();
    public static EscapeMutator v() {
        return instance;
    }
    private EscapeMutator() {}

    public int modifier; // static method or not?
//    the value that is assigned. e.g. for i0 = 0; newAssign is i0.
    public Value newAssign;
//    the assignment stmt. like: $r1 = new java.lang.Object; or i0 = 0;
//    for specialinvoke $r1.<java.lang.Object: void <init>()>();,
//    newAssignUnit is the corresponding $r1 = new java.lang.Object;.
    public Unit newAssignUnit;
    public boolean obj = false;
    public boolean init = false;

//    if u is a stmt like: specialinvoke $r1.<java.lang.Object: void <init>()>();
    private boolean isNewInvoke(Unit u) {
        String name = ((JInvokeStmt) u).getInvokeExpr().getMethodRef().getName();
        return name.equals("<init>");
    }

    public Value isNewAssignOf(Unit u, Value base) {
        if (u instanceof JAssignStmt) {
            JAssignStmt ass = (JAssignStmt) u;
            if (base.equivTo(ass.rightBox.getValue())) {
                Value value = ass.leftBox.getValue();
                if (value instanceof Local) {
                    return value;
                }
            }
            return null;
        }
        return null;
    }

    public void changeUseBox(Unit u, FieldRef fieldRef) {
        List<ValueBox> useBoxes = u.getUseBoxes();
        Local tmp = null;
        for (ValueBox vb:useBoxes){
            Value value = vb.getValue();
            if (value.equals(newAssign)) {
                if (tmp == null) {
                    tmp = newLocal("$tmp", 0, false, value.getType());
                    JAssignStmt tmpAssign = new JAssignStmt(tmp, fieldRef);
                    units.insertBefore(tmpAssign, u);
                }
                vb.setValue(tmp);
            }
        }
    }

    public Unit changeOriginAssign(Unit u, Value fieldRef) {
        if (u instanceof JAssignStmt) {
            JAssignStmt ss = (JAssignStmt) u;
            if (ss.getLeftOp().equals(newAssign)) {
                AssignStmt assignStmt, res;
                Value rightOp = ss.getRightOp();
                if (rightOp instanceof Local || rightOp instanceof Constant) {
                    assignStmt = Jimple.v().newAssignStmt(fieldRef, rightOp);
                    res = assignStmt;
                } else {
                    Local tmp = newLocal("$tmp", 0, false, newAssign.getType());
                    AssignStmt tmpAss = Jimple.v().newAssignStmt(tmp, rightOp);
                    units.insertBefore(tmpAss, u);
                    assignStmt = Jimple.v().newAssignStmt(fieldRef, tmp);
                    res = tmpAss;
                }

                units.insertBefore(assignStmt, u);
                units.remove(u);
                toTrans.remove(u);
                toTrans.add(assignStmt);
                return res;
            }
        }
        return null;
    }

    public void funcToClsObj() {
        FieldRef fieldRef = addField(newAssign.getType());

//        Iterator<Unit> unitIterator = units.snapshotIterator();
//        while (unitIterator.hasNext()) {
//            Unit u = unitIterator.next();
//            if (u.equals(newAssignUnit)) {
////                JAssignStmt ss = (JAssignStmt) u;
////                AssignStmt assignStmt = Jimple.v().newAssignStmt(fieldRef, ss.getRightOp());
////                units.insertBefore(assignStmt, u);
////                units.remove(u);
//                Unit newStart = changeOriginAssign(u, fieldRef);
//                if (newStart != null) {
//                    while (unitIterator.hasNext()&&!unitIterator.next().equals(newStart)) {}
//                    unitIterator.next();
//                }
//                break;
//            }
//        }
        Unit u = body.getFirstNonIdentityStmt();
        while ((u = units.getSuccOf(u))!=null) {
            Unit tmp = changeOriginAssign(u, fieldRef);
            if (tmp != null) {
                u = tmp;
            }
            changeUseBox(u, fieldRef);
        }
    }

    public FieldRef addField(Type type) {
        boolean isStatic = Modifier.isStatic(modifier);
        init = body.getMethod().getName().equals("<init>");
        isStatic |= init;

        String name = "escapeField" + info.newFieldCount++;
        while (mainClass.declaresFieldByName(name)) {
            name = "escapeField" + info.newFieldCount++;
        }
        SootField newField = new SootField(name, type, modifier);
        mainClass.addField(newField);
        SootFieldRef newFieldRef = newField.makeRef();

        if (isStatic)
            return Jimple.v().newStaticFieldRef(newFieldRef);
        else
            return Jimple.v().newInstanceFieldRef(body.getThisLocal(), newFieldRef);
    }

    @Override
    public boolean transform(Body body, Unit u) {
        setUp(body);
        if (!preWork(u)){
            return false;
        }
        this.modifier = body.getMethod().getModifiers();
        funcToClsObj();
        return true;
    }

    @Override
    public boolean canApply(Unit u) {
//        reset
        this.newAssign = null;
        this.newAssignUnit = null;
//        if u is new invoke stmt. e.g. specialinvoke $r1.<java.lang.Object: void <init>()>();
        if ((u instanceof JInvokeStmt) && isNewInvoke(u)) {
            return handleInvoke(u);
//        if u is assignment stmt. e.g. $r1 = new java.lang.Object; or i0 = 0;
        } else if (u instanceof JAssignStmt) {
            return handleAssign(u);
        }
        return false;
    }

//    u: specialinvoke $r1.<java.lang.Object: void <init>()>();
//    traverse reversely to get the: $r1 = new java.lang.Object;
    public boolean handleInvoke(Unit u) {
        SpecialInvokeExpr invokeExpr = (SpecialInvokeExpr)(((JInvokeStmt) u).getInvokeExpr());
        Value base = invokeExpr.getBase(); //base: $r1
        while ((u = units.getSuccOf(u))!=null) {
            newAssign = isNewAssignOf(u, base);
            if (newAssign != null) {
                newAssignUnit = u;
                obj = true;
                return true;
            }
        }
        return false;
    }

//    u: $r1 = new java.lang.Object; or i0 = 0;
    public boolean handleAssign(Unit u) {
        JAssignStmt s = (JAssignStmt) u;
//        case $r1 = new java.lang.Object;
        if (s.rightBox.getValue() instanceof JNewExpr) {
            Value value = s.leftBox.getValue();
            Iterator<Unit> unitIterator = units.snapshotIterator();
            while (unitIterator.hasNext()&&!unitIterator.next().equals(u)) {}
            while (unitIterator.hasNext()) {
                Unit next = unitIterator.next();
                if (next instanceof JInvokeStmt) {
                    JInvokeStmt ss = (JInvokeStmt) next;
                    if (ss.getInvokeExpr() instanceof JSpecialInvokeExpr &&
                            ((JSpecialInvokeExpr) ss.getInvokeExpr()).getBase().equals(value)) {
                        return handleInvoke(next);
                    }
                }
            }
            return false;
//            case i0 = 0; (not new stmt)
        } else {
            Value lvalue = s.leftBox.getValue(); // lvalue: i0
            if (lvalue instanceof JimpleLocal) {
                newAssign = lvalue;
                newAssignUnit = u;
//                lvalue is PrimType
                if (newAssign.getType() instanceof PrimType) {
                    obj = false;
//                lvalue is object
                } else if (newAssign.getType() instanceof RefType) {
                    obj = true;
                } else {
                    return false;
                }
                return true;
            }
            return false;
        }
    }
}
