package Mutator;

import chooser.blockChooser;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JIdentityStmt;
import soot.shimple.ShimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.CompleteBlockGraph;

import java.util.*;

public class CFGShimpleMutator extends CFGMutator {
    private static final CFGShimpleMutator instance = new CFGShimpleMutator();
    public static boolean fix = false;

    public blockChooser blockChooser;
    public ArrayList<Block> blocksToTrans = new ArrayList<>();
    public BlockGraph graph;
    public Map<Unit, Block> unitToBlock;
    public List<Block> blocks;

    public CFGShimpleMutator() {
    }

    public static CFGShimpleMutator v() {
        return instance;
    }

    public void setUp(Body body) {
        MyMutator.body = (JimpleBody) body;
        locals = MyMutator.body.getLocals();
        units = MyMutator.body.getUnits();
        first = MyMutator.body.getFirstNonIdentityStmt();
//        this.sGraph = new CompleteBlockGraph(sbody);
        this.graph = new CompleteBlockGraph(MyMutator.body);
        this.blocks = graph.getBlocks();
//        this.sUitToBlock = getUnitToBlockMap(sGraph);
        this.unitToBlock = getUnitToBlockMap(graph);
        setMainClass(body.getMethod().getDeclaringClass());

    }

    public Map<Unit, Block> getUnitToBlockMap(BlockGraph blocks) {
        Map<Unit, Block> unitToBlock = new HashMap();
        for (Block block : blocks)
            for (Unit unit : block)
                unitToBlock.put(unit, block);
        return unitToBlock;
    }

