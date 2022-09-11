package utils;

import java.util.*;

public class Bandit {
    public int arm;
    public int totalTime;
    public HashMap<String, Integer> interpret = new HashMap<>();
    public int[] time;
    public int[] reward;
    public int[] variance;
    public double[] score;
    public int[] tmp;

    public Bandit(int arm, Set<String> arms) {
        if (arm < 0 && arms != null && !arms.isEmpty()) {
            arm = arms.size();
        }
        this.arm = arm;

        time = new int[arm];
        reward = new int[arm];
        variance = new int[arm];
        score = new double[arm];
        Arrays.fill(score, 1000.0);
        tmp = new int[arm];

        int i = 0;
        if (arms != null) {
            for (String s: arms) {
                interpret.put(s, i++);
            }
        }
    }

    public void clearTmp() {
        Arrays.fill(tmp, 0);
    }

    public double getVarianceUcb (int selectArm) {
        double ucb_component = Math.sqrt((2 * Math.log(totalTime)) / time[selectArm]);

        double mean = (double)reward[selectArm] / (time[selectArm] + 0.0);
        double var = (double)variance[selectArm] / (time[selectArm] + 0.0) - mean * mean;

        return var + ucb_component;
    }

    public double getVarianceUcb (String selectArm) {
        return getVarianceUcb(interpret.get(selectArm));
    }

    public void update(int gain, int selectArm) {
        reward[selectArm] += gain;
        variance[selectArm] += gain*gain;

        double tuneComponent = Math.min(0.25, getVarianceUcb(selectArm));
        score[selectArm] = Math.sqrt((tuneComponent *Math.log(totalTime)) / time[selectArm])
                + (double) reward[selectArm] / (time[selectArm] + 0.0);
        clearTmp();
    }

    public void update(int gain, String selectArm) {
        update(gain, interpret.get(selectArm));
    }

    public void updateTmp(int gain) {
        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i] > 0) {
                update(gain, i);
            }
        }
        clearTmp();
    }

    public void use(int i) {
        if (tmp[i]<1) {
            time[i]++;
            totalTime++;
        }
        tmp[i]++;
    }

    public void use(String i) {
        use(interpret.get(i));
    }

    public int select() {
        double max = Double.MIN_VALUE;
        int res = -1;
        for (int i = 0; i < score.length; i++) {
            System.out.print(score[i]+" ");
            if (i == score.length-1) {
                System.out.println();
            }
            if (score[i] > max) {
                res = i;
                max = score[i];
            }
        }
        use(res);
        return res;
    }

    public String selectString() {
        double max = -1;
        String res = "";
        for (Map.Entry<String, Integer> entry : interpret.entrySet()) {
            int i = entry.getValue();
            String s = entry.getKey();
            if (score[i] > max) {
                res = s;
                max = score[i];
            }
        }
        use(res);
        return res;
    }

    public int getArmUsed() {
        for (int i=0;i<tmp.length;i++){
            if (tmp[i]>0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();

        for(double d:score) {
            res.append(d).append(" ");
        }
        return res.toString();
    }
}
