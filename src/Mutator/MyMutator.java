package Mutator;

import chooser.stmtChooser;
import soot.jimple.*;
import utils.Bandit;
import utils.Info;
import soot.*;
import soot.jimple.internal.*;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.util.*;

public abstract class MyMutator {
    public static Info info;
    public static ArrayList<Unit> toTrans;
    public static JimpleBody body;
    public static Chain<Local> locals;
    public static PatchingChain<Unit> units;
    public static Unit first;
    public static Random random = new Random();
    public static SootClass mainClass;
    public static SootClass digit;
    public static SootClass randomObj;
    public static stmtChooser chooser;
    public static HashMap<MyMutator, HashSet<String>> cannot = new HashMap<>();


    public static HashMap<String, Integer> mutator2ind = new HashMap<>();
    public static String[] mutators = new String[]{"ScalarMutator", "EscapeMutator", "SimplificationMutator",
            "InlineMutator", "WrapMutator", "TransitionMutator"
    };

    public static Bandit mutatorLocalBandit;

    static {
        for (int i = 0; i < mutators.length; i++) {
            mutator2ind.put(mutators[i], i);
        }
    }

    public static void setBandits (Bandit mutatorLocalBanditt) {
        mutatorLocalBandit = mutatorLocalBanditt;
    }

    public void setChooser(stmtChooser chooser) {
        if (chooser == null) {
            System.out.println("need a chooser");
            return;
        }
        this.chooser = chooser;
    }

    public static void setMainClass(SootClass sc) {
        mainClass = sc;
    }

    public static void setDigit(SootClass d){
        digit = d;
    }

    public static void setRandomObj(SootClass r){
        randomObj = r;
    }

    public static void useMutator(MyMutator transformer){
        String name = transformer.getClass().getSimpleName();
        String subSig = body.getMethod().getSubSignature();
        int ind = mutator2ind.get(name);

        info.mutatorsUsed[ind]++;
        info.mutateCnt.put(subSig, info.mutateCnt.get(subSig)+1);
    }

    public static int lineNum(Unit unit) {
        int line = -1;
        List<Tag> tags = unit.getTags();
        for (Tag t : tags) {
            if (t instanceof LineNumberTag) {
                line = ((LineNumberTag) t).getLineNumber();
                break;
            }
        }
        return line;
    }

    public static ArrayList<ValueBox> getVals(JAssignStmt stmt){
        ArrayList<ValueBox> res = new ArrayList<>();
        for(ValueBox v:stmt.getUseBoxes()){
            if ((v.getValue().getUseBoxes().isEmpty())&&v.getValue() instanceof Immediate)
                res.add(v);
        }
        return res;
    }

    public static Info getInfo(){
        return info;//new Info (zeroLocals, oneLocals, mutatorsUsed, newLocalCount, newFuncCount, newFieldCount, objCount);
    }

    public static void setInfo(Info infoo){
        info = infoo;
    }


    public void setUp(Body body) {
//        zeroLocals = zeroLocalss;
//        oneLocals = oneLocalss;

        setMainClass(body.getMethod().getDeclaringClass());
        MyMutator.body = (JimpleBody) body;
        locals = body.getLocals();
        units = body.getUnits();
        first = MyMutator.body.getFirstNonIdentityStmt();
    }

    public static Local newLocal(String name, int tmp, boolean init, Type t) {
        Local newLocal = Jimple.v().newLocal(name + info.newLocalCount++, t);
        body.getLocals().add(newLocal);
        if (init) {
            AssignStmt ass1 = Jimple.v().newAssignStmt(newLocal, IntConstant.v(tmp));
            units.insertBefore(ass1, first);
        }
        return newLocal;
    }

    public static Local getLocalByName(String name){
        for (Local l:locals){
            if (l.getName().equals(name)){
                return l;
            }
        }
        return null;
    }

    public static void generateZeroLocals(int n) {
        if (info.zeroLocals.size() >= n) {
            return;
        }
        int n1 = n / 2;
        int n2 = n - n1;
        while (n1-- > 0)
            generateZeroAddOrSub(random.nextInt(), random.nextBoolean());
        while (n2-- > 0)
            generateZeroMulOrDiv(random.nextInt(), random.nextBoolean());
    }

