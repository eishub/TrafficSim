package microModel.random;

/**
 * Exponential Distribution.
 */
public class Exponential implements ProbabilityDistribution<Double> {

    /**
     * Mean value of the Distribution
     */
    private double mean;

    /**
     * Sets the mean value for the Distribution
     * @param mean The mean value
     * @return return the Distribution instance so it can be chained for example <p>Exponential.mean( .. value ..).rand()</p>
     */
    public Exponential mean(double mean) {
        this.mean = mean;
        return this;
    }

    @Override
    public Double rand() {
        // note: r = -log(uniform)/gamma & mean = 1/gamma
        return -Math.log(RANDOM.nextDouble()) * mean;

    }

    @Override
    public Double rand(Double lowerBound, Double upperBound) {
        //Just ignore the bound. The bound is meant for generating random integers.
        return rand();
    }
}
