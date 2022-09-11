package Mutator;

import soot.Body;
import soot.Unit;
import soot.jimple.JimpleBody;
import utils.MutatorChooser;

import java.util.ArrayList;

public abstract class CFGMutator extends MyMutator {
    public static int exclude = -1;
    public static String mutatorChoose;
    public ArrayList<MyMutator> localTransformers;
    public int depth = 2;
    public int cfgTimes = 1;//1;

    public void choose() {
        toTrans = chooser.choose(units, cannot.get(this));
    }



    @Override
    public boolean transform(Body body) {
        if (chooser == null) {
            System.out.println("need a chooser");
            return false;
        }
        setUp(body);
        choose();
//        return transformRec(depth, cfgTimes);
        return transformSplit();
    }

    public void setLocalTransformers(ArrayList<MyMutator> localTransformers) {
        this.localTransformers = localTransformers;
    }

    @Override
    public abstract boolean transform(Body body, Unit u);

    public boolean transformRec(int depth, int cfg) {
        if (depth < 1||toTrans.isEmpty()) {
            return false;
        }

        body = (JimpleBody) body.getMethod().retrieveActiveBody();
        boolean tran = false;
        Unit u = toTrans.get((int) (Math.random() * toTrans.size()));
        int c = (int) (Math.random() * 2);

//        if (transform(body, u)){
//            tran = true;
//        }
//        transformRec(depth-1, cfg-1);

        if (cfg > 0 && c == 1) {
            if (transform(body, u)){
                tran = true;
            }
            tran |= transformRec(depth-1, cfg-1);
        } else {
            if (localTransformers.isEmpty()) {
                return false;
            }
            MyMutator l = chooseTransformer();//localTransformers.get((int) (Math.random() * localTransformers.size()));
            if(l!=null&&l.transform(body, u)){
                tran = true;
            }
            tran |= transformRec(depth-1, cfg);
        }
        chooser.remove(u);
        return tran;
    }

    public boolean transformSplit() {
        if (toTrans == null||toTrans.isEmpty()) {
            return false;
        }
        body = (JimpleBody) body.getMethod().retrieveActiveBody();
        Unit u = toTrans.get((int) (Math.random() * toTrans.size()));
//        chooser.remove(u);
        return transform(body, u);
    }

    public MyMutator chooseTransformer() {
        int m = -1;
        if (mutatorChoose.toLowerCase().contains("random")) {
            m = MutatorChooser.chooseLocalRandom();
        } else if (mutatorChoose.toLowerCase().contains("distribut")) {
            m = MutatorChooser.chooseLocalByDistribution();
        } else if (mutatorChoose.toLowerCase().contains("bandit")) {
            m = MutatorChooser.chooseBandit(mutatorLocalBandit);
        }
        return localTransformers.get(m);
    }

}