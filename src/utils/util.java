package utils;

import soot.SootClass;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class util {
    public static HashMap<String, Integer> mutator2ind = new HashMap<>();
    public static String[] mutatorsAll = new String[]{"encapsulationTransformer", "escapeTransformer", "substitutionTransformer",
            "addBranchTransformer", "addLoopTransformer", "inlineTransformer", "backEdgeTransformer"
    };

    static {
        for (int i = 0; i < mutatorsAll.length; i++) {
            mutator2ind.put(mutatorsAll[i], i);
        }
    }

    public static int findCeil(int[] arr, int r, int l, int h)
    {
        int mid;
        while (l < h)
        {
            mid = l + ((h - l) >> 1); // Same as mid = (l+h)/2
            if(r > arr[mid])
                l = mid + 1;
            else
                h = mid;
        }
        return (arr[l] >= r) ? l : -1;
    }

    static int myRand(int[] arr, int[] freq, int n)
    {
        // Create and fill prefix array
        int[] prefix = new int[n];
        int i;
        prefix[0] = freq[0];
        for (i = 1; i < n; ++i)
            prefix[i] = prefix[i - 1] + freq[i];

        // prefix[n-1] is sum of all frequencies.
        // Generate a random number with
        // value from 1 to this sum
        int r = ((int)(Math.random()*(323567)) % prefix[n - 1]) + 1;

        // Find index of ceiling of r in prefix array
        int indexc = findCeil(prefix, r, 0, n - 1);
        return arr[indexc];
    }


    public static int randomChoice(int range) {
        return (int) (Math.random() * range);
    }

    public static int randomBetween(int low, int high) {
        return low + randomChoice(high-low);
    }

    public static String getPkgName(String className) {
        String[] split = className.split("\\.");
        StringBuilder fileName = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            fileName.append(split[i]).append("/");
        }
        return fileName.toString();
    }

    public static String getPkgNameWithClsName(String className) {
        return className.replaceAll("\\.", "/")+".class";
    }

    public static StringBuilder mkdir(String className, String path) {
        StringBuilder fileName = new StringBuilder(path.endsWith("/")?path:path+"/");
        fileName.append(getPkgName(className));
        File file = new File(fileName.toString());
        if(!(file.exists()&& file.isDirectory())) {
            file.mkdirs();
        }
        return fileName;
    }

    public static void move(String source, String dest) {
        File src = new File(source);
        File destination = new File(dest);
        if (destination.exists()) {
            destination.delete();
        }
        try {
            Files.copy(src.toPath(), destination.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void move(SootClass cls, String srcPkg, String destPkg) {
        mkdir(cls.getName(), destPkg);
        String pkgNameWithClsName = getPkgNameWithClsName(cls.getName());
        move((srcPkg.endsWith("/")?srcPkg:srcPkg+"/")+pkgNameWithClsName,
                (destPkg.endsWith("/")?destPkg:destPkg+"/")+pkgNameWithClsName);
    }

    public static boolean writeBack(SootClass cls, String pkg) {
        StringBuilder fileName = mkdir(cls.getName(), pkg);
        fileName.append(cls.getShortName()).append(".class");

        File f = new File(fileName.toString());
        if (f.exists()) {
            f.delete();
        }

        OutputStream streamOut;
        try {
            streamOut = new JasminOutputStream(new FileOutputStream(fileName.toString()));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            JasminClass jasminClass = new soot.jimple.JasminClass(cls);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void writeFile(String filename, boolean append, ArrayList<String> str) {
        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename, append));
            for (String s:str) {
                bufferedWriter.append(s).append("\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> readTextFile(String filename, String pattern) {
        ArrayList<String> res = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filename))) {
            String s;
            if (pattern==null) {
                while ((s = bufferedReader.readLine()) != null) {
                    res.add(s);
                }
            } else {
                while ((s = bufferedReader.readLine()) != null) {
                    if (Pattern.matches(pattern, s)) {
                        res.add(s);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static ArrayList<String> exec(String cmd) {
        ArrayList<String> res = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            final InputStream inputStream = p.getInputStream();
            final InputStream errorStream = p.getErrorStream();
            new Thread(() -> {
                BufferedReader br2 = null;
                try {
                    br2 = new BufferedReader(new InputStreamReader(errorStream, "gbk"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                try {
                    int cnt = 0;
                    String line2;
                    while ((line2 = br2.readLine()) != null) {
                        if (cnt<25) {
                            if (line2.toLowerCase().contains("java.lang.verifyerror")) {
                                System.out.println(line2);
                            }
                            cnt++;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br2.close();
                        errorStream.close();
                    } catch (IOException e) {
                        System.out.println("br2, errorStream error");
                        e.printStackTrace();
                    }
                }
            }).start();
            new Thread(()->{
                BufferedReader br1;
                try {
                    br1 = new BufferedReader(new InputStreamReader(inputStream, "gbk"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                try {
                    String line1;
                    while ((line1 = br1.readLine()) != null) {
                        res.add(line1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br1.close();
                        inputStream.close();
                    } catch (IOException e) {
                        System.out.println("br1, inputStream error");
                        e.printStackTrace();
                    }
                }
            }).start();

            p.waitFor(50, TimeUnit.SECONDS);
            if (p.isAlive()) {
                p.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static diff crash(ArrayList<String> commands, SootClass mainClass, String seedAll, int sds, int epochs) {
        diff rees = new diff();
        ArrayList<HashSet<String>> all = new ArrayList<>();
        ArrayList<String> toRemove = new ArrayList<>();
        int min = Integer.MAX_VALUE;
        for (String cmd : commands) {
            HashSet<String> exec = new HashSet<>(exec(cmd));
            if (exec.size()>2) {
                min = Math.min(min, exec.size());
                all.add(exec);
            } else {
                toRemove.add(cmd);
            }
        }
        commands.removeAll(toRemove);

        if (all.isEmpty()) {
            return rees;
        }

        int diff = all.get(0).size();
        HashSet<String> tmp = new HashSet<>(all.get(0));
        for (int i=1;i<all.size();i++) {
            tmp.retainAll(all.get(i));
        }
        if (all.size()>1) {
            diff -= tmp.size();
        }

        if (diff>=min/2) {
            writeBack(mainClass, seedAll+"/seeds"+sds+"/found/epoch"+epochs);
            ArrayList<String> res = new ArrayList<>();
            res.add("\n\n================== epoch: "+epochs+" =================\n\n");
            for (int i=0;i<all.size();i++) {
                res.add("\n"+commands.get(i)+"\n");
                res.addAll(all.get(i));
            }
            writeFile(seedAll+"/seeds"+sds+"/found/found.txt", true, res);
            rees.diff = true;
        }

        for(String s:tmp) {
            if (s.contains("OK")) {
                rees.ok = true;
                break;
            }
        }
        return rees;
    }

    private static void printMessage(String cmd) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec(cmd);
        InputStream input = process.getInputStream();
        InputStream error = process.getErrorStream();
        new Thread(() -> {
            Reader reader = new InputStreamReader(input);
            BufferedReader bf = new BufferedReader(reader);
            String line = null;
            try {
                while((line=bf.readLine())!=null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        process.waitFor();
    }
}
