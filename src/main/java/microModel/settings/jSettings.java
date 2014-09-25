package microModel.settings;

import microModel.core.Parameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple class to store setting of several primitive types and objects. The
 * settings are set and retrieved with the put/get methods per data type.
 */
public class jSettings {

    /** Singleton instance. */
    private static jSettings INSTANCE = new jSettings();

    /**
     * Place holder for storing non default options for the available modelParameters or new ones.
     */
    private Map<Parameter<?>, Object> settings = new HashMap<Parameter<?>, Object>();

    public static jSettings getInstance() {
        return INSTANCE;
    }

    private jSettings() {}

    /**
     * Returns the value of the sough after parameter. This can either be a default value or a custom value
     * which has to have been given to the parameter by using the {@link jSettings#put(Parameter, Object)}}
     * method.
     *
     * @param p   The parameter to look for.
     * @param <T> The type of value the parameter has (i.e. an Integer, Boolean, String, ... parameter)
     * @return The value of the parameter sought after.
     */
    public <T> T get( Parameter<T> p) {
        if (p == null)
            return null;

        if (settings.containsKey(p))
            return (T) settings.get(p);

        return p.value();
    }

    /**
     * Returns the value of the sough after parameter. (see {@link jSettings#get(Parameter)})
     * TODO: what happens if the there a parameter with the name exists it is not of the type given.
     *
     * @param name The name of the parameter.
     * @param type The Class type of the value the parameter has.
     * @param <T>  The type of the value
     * @return The value of the parameter
     */
    public <T> T get(String name, Class<T> type) {
        Parameter<T> result = null;
        for (Parameter<?> p : settings.keySet()) {
            if (p.name().toLowerCase().compareTo(name.toLowerCase()) == 0) {
                result = (Parameter<T>) p;
            }
        }
        return get(result);
    }

    /**
     * Sets the value of a settings parameter to the given value.
     *
     * @param p     The parameter
     * @param value The value for the Parameter
     * @param <T>   The type of value the parameter represents.
     */
    public <T> void put(Parameter<T> p, T value) {
        settings.put(p, value);
    }

    /**
     * Removes a custom parameter and its value. If the parameter is a built in parameter it
     * resets the value of that parameter to its default value.
     *
     * @param p
     * @param <T>
     * @return
     */
    public <T> T remove(Parameter<T> p) {
        return (T) settings.remove(p);
    }

}