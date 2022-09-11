package chooser;

import soot.Unit;
import soot.util.Chain;

import java.util.*;

public class chooserByStr implements stmtChooser {
    public Set<String> strs;

    public chooserByStr() {
        this.strs = new HashSet<>();
    }

    public chooserByStr(String test){
        this.strs = new HashSet<>();
        this.strs.add(test);
    }

    public void addRule(String str){
        this.strs.add(str);
    }

    public void addRule(Collection<String> str){
        this.strs.addAll(str);
    }

    @Override
    public ArrayList<Unit> choose(Chain<Unit> units, HashSet<String> cannot) {
        ArrayList<Unit> res = new ArrayList<>();
        for (Unit u : units) {
            String s = u.toString();
            if (this.strs.contains(s) && (cannot==null || !cannot.contains(s))) {
                res.add(u);
            }
        }
        return res;
    }

    @Override
    public void remove(Unit u) {
        strs.remove(u.toString());
    }
}
