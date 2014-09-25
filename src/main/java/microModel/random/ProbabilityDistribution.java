package microModel.random;

import java.util.Random;

public interface ProbabilityDistribution <T> {

    /**
     * Generator for random numbers. This generator is connected to the seed and
     * should be used for all operations that are random and should be
     * repeatable. Before the generator can be used, the seed needs to be set
     * using <tt>setSeed</tt>.
     */
    public static Random RANDOM = new Random();

    /**
     * Returns a distributed random number.
     *
     * @return Distributed random number.
     */
    public T rand();

    /**
     * Returns a distributed random number with the given bounds.
     * This method is meant for Random Integer generators. Other
     * types can simply return the same result as {@link #rand()}
     * and ignore the given bounds.
     * @param upperBound
     * @return
     */
    public T rand(T lowerBound, T upperBound);
}