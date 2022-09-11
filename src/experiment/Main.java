package experiment;

import org.apache.commons.cli.*;
import utils.MutatorChooser;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {
    public static ArrayList<String> disable = new ArrayList<>();
    static {
        disable.add("scalar");
        disable.add("escape");
        disable.add("simp");
        disable.add("inline");
        disable.add("wrap");
        disable.add("trans");
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", "index", true, "The index of this experiment, to mark the log and seeds files.");
        options.addOption("I", "iterations", true, "Total iterations of mutation.");
        options.addOption("p","project", true, "The project that serves seed files ./resources/project.properties will be read.");
        options.addOption("s", "seeds", false, "The path to store seeds generated.");
        options.addOption("l", "log", false, "The path to store log files.");
        options.addOption("e", "exclude", false, "The mutator to be disabled: [scalar|escape|simp|inline|wrap|trans]\nNo one will be disabled in default.");

        CommandLineParser parser = new DefaultParser();
        String ind = "0", seed = "seeds", log = "./logs", prop = "./resources";
        int ep = 0, exclude = -1;

        try {
            CommandLine cmd = parser.parse(options, args);
            ind = cmd.getOptionValue("i");
            String e = cmd.getOptionValue("I");
            String p = cmd.getOptionValue("p");
            String msg = "Need options:";
            if (ind == null) { msg += " \n-i"; }
            if (e == null) { msg += " \n-I"; }
            if (p == null) { msg += " \n-p"; }
            if (!msg.equals("Need options:")) { throw new ParseException(msg); }
            ep = Integer.parseInt(e);
            if (cmd.hasOption("s")) {
                seed = cmd.getOptionValue("s");
            }
            if (cmd.hasOption("l")) {
                log = cmd.getOptionValue("l");
            }
            if (cmd.hasOption("p")) {
                prop = cmd.getOptionValue("p");
            }
            if (cmd.hasOption("e")) {
                exclude = disable.indexOf(cmd.getOptionValue("e"));
            }
        } catch (ParseException exp) {
            System.err.println(exp.getMessage());
            return;
        }

        String mutatorChoose = "bandit";
        System.out.println(ind);

        long l = System.currentTimeMillis();
        String tmp = "./tmp/tmp-"+l;

        new File(tmp).mkdirs();
        File logPath = new File(log);
        File seedPath = new File(seed);
        if (!logPath.exists()) { logPath.mkdirs(); }
        if (!seedPath.exists()) { seedPath.mkdirs(); }


        String tag = prop + "_" + mutatorChoose + "_" + exclude + "_" + ind;
        try (PrintStream printStream = new PrintStream(log + "/" + tag + ".log")) {
            System.setOut(printStream);
            MutatorChooser.only = false;
            framework framework = new framework();
            experiment.framework.exclude = exclude;
            experiment.framework.mutatorChoose = mutatorChoose;
            experiment.framework.vm = "hotspot";
            experiment.framework.debug = false;
            experiment.framework.tag = tag;
            experiment.framework.tmp = tmp;
            if (mutatorChoose.toLowerCase().contains("bandit")) {
                framework.mix = true;
            }
            framework.fuzz(ep, seed, prop);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            File destination = new File(tmp);
            if (destination.exists()) {
                destination.delete();
            }
        }
    }
}
