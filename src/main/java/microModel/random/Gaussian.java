package microModel.random;

/**
 * Gaussian Distribution.
 */
public class Gaussian implements ProbabilityDistribution<Double> {

    /**
     * Mean value of the Distribution
     */
    private double mean;
    /**
     * Standard deviation of the Distribution
     */
    private double std;

    /**
     * Sets the mean value for the Distribution
     * @param mean The mean value
     * @return return the Distribution instance so it can be chained for example <p>Gaussian.mean( .. value ..).rand()</p>
     */
    public Gaussian mean(double mean) {
        this.mean = mean;
        return this;
    }

    /**
     * Sets the standard deviation value for the Distribution
     * @param std The standard deviation value
     * @return return the Distribution instance so it can be chained for example <p>Gaussian.std( .. value ..).rand()</p>
     */
    public Gaussian std(double std) {
        this.std = std;
        return this;
    }

    @Override
    public Double rand() {
        return RANDOM.nextGaussian() * std + mean;
    }

    @Override
    public Double rand(Double lowerBound, Double upperBound) {
        //Just ignore the bound. The bound is meant for generating random integers.
        return rand();
    }
}
