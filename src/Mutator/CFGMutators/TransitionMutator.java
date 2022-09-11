package Mutator.CFGMutators;

import Mutator.CFGShimpleMutator;
import soot.Body;
import soot.Unit;
import soot.toolkits.graph.Block;

import java.util.ArrayList;

public class TransitionMutator extends CFGShimpleMutator {
    private static final TransitionMutator instance = new TransitionMutator();
    public static TransitionMutator v() {
        return instance;
    }
    private TransitionMutator() {}

    public static boolean fix = true;
    public boolean transform(Body body, Block block) {
        useMutator(this);
        int src = blocks.indexOf(block);
        int dest;
        if (fix) {
            dest = utils.util.randomChoice(src);
            while (dest == 0){
                dest = utils.util.randomChoice(src);
            }
            return addEdgeJimple(src, dest) != null;
        } else {
            dest = utils.util.randomChoice(blocks.size());
            return addEdgeJimpleNoCheck(src, dest) != null;
        }
    }

    @Override
    public boolean transform(Body body) {
        if (chooser == null) {
            System.out.println("need a chooser");
            return false;
        }
        setUp(body);
        choose();
        return transform(body, (Unit) null);
    }

    @Override
    public boolean transform(Body body, Unit u) {
        setUp(body);
        choose();
        if (blocksToTrans.isEmpty()){
            return false;
        }
        Block b;

        if (fix) {
            if (blocksToTrans.size() < 3){
                return false;
            }
            b = blocksToTrans.get((int) (Math.random() * blocksToTrans.size()));
            while (blocks.indexOf(b)<2){
                b = blocksToTrans.get((int) (Math.random() * blocksToTrans.size()));
            }
        } else {
            b = blocksToTrans.get((int) (Math.random() * blocksToTrans.size()));
        }
        return (transform(body, b));
    }


    public boolean canApply(Unit unit){
        return true;
    }

    public void choose() {
        blocksToTrans = new ArrayList<>(blocks);
    }
}
