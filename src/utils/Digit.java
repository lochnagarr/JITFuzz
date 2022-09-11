package utils;

public class Digit {
    public int valueInt = 0;
    public double valueDouble = 0;
    private int isDouble;

    public Digit(int isDouble, double value) {
        this.isDouble = isDouble;
        if (isDouble != 0)
            this.valueDouble = value;
        else
            this.valueInt = (int) value;
    }

    public boolean isDouble() {
        return isDouble != 0;
    }

    public int getInt() {
        return valueInt;
    }

    public double getDouble() {
        return valueDouble;
    }
}
