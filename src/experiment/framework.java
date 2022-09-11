package experiment;

import Mutator.CFGMutator;
import Mutator.CFGMutators.*;
import Mutator.LocalMutators.ScalarMutator;
import Mutator.LocalMutators.EscapeMutator;
import Mutator.LocalMutators.SimplificationMutator;
import Mutator.MyMutator;
import chooser.chooserByStr;
import soot.*;
import soot.options.Options;
import utils.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class framework {
    public static boolean debug = true;
    public static int exclude = -1;
    public static String mutatorChoose;
    public static String vm;
    public static String runCmd;
    public static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String java = "java ";
    public static String signal = "a fatal error has been";
    public static HashSet<String> sigs = new HashSet<>();
    public static String map1;
    public static String separator = "!";
    public static HashSet<String> liveStmts = new HashSet<>();
    public static HashSet<String> liveMethods;

    public static String lastSeedPath = "";
    public static String mutateClass;
    public static String mutateMethod;
    public static Set<Integer> reached = new HashSet<>();
    public static ArrayList<Info> seeds = new ArrayList<>();

    public static String seedAll = "seedAll";
    public static String tag;
    public static String tmp;
//    public int sds = 0;
    public int epochs = 1;
    public String sourceDirectory;
    public SootClass mainClass;
    public SootMethod sm;
    public Body body;
    public liveStmtCollector liveStmtCollector;
    public SootClass digit;
    public SootClass randomObj;
    public ArrayList<MyMutator> localMutators = new ArrayList<>();
    public ArrayList<MyMutator> allMutators = new ArrayList<>();

    public Random random = new Random();
    public boolean randChoose = false;
    public int live = 0;
    public boolean mix = false;

    public static Bandit stackSizeBandit = new Bandit(4, null);
    public static Bandit[] mutatorBandits = new Bandit[4];

    public static boolean first = true;
    public static String prop;

    public static int ind = 0;
    public static boolean mutatorChooseBandit;

    public static int noLiveSeed = 0;
    public static int liveSeed = 0;

    public void prepare(String url, String path){
        System.out.println("exclude: "+exclude);
        for (int i=0;i< mutatorBandits.length;i++) {
            if (exclude > 3) {
                mutatorBandits[i] = new Bandit(2, null);
            } else {
                mutatorBandits[i] = new Bandit(3, null);
            }
        }

        this.seedAll = path;
        Properties properties = PropertiesReader.readProperties(url);

        CmdGenerator.tmp = tmp;
        experiment.liveStmtCollector.tmp = tmp;
        CmdGenerator.prepare(properties);
        mutateClass = properties.getProperty("mutateClass");
        sourceDirectory = "./util:"+properties.getProperty("sourceDirectory");

        seeds.add(new Info("0"+separator+mutateClass+separator+mutateMethod, 0));

        liveStmtCollector = new liveStmtCollector();
        liveStmtCollector.setCmd(CmdGenerator.liveMethodsCmd());

        localMutators.add(ScalarMutator.v());
        localMutators.add(EscapeMutator.v());
        localMutators.add(SimplificationMutator.v());
        localMutators.add(InlineMutator.v());

        allMutators.addAll(localMutators);
        allMutators.add(WrapMutator.v());
        allMutators.add(TransitionMutator.v());

        CFGMutator.exclude = exclude;
        CFGMutator.mutatorChoose = mutatorChoose;
        MutatorChooser.exclude = exclude;

        runCmd = CmdGenerator.runCmd(properties, vm);
        map1 = " "+tmp+"/map1.txt";
    }

    public void fuzz(int eps, String path, String prop_) {
        mutatorChooseBandit = mutatorChoose.toLowerCase().contains("bandit");
        prop = prop_;
        prepare("resources/"+prop_+".properties", path);
        epochs = 1;
        reached.clear();
        try {
            setupSoot();
        } catch (Exception e) {
            System.out.println(epochs);
            System.out.println(Options.v().soot_classpath());
            e.printStackTrace();
            return;
        }

        liveMethods = new HashSet<>();
        liveMethods = liveStmtCollector.liveMethods(mainClass);
        System.out.println("live method count: "+liveMethods.size());
        liveStmtCollector.setLiveMethods(liveMethods);
        Info info = null;
        while (epochs<eps){
            if (seeds.isEmpty()) {
                seeds.add(new Info("0"+separator+mutateClass+separator+mutateMethod, 0));
            }
            if (mix) {
                if (live<11) {
                    mutatorChoose = "random";
                } else {
                    mutatorChoose = "bandit";
                }
            }

            String sd;
            Info last = info;
            if (live < 50) {
                info = seeds.get(seeds.size()-1);
            } else {
//                info = seeds.get(seeds.size()-1);
                info = seeds.get(ind);
            }
            info.father = last;
            sd = info.seed;
            info = new Info(info, epochs);
            String[] s = sd.split(separator);
            System.out.printf("\n======================== epoch: %d,  mutate from: %s =========================\n", epochs, s[0]);
            setSeed(s[0]);
            try {
                fuzzOne(s[1], s[2], info, sd);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(epochs+"\n"+s[0]+"\n"+s[1]);
                if (seeds.indexOf(info.father) > 0) {
                    seeds.remove(info.father);
                }
            } finally {
                epochs++;
                if (info.father.abandon > 2 && seeds.indexOf(info.father) > 0) {
                    seeds.remove(info.father);
                }
                ind = Math.min(ind+1, seeds.size()-1);
            }
        }
    }

    public boolean fuzzOne(String className, String signature, Info info, String lastSd) {
        clearTmp();
        setupSoot();

        if (info.mutateCnt.isEmpty()) {
            info.initMutateCnt(liveMethods);
        }

        String subSig = chooseMethodQuick();
        if (subSig == null||subSig.equals("error")||liveStmts.isEmpty()){
            System.out.println("no live codes, continue");
            seeds.remove(info.father);
            return false;
        }
//        setupSoot();

        sm = mainClass.getMethod(subSig);
        body = sm.retrieveActiveBody();

        boolean tran = false;
        chooserByStr chooserByStr = new chooserByStr();
        chooserByStr.addRule(liveStmts);
        MutatorChooser.seed = (int)(Math.random()*4);

        MyMutator transformer = null;

        System.out.println("Mutator choose: "+mutatorChoose);

        int maxStage;
        if (mutatorChooseBandit) {
            maxStage = stackSizeBandit.select();
        } else {
            maxStage = util.randomChoice(4);
        }
        transformer = allMutators.get(MutatorChooser.choose_(mutatorChooseBandit, exclude, mutatorBandits[maxStage], epochs));
        System.out.println("Mutator: "+transformer);

        for (int curStage = 0; curStage < (int) Math.pow(2, maxStage+2); curStage++) {
            MyMutator.setMainClass(mainClass);
            MyMutator.setDigit(digit);
            MyMutator.setRandomObj(randomObj);
            MyMutator.setInfo(info);
            transformer.setChooser(chooserByStr);

            try{
                tran |= transformer.transform(body);
            }catch (Exception e) {
                e.printStackTrace();
                System.out.println("error: epoch: "+epochs+"-"+ind);
                break;
            }
            info = MyMutator.getInfo();
        }

        if (!tran){
            System.out.println("No transform performed");
            if (live > 30) {
                updateBandits(maxStage, 0);
            }
            info.abandon();
            return false;
        }
        sm.setActiveBody(body);
        try {
            if (!util.writeBack(mainClass, tmp+"/tmp")) {
                info.abandon();
                System.out.println("write error: "+epochs+"-"+ind);
                noLiveSeed ++;
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            info.abandon();
            System.out.println("write error: "+epochs+"-"+ind);
            noLiveSeed ++;
            return false;
        }
        liveSeed ++;

        util.move(mainClass, tmp+"/tmp", seedAll+"/"+prop+"/"+tag+"/epoch"+epochs);
        diff run = run();
        info.run = run;

        int newEdge = newEdge(className, signature, info, run);
        info.update(run.time, newEdge, 10, true);
        newEdge = Math.min(1, newEdge);
        if(run.error) {
            info.abandon();
        } else {
            updateBandits(maxStage, newEdge);
        }
        live++;
        System.out.printf("live seeds: %d, no live seeds: %d\n", liveSeed, noLiveSeed);
        return true;
    }

    public void addSeed(String className, String signature, Info i) {
        i.seed = epochs+separator+className+separator+sm.getSubSignature();
        seeds.add(i);
    }

    public diff run() {
        diff res = new diff();
        System.out.println(df.format(new Date()));

        logDebug(runCmd);
        long begin = System.currentTimeMillis();
        ArrayList<String> exec1 = util.exec(runCmd);
        long end = System.currentTimeMillis();
        res.setTime((end - begin) / 1000.0);
        if (res.time > 45) {
            ind = 0;
        }

        String oracle;
        for (String s:exec1){
            logDebug(s);
//            System.out.println(s);
            s = s.toLowerCase();
            if (!res.ok && s.contains("ok (")) {
                res.ok = true;
                break;
            } else if (!res.failure && s.contains("failures")) {
                res.failure = true;
            } else if (s.contains("java.lang.verifyerror")) {
                res.error = true;
                noLiveSeed ++;
                break;
            } else if (s.contains("illegal target of jump or branch")) {
                res.error = true;
                ind = 0;
                break;
            } else if(s.contains("java.lang.classformaterror"))  {
                res.error = true;
                ind = 0;
                break;
            } else if (s.contains(signal)) {
                System.out.println("\ninteresting found!");
                res.newFound = true;
                res.failure = true;
                res.error = true;
            }
        }

        if (res.error || res.failure || res.newFound) {
            for (int i = 0; i < 20 && i < exec1.size(); i++) {
                System.out.println(exec1.get(i));
            }
        }

        return res;
    }

    public int newEdge(String className, String signature, Info info, diff run) {
        if (vm.toLowerCase().contains("noinstrument")) {
            addSeed(className, signature, info);
            randChoose = true;
            return 0;
        }
        Set<Integer> s2 = new HashSet<>();
        try {
            BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream(map1.substring(1))));
            String line;
            while ((line = br2.readLine()) != null) {
                s2.add(Integer.parseInt(line.split(":")[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!reached.isEmpty()) {
            first = false;
        }
        System.out.printf("\nedges collected:%d\n", s2.size());
        s2.removeAll(reached);
        System.out.printf("new edges found:%d\ntotal edges:%d\n", s2.size(), reached.size());
        if (s2.size()!=0) {
            reached.addAll(s2);
            if (!run.error&&(!run.failure||run.newFound)) {
                addSeed(className, signature, info);
                randChoose = false;
//                util.writeBack(mainClass, seedAll+"/seeds"+sds+"/epoch"+epochs);
            }
            return s2.size();
        }
        return 0;
    }

    public void setupSoot() {
        G.reset();
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(lastSeedPath+":"+sourceDirectory);
        Options.v().set_keep_line_number(true);

        SootClass sc = Scene.v().loadClassAndSupport(mutateClass);
        sc.setApplicationClass();

        digit = Scene.v().loadClassAndSupport("utils.Digit");
        randomObj = Scene.v().loadClassAndSupport("java.util.Random");

        Scene.v().loadNecessaryClasses();

        mainClass = Scene.v().getSootClass(mutateClass);

        for (SootMethod method : mainClass.getMethods()) {
            method.retrieveActiveBody();
        }
    }


    public void setSeed(String e){
        String p = seedAll+"/"+prop+"/"+tag+"/epoch"+e;
        File file = new File(p);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
        }
        lastSeedPath = p;
    }

    public String chooseMethodQuick() {
        liveStmts.clear();
        String res;

        res = (String) liveMethods.toArray()[util.randomChoice(liveMethods.size())];
        if (res!=null) {
            Body bd = mainClass.getMethod(res).retrieveActiveBody();
            for (Unit u:bd.getUnits()) {
                String s = u.toString();
                if (!s.contains("@this:") && !s.contains("return")&&!s.contains("@parameter")) {
                    liveStmts.add(u.toString());
                }
            }
        }
        return res;
    }

    public void updateBandits(int index, int reward) {
        if (first||index<0) {
            return;
        }
        stackSizeBandit.updateTmp(reward);
        mutatorBandits[index].updateTmp(reward);
        logBandit();
    }

    public void clearTmp() {
        stackSizeBandit.clearTmp();
        for (Bandit bandit:mutatorBandits) {
            bandit.clearTmp();
        }
    }

    public void logDebug(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

    public void logBandit() {
        if (debug) {
            System.out.println(stackSizeBandit);
            for (Bandit b:mutatorBandits) {
                System.out.println(b);
            }
        }
    }

}

