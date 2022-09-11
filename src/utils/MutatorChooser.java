package utils;

import java.util.HashSet;
import java.util.Set;

public class MutatorChooser {
    public static int exclude;
    public static Set<Integer> excludes = new HashSet<>();
    public static int excludeTime=0;
    public static int seed = -1;
    public static boolean only = false;

    public static int chooseCFGByDistribution() {
        while (true) {
            double t = Math.random();
            if (t<0.22) {
                if (exclude == 3) {
                    continue;
                }
                return 0;
            } else if (t<0.44) {
                if (exclude == 4) {
                    continue;
                }
                return 1;
            } else if (t<0.66) {
                if (exclude == 5) {
                    continue;
                }
                return 2;
            } else {
                if (exclude == 6) {
                    continue;
                }
                return 3;
            }
        }
    }

    public static int chooseCFGRandom() {
        int ex = exclude - 3;
        int res = (int)(Math.random()*3);
        while (res==ex) {
            res = (int)(Math.random()*3);
        }
        return res;
    }

    public static int chooseLocalByDistribution() {
        while (true) {
            double random = Math.random();
            if (random<0.2) {
                if (exclude == 0) {
                    continue;
                }
                return 0;
            }else if (random<0.6) {
                if (exclude == 1) {
                    continue;
                }
                return 1;
            } else {
                if (exclude == 2) {
                    continue;
                }
                return 2;
            }
        }
    }

    public static int chooseLocalRandom() {
        int res = (int)(Math.random()*3);
        while (res==exclude) {
            res = (int)(Math.random()*3);
        }
        return res;
    }

    public static int chooseBandit(Bandit bandit) {
        return bandit.select();
    }

    public static int chooseAllAvg(Info info) {
        int res = -1;
        int min = Integer.MAX_VALUE;
        int[] mutatorsUsed = info.mutatorsUsed;
        for (int i=0;i<7;i++) {
            if (mutatorsUsed[i]<min && exclude!=i) {
                min = mutatorsUsed[i];
                res = i;
            }
        }
        return res;
    }

    public static int choose(boolean mutatorChooseBandit, int exclude, Bandit bandit, int epoch) {
        if (only) {
            return exclude;
        }

        int type;
        do {
            if (mutatorChooseBandit) {
                type = bandit.select();
            } else {
                if (exclude > 3) {
                    type = util.randomChoice(2);
                } else {
                    type = util.randomChoice(3);
                }
            }
            if (exclude == 4 && type == 1) {
                type = 2;
            }
        } while (type > 3 && type == exclude - 3);

        return chooseByTypeAndExclude(exclude, type);
    }

    private static int chooseByTypeAndExclude(int exclude, int type) {
        if (type == 0) {
            if (exclude > 3 || exclude < 0) {
                return util.randomChoice(4);
            } else {
                int res = util.randomChoice(3);
                return res>=exclude?res+1:res;
            }
        } else {
            return type + 3;
        }
    }

    public static int choose_(boolean mutatorChooseBandit, int exclude, Bandit bandit, int epoch) {
        if (only) {
            return exclude;
        }

        if (mutatorChooseBandit) {
            int type = bandit.select();
            if (exclude == 4 && type == 1) {
                type = 2;
            }
            return chooseByTypeAndExclude(exclude, type);
        }else {
            if (exclude<0) {
                return util.randomChoice(6);
            } else {
                int res = util.randomChoice(5);
                return res>=exclude?res+1:res;
            }
        }
    }
}
