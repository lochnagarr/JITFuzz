package chooser;

import soot.toolkits.graph.Block;

import java.util.ArrayList;

public interface blockChooser extends chooser {
    ArrayList<Block> choose(ArrayList<Block> blocks);
}
