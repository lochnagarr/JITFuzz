package Mutator.LocalMutators;

import Mutator.LocalMutator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.ArrayList;
import java.util.List;

public class ScalarMutator extends LocalMutator {
    private static final ScalarMutator instance = new ScalarMutator();
    public static ScalarMutator v() {
        return instance;
    }
    private ScalarMutator() {}

    public SootClass Digitcls;
    public ArrayList<ValueBox> valsToEnc = new ArrayList<>();

    public void encapsulate(Unit u) {
        for (ValueBox vb : valsToEnc) {
//            if (vb instanceof ImmediateBox) {
//                System.out.println(vb);
                Value v = vb.getValue();

                if (v instanceof NumericConstant) {
                    Local tmp = null;
                    Local obj = null;

                    if (v instanceof RealConstant) {
                        if (v instanceof DoubleConstant){
                            tmp = newLocal("enc",0, false, DoubleType.v());
                            obj = addObj(true, (DoubleConstant) v);
                        }else {
                            tmp = newLocal("enc",0, false, FloatType.v());
                            obj = addObj(true, DoubleConstant.v(((FloatConstant)v).value));
                        }
                        AssignStmt value = Jimple.v().newAssignStmt(tmp, new JInstanceFieldRef(obj, Digitcls.getFieldByName("valueDouble").makeRef()));
                        units.insertBefore(value, u);
                    }

                    else if (v instanceof ArithmeticConstant) {
                        if (v instanceof IntConstant){
                            tmp = newLocal("enc",0, false, IntType.v());
                            obj = addObj(false, DoubleConstant.v(((IntConstant)v).value));
                        }else if (v instanceof LongConstant){
                            tmp = newLocal("enc",0, false, LongType.v());
                            obj = addObj(false, DoubleConstant.v(((LongConstant)v).value));
                        }
                        AssignStmt value = Jimple.v().newAssignStmt(tmp, new JInstanceFieldRef(obj, Digitcls.getFieldByName("valueInt").makeRef()));
                        units.insertBefore(value, u);
                    }
                    vb.setValue(tmp);
                }
//            }
        }
    }

    public Local addObj(boolean isDouble, NumericConstant value) {
        Local l1 = Jimple.v().newLocal("$tmpObj" + info.objCount, RefType.v());
        Local l2 = Jimple.v().newLocal("tmpObj" + info.objCount++, RefType.v());
        body.getLocals().add(l1);
        body.getLocals().add(l2);

//        SootClass cls = Scene.v().getSootClass("utils.Digit");
        SootMethod method = null;
        try {
            method = digit.getMethodByName("<init>");
        }catch (Exception e){
            e.printStackTrace();
        }

        int tpval = 0, tpis = 0;
        if (isDouble) {
            tpis = 1;
        }

        AssignStmt newass = Jimple.v().newAssignStmt(l1, Jimple.v().newNewExpr(digit.getType()));
//        SpecialInvokeExpr specialInvokeExpr = Jimple.v().newSpecialInvokeExpr(l1, method.makeRef(), IntConstant.v(tpis), isDouble ? DoubleConstant.v(value) : IntConstant.v(tpval));
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(l1, method.makeRef(), IntConstant.v(tpis), value));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(l2, l1);
        units.insertBefore(newass, first);
        units.insertBefore(invokeStmt, first);
        units.insertBefore(assignStmt, first);

        return l2;
    }

    @Override
    public boolean transform(Body body, Unit u) {
        setUp(body);
        if (!preWork(u)){
            return false;
        }
        this.Digitcls = Scene.v().getSootClass("utils.Digit");
        encapsulate(u);
        return true;
    }

    @Override
    public boolean canApply(Unit u) {
        valsToEnc.clear();
        List<ValueBox> useBoxes = u.getUseBoxes();
        for (ValueBox vb : useBoxes){
            if (vb.getValue() instanceof NumericConstant) {
                valsToEnc.add(vb);
            }
        }
        return !valsToEnc.isEmpty();
    }
}