    public static void generateOneLocals(int n) {
        if (info.oneLocals.size() >= n) {
            return;
        }
        int n1 = n / 2;
        int n2 = n - n1;
        if (n1 < n2) {
            n1 = n2;
            n2 = n - n1;
        }
        while (n1-- > 0)
            generateOneAddOrSub(random.nextInt(), random.nextBoolean());
        while (n2-- > 0)
            generateOneMulOrDiv(random.nextInt(), random.nextBoolean());
    }

    public static void generateZeroAddOrSub(int rand, boolean add) {
        Local l1 = newLocal("zra",rand, true, IntType.v());
        Local l2 = newLocal("zra", add ? -rand : rand, true, IntType.v());
        Local l = newLocal("zero",0, false, IntType.v());
        JAssignStmt ass = new JAssignStmt(l, add ? new JAddExpr(l1, l2) : new JSubExpr(l1, l2));
        info.zeroLocals.add(l);
        units.insertBefore(ass, first);
    }

    public static void generateZeroMulOrDiv(int rand, boolean mul) {
        Local l1 = newLocal("zrm",0, true, IntType.v());
        Local l2 = newLocal("zrm", rand, true, IntType.v());
        Local l = newLocal("zero", 0, false, IntType.v());
        JAssignStmt ass = new JAssignStmt(l, mul ? new JMulExpr(l1, l2) : new JDivExpr(l1, l2));
        info.zeroLocals.add(l);
        units.insertBefore(ass, first);
    }

    public static void generateOneAddOrSub(int rand, boolean add) {
        Local l1 = info.zeroLocals.get((int) (Math.random() * info.zeroLocals.size()));
        Local l2 = newLocal("onea", add ? 1 : -1, true, IntType.v());
        Local l = newLocal("one",0, false, IntType.v());
        JAssignStmt ass = new JAssignStmt(l, add ? new JAddExpr(l1, l2) : new JSubExpr(l1, l2));
        info.oneLocals.add(l);
        units.insertBefore(ass, first);
    }

    public static void generateOneMulOrDiv(int rand, boolean mul) {
        Local l1;
        Local l2;
        Local l = newLocal("one", 0, false, IntType.v());
        JAssignStmt ass;
        if (mul && !info.oneLocals.isEmpty()) {
            l1 = info.oneLocals.get((int) (Math.random() * info.oneLocals.size()));
            l2 = newLocal("onem", 1, true, IntType.v());
            ass = new JAssignStmt(l, new JMulExpr(l1, l2));
        } else {
            l1 = newLocal("onem",rand, true, IntType.v());
            l2 = newLocal("onem",rand, true, IntType.v());
            ass = new JAssignStmt(l, new JDivExpr(l1, l2));
        }
        info.oneLocals.add(l);
        units.insertBefore(ass, first);
    }

    public static Local castTo(Local toCast, Type type, Unit toInsert, boolean before){
        if (toCast.getType().equals(type)) {
            return toCast;
        }else {
            Local cast = newLocal("$c" + info.newLocalCount++, 0, false, type);
            AssignStmt castAss = Jimple.v().newAssignStmt(cast, Jimple.v().newCastExpr(toCast, type));
            if (before) {
                units.insertBefore(castAss, toInsert);
            }else {
                units.insertAfter(castAss, toInsert);
            }
            return cast;
        }
    }

    public static boolean isNew(Unit u) {
        if (u instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) u).getRightOp();
            return rightOp instanceof NewExpr;
        }
        return false;
    }

    public static boolean isInit(Unit u) {
        if (u instanceof InvokeStmt) {
            InvokeExpr invokeExpr = ((InvokeStmt) u).getInvokeExpr();
            return invokeExpr.getMethodRef().getName().equals("<init>");
        }
        return false;
    }

    public abstract boolean transform(Body body, Unit u);
    public abstract boolean transform(Body body);

    public abstract boolean canApply(Unit u);
    public boolean preWork(Unit u){
        if (isNew(u) || isInit(u) || !canApply(u)) {
//            if (!canApply(u)) {
                if (!cannot.containsKey(this)) {
                    cannot.put(this, new HashSet<>());
            }
            cannot.get(this).add(u.toString());
            return false;
        }
        System.out.println(this+" :"+u);
        useMutator(this);
        return true;
    }

}