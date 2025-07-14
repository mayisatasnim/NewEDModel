public abstract class Utils {
    public static double getNormal(double mean, double standardDeviation) {
        return mean + standardDeviation * new java.util.Random().nextGaussian();
    }
    public static double getExp(double rate){
        if(rate == 0) return Integer.MAX_VALUE;
        double u = Math.random();
        return (-1.0/rate)*Math.log(1-u);
    }
}
