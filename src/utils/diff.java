package utils;

public class diff {
    public boolean diff = false;
    public boolean ok = false;
    public boolean error = false;
    public boolean failure = false;
    public boolean newFound = false;

    public double time = 60;

    public int abandon = 0;


    public void setTime(double time) {
        this.time = Math.min(time, this.time);
    }

    public diff(boolean diff, boolean ok, boolean newFound) {
        this.diff = diff;
        this.ok = ok;
        this. newFound = newFound;
    }

    public diff() {}
}
