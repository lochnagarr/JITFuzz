package utils;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Info{
    public ArrayList<Local> zeroLocals = new ArrayList<>();
    public ArrayList<Local> oneLocals = new ArrayList<>();
    public int[] mutatorsUsed;
    public HashMap<String, Integer> mutateCnt;
    public int newLocalCount;
    public int newFuncCount;
    public int newFieldCount;
    public int objCount;
    public Info father;
    public Info last;

    public static double totalTime;
    public double time = 0;

    public int coverage = 0;
    public int indirectCoverage = 0;

    public int accessTime = 0;
    public int indirectAccessTime = 0;

    public int epoch = 0;
    public double fitness = Double.MIN_VALUE;

    public String seed = "";
    public diff run = null;

    public int abandon = 0;

    public Info(){
        mutatorsUsed = new int[7];
        mutateCnt = new HashMap<>();
        newLocalCount = 0;
        newFuncCount = 0;
        newFieldCount = 0;
        objCount = 0;
        father = null;
    }

    public Info(String seed, int epoch) {
        this();
        this.seed = seed;
        this.epoch = epoch;
    }

    public Info(Info info, int epoch){
        this.mutatorsUsed = new int[7];
        System.arraycopy(info.mutatorsUsed, 0, this.mutatorsUsed, 0, info.mutatorsUsed.length);
        this.mutateCnt = new HashMap<>(info.mutateCnt);
        this.newLocalCount = info.newLocalCount;
        this.newFuncCount = info.newFuncCount;
        this.newFieldCount = info.newFieldCount;
        this.objCount = info.objCount;
        this.father = info;

        this.epoch = epoch;
    }

    public void initMutateCnt(SootClass sootClass) {
        mutateCnt.clear();
        for (SootMethod sm: sootClass.getMethods()) {
            String subSignature = sm.getSubSignature();
            if (subSignature.equals("void log(java.lang.String)")||subSignature.equals("void log(int,java.lang.String)")
                    ||subSignature.equals("void <init>()")||subSignature.equals("void <clinit>()")) {
                continue;
            }
            mutateCnt.put(subSignature, 1);
        }
    }

    public void initMutateCnt(Set<String> liveMethods) {
        mutateCnt.clear();
        for (String s: liveMethods) {
            mutateCnt.put(s, 1);
        }
    }

    public void update(double time, int coverage, int cnt, boolean direct) {
        this.time += time;
        if (direct) {
            this.coverage += coverage;
            this.accessTime ++;
        } else {
            this.indirectCoverage += coverage;
            this.indirectAccessTime ++;
        }
        if (this.father != null && cnt > -1) {
            father.update(time, coverage, cnt-1, false);
        }
    }

    public void calculateFitness(int curEpoch) {
        this.fitness = coverage - 0.01*(curEpoch - epoch);
    }

    public void setTotalTime(double t) {
        totalTime = t;
    }

    public double getFitness() {
        double better;
        if (coverage == 0 && indirectCoverage == 0) {
            better = 1;
        } else if (coverage == 0) {
            better =  0.5;
        } else if (indirectCoverage == 0) {
            better = 2;
        } else {
            better = ((coverage+0.0)/accessTime) / ((indirectCoverage+0.0)/indirectAccessTime);
        }
        return better - 0.1*time/(accessTime+indirectAccessTime);
    }

    public double getFitness(int epoch) {
        return this.getFitness();
    }

    public void abandon() {
        this.abandon ++;
        this.father.abandon++;
    }
}
