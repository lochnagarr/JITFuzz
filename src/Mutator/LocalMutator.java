package Mutator;

import soot.Body;
import soot.Unit;

public abstract class LocalMutator extends MyMutator {
//    public stmtChooser chooser;

    @Override
    public abstract boolean transform(Body body, Unit u);

    public abstract boolean canApply(Unit u);

    public void choose() {
        toTrans = chooser.choose(units, cannot.get(this));
    }

    @Override
    public boolean transform(Body body) {
        setUp(body);
        choose();
        if (toTrans.isEmpty()) {
//            System.out.println("toTrans is empty!");
            return false;
        }
        body = body.getMethod().retrieveActiveBody();
        Unit u = toTrans.get((int) (Math.random() * toTrans.size()));
        return  transform(body, u);
    }
}
