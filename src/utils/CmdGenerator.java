package utils;

import experiment.liveStmtCollector;

import java.io.StringBufferInputStream;
import java.util.*;

public class CmdGenerator {
    public static Properties p;
    public static String Xcomp = " -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -Xcomp -XX:CompileOnly=";
    public static String j9Xcomp = " -Xjit:count=0,optlevel=scorching,limit={";
    public static String JUnit = " org.junit.runner.JUnitCore ";

    public static String fuzz;
    public static String cp;
    public static String map1;
    public static String javaBin;
    public static String j9Bin;
    public static String showmap;
    public static String unitPath;
    public static String mutateClass;
    public static String sourceDirectory;
    public static String module;

    public static String benchCp;
    public static String benchMainClass;
    public static List<String> dependencies = new ArrayList<>();
    public static Map<String, String> benchClass = new HashMap<>();
    public static String dependencyDir;

    public static String command;
    public static String tmp;

    public static String runCmd(Properties properties, String vm) {
        String res = null;
//        if (tool.contains("junit")) {
//            String unitPath = properties.getProperty("unitPath");
//            if (vm.contains("hotspot")) {
//                res = showmap + map1 + javaBin + Xcomp + cp + JUnit+unitPath;
//            } else if (vm.contains("j9")) {
//                res  =showmap + map1 + j9Bin+ j9Xcomp + cp + JUnit+unitPath;
//            } else {
//                res = javaBin+ Xcomp + cp + JUnit + unitPath;
//            }
//        }

        String cmd = p.getProperty("command");
        cmd = cmdAddTmp(cmd, tmp+"/tmp");
        res = showmap + map1 + " " + cmd;
        return res;
    }

    public static String liveMethodsCmd() {
//        String cpp = " -cp "+tmp+"/tmpLive:"+fuzz+"/junit.jar:"+fuzz+"/hamcrest.jar:"+fuzz+"/util:"+sourceDirectory;
//        if (tool.contains("junit")) {
//            cpp += " ";
//            res = "java" + cpp + "org.junit.runner.JUnitCore "+unitPath;
//        }

        String cmd = p.getProperty("command");
        cmd = removeOptions(cmd);
        cmd = cmdAddTmp(cmd, tmp+"/tmpLive");
        cmd = cmd2Common(cmd);
        return cmd;
    }

    public static void prepare(Properties properties) {
        p = properties;
        fuzz = properties.getProperty("fuzz");
        cp = " -cp "+tmp+"/tmp:"+fuzz+"/util:"+fuzz+"/junit.jar:"+fuzz+"/hamcrest.jar ";
        map1 = " "+tmp+"/map1.txt";
        javaBin = " "+properties.getProperty("javaBin");
        j9Bin = " "+properties.getProperty("j9Bin");
        showmap = properties.getProperty("AFLplusplus")+"/afl-showmap -t 1000000 -o";
        unitPath = properties.getProperty("unitPath");
        mutateClass = properties.getProperty("mutateClass");
        String compileOnly = mutateClass.replace('.', '/');
        Xcomp += compileOnly;
        j9Xcomp += compileOnly+".*} ";
        sourceDirectory = properties.getProperty("sourceDirectory");
        cp = addCp(cp, sourceDirectory);
        dependencyDir = properties.getProperty("dependency");

        module = ":"+properties.getProperty("module")+":test --tests "+unitPath;

        benchCp = benchCp();
        benchMainClass = properties.getProperty("bench");

        benchClass.put("sunflow", "org.sunflow.Benchmark -bench");
    }

    public static String addCp(String cp, String c){
        cp = cp.substring(0, cp.length()-1);
        cp+= ":"+c+" ";
        return cp;
    }

    public static String benchCp() {
        StringBuilder res = new StringBuilder();
        for(String s:dependencies) {
            res.append(":").append(dependencyDir).append("/").append(s);
        }
        return res.toString();
    }

    public static String cmdAddTmp(String cmd, String tmp) {
        String[] s = cmd.split(" ");
        for (int i=0;i<s.length;i++) {
            if (s[i].contains("-cp")||s[i].contains("classpath")) {
                s[i+1] = tmp+":"+fuzz+"/util:"+s[i+1];
                break;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String ss: s) {
            stringBuilder.append(ss).append(" ");
        }
        return stringBuilder.toString();
    }

    public static String cmd2Common(String cmd) {
        String[] s = cmd.split(" ");
        for (int i=0;i<s.length;i++) {
            if (s[i].contains("build")&&s[i].contains("jdk/bin/java")) {
                s[i] = "java";
            }
            if (s[i].contains("-Xcomp") || s[i].contains("-XX:CompileOnly")) {
                s[i] = "";
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String ss: s) {
            stringBuilder.append(ss).append(" ");
        }
        return stringBuilder.toString();
    }

    public static String removeOptions(String cmd) {
        int jar = cmd.indexOf("-jar ");
        if (jar == -1) {
            jar = cmd.length();
        }
        int cp = cmd.indexOf("-cp ");
        if (cp == -1) {
            cp = cmd.length();
        }
        int classpath = cmd.indexOf("-cp ");
        if (classpath == -1) {
            classpath = cmd.length();
        }
        cp = Math.min(cp, classpath);

        return "java " + cmd.substring(Math.min(jar, cp));
    }

    public static void main(String[] args) {
        String cmd = "/data/jit/openj9-openjdk-jdk17/build/linux-x86_64-server-release/images/jdk/bin/java \"-Xjit:count=0,optlevel=scorching,limit={cn/hutool/core/text/PasswdStrength}\" -jar /data/jit/junit-platform-console-standalone-1.8.2.jar -cp /data/jit/benchmark/hutool/hutool-core/target/test-classes:/data/jit/benchmark/hutool/hutool-core/target/classes:/data/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar:/data/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/data/.m2/repository/org/projectlombok/lombok/1.18.24/lombok-1.18.24.jar -m cn.hutool.core.text.PasswdStrengthTest#strengthTest";
        System.out.println(removeOptions(cmd));
    }
}