    public Block addEdgeJimple(Block src, Block dst) {
        Local backCount = Jimple.v().newLocal("bkcnt" + info.newLocalCount++, IntType.v());
        body.getLocals().add(backCount);
        AssignStmt ass = Jimple.v().newAssignStmt(backCount, IntConstant.v(0));
        units.insertBefore(ass, body.getFirstNonIdentityStmt());

        Unit tail = src.getTail();
        Unit next = units.getSuccOf(tail);
        Unit head = dst.getHead();

        LeExpr leExpr = Jimple.v().newLeExpr(backCount, IntConstant.v(2));
        Stmt gotoStmt = Jimple.v().newIfStmt(leExpr, head);
        Stmt increaseCnt = Jimple.v().newAssignStmt(backCount, Jimple.v().newAddExpr(backCount, IntConstant.v(1)));

        units.insertAfter(gotoStmt, tail);
        units.insertAfter(increaseCnt, tail);
        /*
            original tail
             cnt++
             if(cnt<2)
                goto ...
         */

        if (fix && (tail instanceof GotoStmt || tail instanceof IfStmt)) {
            Local l1 = Jimple.v().newLocal("$bkrd" + info.newLocalCount++, RefType.v());
            Local randBoolean = Jimple.v().newLocal("bkbo" + info.newLocalCount++, BooleanType.v());
            body.getLocals().add(l1);
            body.getLocals().add(randBoolean);

            SootMethod nextBoolaen = randomObj.getMethodByName("nextBoolean");
            SootMethod init = randomObj.getMethod("void <init>()");

            AssignStmt newass = Jimple.v().newAssignStmt(l1, Jimple.v().newNewExpr(randomObj.getType()));
            InvokeStmt initInvoke = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(l1, init.makeRef()));

            AssignStmt nextBoolaenInvoke = Jimple.v().newAssignStmt(randBoolean, Jimple.v().newVirtualInvokeExpr(l1, nextBoolaen.makeRef()));
            units.insertBefore(newass, tail);
            units.insertBefore(initInvoke, tail);
            units.insertBefore(nextBoolaenInvoke, tail);

            IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(randBoolean, IntConstant.v(0)), increaseCnt);
            units.insertBefore(ifStmt, tail);
            units.insertAfter(Jimple.v().newGotoStmt(next), tail);
        }
        System.out.printf("*********************** add edge Jimple: block %d to block %d ****************************\n", src.getIndexInMethod(), dst.getIndexInMethod());
        return src;
    }

    public int searchParent(int node){
        if (node<2) {
            return 0;
        }
        ArrayList<Integer> tmp = new ArrayList<>();
        Block block = blocks.get(node);
        for (Block preBlk:block.getPreds()) {
            int ind = preBlk.getIndexInMethod();
            if (ind < block.getIndexInMethod()) {
//            if (ind != block.getIndexInMethod()) {
                tmp.add(ind);
            }
        }

        if (tmp.isEmpty() || tmp.contains(0)) {
            return 0;
        } else if (tmp.size()==1) {
            return tmp.get(0);
        } else {
            PriorityQueue<Integer> pre = new PriorityQueue<>((i1, i2) -> i2 - i1);
            pre.addAll(tmp);
            boolean end ;
            do {
                if (pre.isEmpty()||pre.contains(0)||pre.contains(1)){
                    return 0;
                }
                int parent = searchParent(pre.poll());
                pre.add(parent);

                Iterator<Integer> iterator = pre.iterator();
                int last = iterator.next();

                end = true;
                while (iterator.hasNext()) {
                    int next = iterator.next();
                    if (next != last) {
                        end = false;
                        break;
                    }
                }
            } while (!end);
            return pre.peek();
        }
    }

    public int searchRealDst(int src, int dst) {
        if (dst<2) {
            return 1;
        }
        int pre = dst;
        while (src != dst) {
            if (src == 0 || dst == 0) {
                return 1;
            }
            if (src < dst) {
                pre = dst;
                dst = searchParent(dst);
            } else {
                src = searchParent(src);
            }
        }
        return Math.max(pre, 1);
    }

    public Block addEdgeJimple(int srcInd, int dstInd) {
        if (srcInd < 0 || srcInd >= blocks.size() || dstInd < 0 || dstInd >= blocks.size()) {
            System.out.println("block does not exist");
            return null;
        }

        int RealdstInd = searchRealDst(srcInd, dstInd);
//        System.out.printf("\ndst: %d, real dst: %d\n", dstInd, RealdstInd);
        return addEdgeJimple(blocks.get(srcInd), blocks.get(RealdstInd));
    }

    public Block addEdgeJimpleNoCheck(int srcInd, int dstInd) {
        if (srcInd < 0 || srcInd >= blocks.size() || dstInd < 0 || dstInd >= blocks.size()) {
            System.out.println("block does not exist");
            return null;
        }
//        System.out.printf("\ndst: %d, real dst: %d\n", dstInd, dstInd);
        return addEdgeJimple(blocks.get(srcInd), blocks.get(dstInd));
    }

    public Unit getFirstNotIdentityStmtOfSbody(Body body){
        if (!(body instanceof ShimpleBody)){
            return null;
        }

        for (Unit u:body.getUnits()){
            if (!(u instanceof JIdentityStmt)){
                return u;
            }
        }
        return null;
    }


    public void choose(blockChooser chooser) {
        if (chooser == null) {
            System.out.println("need a chooser");
            return;
        }

        this.blockChooser = chooser;
        blocksToTrans = chooser.choose((ArrayList<Block>) blocks);
    }

    @Override
    public boolean transform(Body body, Unit u) {
        return false;
    }

    @Override
    public boolean canApply(Unit u) {
        return false;
    }

    public boolean transform(Body body, Block block) {
        return false;
    }

    public boolean transformRec(int depth, int cfg) {
        if (depth < 1) {
            return false;
        }
        body = (JimpleBody) body.getMethod().retrieveActiveBody();
        setUp(body);
        choose();
        boolean tran = false;
        int c = (int) (Math.random() * 2);
        if (cfg > 0 && c == 1) {
            if (blocksToTrans.isEmpty()){
                return false;
            }
            useMutator(this);

            if (blocksToTrans.size() < 3){
                return false;
            }
            Block b = blocksToTrans.get((int) (Math.random() * blocksToTrans.size()));
            while (blocks.indexOf(b)<2){
                b = blocksToTrans.get((int) (Math.random() * blocksToTrans.size()));
            }

            if (transform(body, b)) {
                tran = true;
            }
            if (transformRec(depth - 1, cfg - 1)) {
                tran = true;
            }
        } else {
            if (localTransformers.isEmpty()||toTrans.isEmpty()) {
                return false;
            }
            Unit u = toTrans.get((int) (Math.random() * toTrans.size()));
            MyMutator l = chooseTransformer();//localTransformers.get((int) (Math.random() * localTransformers.size()));
            if (l!=null&&l.transform(body, u)) {
                tran = true;
            }
            if(transformRec(depth - 1, cfg)){
                tran = true;
            }
        }
        return tran;
    }
}
