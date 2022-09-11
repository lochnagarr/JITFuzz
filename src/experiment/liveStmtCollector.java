package experiment;

import Mutator.CFGShimpleMutator;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.Block;

import java.util.*;

import utils.util;

public class liveStmtCollector {
    public static CFGShimpleMutator v = new CFGShimpleMutator();
    public static String cp;
    public static String unitPath;
    public static HashSet<String> liveMethods = new HashSet<>();
    public static String cmd;
    public static String tmp;

    public void setLiveMethods (HashSet<String> liveMethods1) {
        liveMethods = liveMethods1;
    }

    public static List<Block> instrument(SootClass mainClass, SootMethod sm) {
//        liveStmtsOf.put(sm.getSubSignature(), v.blocks);

        SootMethod log = mainClass.getMethod("void log(int,java.lang.String)");
        SootMethodRef sootMethodRef = log.makeRef();

        v.setUp(sm.retrieveActiveBody());
        for (int i = 1; i < v.blocks.size(); i++) {
            Block b = v.blocks.get(i);
            StaticInvokeExpr virtualInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethodRef, IntConstant.v(i), StringConstant.v(sm.getSubSignature()+"%%"));
            InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(virtualInvokeExpr);
            Unit tail = b.getTail();
            String s = tail.toString();
            if (!(s.contains("@caughtexception"))||s.contains("throw ")&&(!b.getTail().equals(b.getHead()))) {
                v.units.insertBefore(invokeStmt, b.getTail());
            }
        }
        return v.blocks;
    }

    public static void instrumentLiveMethod(SootClass mainClass) {
        SootMethod log = mainClass.getMethod("void log(java.lang.String)");
        SootMethodRef sootMethodRef = log.makeRef();

        for (SootMethod sm: mainClass.getMethods()) {
            String subSignature = sm.getSubSignature();
            if (subSignature.equals("void log(java.lang.String)")||subSignature.equals("void log(int,java.lang.String)")
                ||subSignature.equals("void <init>()")||subSignature.equals("void <clinit>()")) {
                continue;
            }
            JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
            UnitPatchingChain units = body.getUnits();
            StaticInvokeExpr virtualInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethodRef, StringConstant.v("\nLive Method: "+sm.getSubSignature()));
            InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(virtualInvokeExpr);
            units.insertAfter(invokeStmt, body.getFirstNonIdentityStmt());
            sm.setActiveBody(body);
        }
    }

    public static void generateLogFunc(SootClass mainClass) {

        SootMethod newMethod = new SootMethod("log", new ArrayList<>(Arrays.asList(IntType.v(), RefType.v("java.lang.String"))), VoidType.v(), Modifier.PUBLIC|Modifier.STATIC);
        mainClass.addMethod(newMethod);

        Body newBody = Jimple.v().newBody(newMethod);
        newMethod.setActiveBody(newBody);

        Local blkInd = Jimple.v().newLocal("ind", IntType.v());
        Local methodName = Jimple.v().newLocal("method", RefType.v());
        newBody.getLocals().add(blkInd);
        newBody.getLocals().add(methodName);
//        System.err
        newBody.getUnits().addLast(Jimple.v().newIdentityStmt(blkInd, Jimple.v().newParameterRef(IntType.v(), 0)));
        newBody.getUnits().addLast(Jimple.v().newIdentityStmt(methodName, Jimple.v().newParameterRef(RefType.v(), 1)));
//        system.out.print
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        newBody.getUnits().add(Jimple.v().newAssignStmt(tmpRef, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));
        newBody.getLocals().add(tmpRef);
        SootMethod soutStr = Scene.v().getMethod("<java.io.PrintStream: void print(java.lang.String)>");
        SootMethod sout = Scene.v().getMethod("<java.io.PrintStream: void println(int)>");

        newBody.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, soutStr.makeRef(), StringConstant.v("Block entered%%"))));
        newBody.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, soutStr.makeRef(), methodName)));
        newBody.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, sout.makeRef(), blkInd)));
        newBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());
    }

    public static void generateLiveMethodFunc(SootClass mainClass) {
        SootMethod newMethod = new SootMethod("log", new ArrayList<>(List.of(RefType.v("java.lang.String"))), VoidType.v(), Modifier.PUBLIC|Modifier.STATIC);
        mainClass.addMethod(newMethod);

        Body newBody = Jimple.v().newBody(newMethod);
        newMethod.setActiveBody(newBody);

        Local methodName = Jimple.v().newLocal("method", RefType.v());
        newBody.getLocals().add(methodName);
        newBody.getUnits().addLast(Jimple.v().newIdentityStmt(methodName, Jimple.v().newParameterRef(RefType.v(), 0)));
//        system.out.print
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        newBody.getUnits().add(Jimple.v().newAssignStmt(tmpRef, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));
        newBody.getLocals().add(tmpRef);
        SootMethod soutStr = Scene.v().getMethod("<java.io.PrintStream: void println(java.lang.String)>");

        newBody.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, soutStr.makeRef(), methodName)));
        newBody.getUnits().add(Jimple.v().newReturnVoidStmt());
    }

    public  HashMap<String, HashSet<String>> collectLiveStmt(SootClass mainClass) {
//        cp = " -cp "+fuzz+"/tmpLive:"+fuzz+"/junit.jar:"+fuzz+"/hamcrest.jar:"+sourceDirectory+" ";
//        unitPath = unitPathh;
        HashMap<String, HashSet<Integer>> enteredBlocks = new HashMap<>();
        HashMap<String, HashSet<String>> ret = new HashMap<>();
        HashMap<String, List<Block>> blocks = new HashMap<>();

//        Body body = sm.retrieveActiveBody();
//        v.setUp(body);
//        System.out.println(sm.getSubSignature()+"\nblocks: "+v.blocks.size()+"; units: "+body.getUnits().size()+"; cnt: "+cnt);

        if (!mainClass.declaresMethod("void log(int,java.lang.String)")) {
            generateLogFunc(mainClass);
        }
        for (String subSig:liveMethods) {
            blocks.put(subSig, instrument(mainClass, mainClass.getMethod(subSig)));
        }
        util.writeBack(mainClass, "tmpLive");

//        String cmd = "java" + cp  + "org.junit.runner.JUnitCore "+unitPath;

        ArrayList<String> res = util.exec(cmd);
        String line1;
        int cnt = 0;
        for(int i=2;i<res.size();i++) {
            line1 = res.get(i);
            if (cnt++<20&&line1.toLowerCase().contains("java.lang.verifyerror")) {
                HashMap<String, HashSet<String>> verifyError = new HashMap<>();
                verifyError.put("error", null);
                return verifyError;
            }
            if (!(line1.startsWith("Block entered")||line1.startsWith(".Block entered"))) {
                continue;
            }
            String[] split = line1.split("%%");
            if (split.length!=3) {
                continue;
            }
            try {
                String subSig = split[1];
                String blk = split[2];
                if (!enteredBlocks.containsKey(subSig)) {
                    enteredBlocks.put(subSig, new HashSet<>());
                }
                enteredBlocks.get(subSig).add(Integer.parseInt(blk));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String subSig:enteredBlocks.keySet()) {
            if (!blocks.containsKey(subSig)) {
                continue;
            }
            if (!ret.containsKey(subSig)) {
                ret.put(subSig, new HashSet<>());
            }
            HashSet<String> stmts = ret.get(subSig);
            for (int i:enteredBlocks.get(subSig)) {
                if (i>=blocks.get(subSig).size()) {
                    continue;
                }
                for (Unit u : blocks.get(subSig).get(i)) {
                    String s = u.toString();
                    if (!s.contains("void log(int") && !s.contains("@this:") && !s.contains("return")&&!s.contains("@parameter")) {
                        stmts.add(u.toString());
                    }
                }
            }
        }
        return ret;
    }

    public HashSet<String> liveMethods(SootClass mainClass) {
//        cp = " -cp "+fuzz+"/tmpLive:"+fuzz+"/junit.jar:"+fuzz+"/hamcrest.jar:"+sourceDirectory+" ";
//        unitPath = unitPathh;

        if (!mainClass.declaresMethod("void log(java.lang.String)")){
            generateLiveMethodFunc(mainClass);
            instrumentLiveMethod(mainClass);
        }
        util.writeBack(mainClass, tmp+"/tmpLive");
//        String cmd = "java" + cp  + "org.junit.runner.JUnitCore "+unitPath;

        HashSet<String> rt = new HashSet<>();
        ArrayList<String> res = util.exec(cmd);
        String line1;
        for(int i=2;i<res.size();i++) {
            line1 = res.get(i);
            if (!line1.startsWith("Live Method: ")) {
                continue;
            }
            String subSig = line1.split("Live Method: ")[1];
            if (!subSig.contains("main")) {
                rt.add(subSig);
            }
        }
        return rt;
    }

    public void setCmd(String cmdd) {
        cmd = cmdd;
    }
}
