package chooser;

import soot.Unit;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashSet;

public interface stmtChooser extends chooser {
    ArrayList<Unit> choose(Chain<Unit> units, HashSet<String> cannot);
    void remove(Unit u);
}
