package utils;

import java.util.*;

public class Bench {
    public static String dependency = "antlr-3.1.3.jar:asm-3.1.jar:avalon-framework-4.2.0.jar:batik-all.jar:commons-io-1.3.1.jar:constantine.jar:guava-r07.jar:jaffl.jar:janino-2.5.15.jar:jline-0.9.95-SNAPSHOT.jar:jnr-posix.jar:ls.txt:xmlgraphics-commons-1.3.1.jar\n";
    public static List<String> dependencies = new ArrayList<>();
    public static Map<String, String> mutateClass = new HashMap<>();
    public static String dic;
    static {
        String[] split = dependency.split(":");
        Collections.addAll(dependencies, split);

        mutateClass.put("sunflow", "org.sunflow.Benchmark");
    }

    public static String benchCp() {
        StringBuilder res = new StringBuilder();
        for(String s:dependencies) {
            res.append(dic).append(s).append(":");
        }
        return res.toString();
    }
}
