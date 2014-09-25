package microModel.core;

/**
 * Class representing a parameter with a default value. This is used for
 * built-in modelParameters and can also be used to create new custom modelParameters.
 * See the Builtin Settings modelParameters for example.
 * @param <T>
 */
public class Parameter<T> {
    private String name;
    private T value;

    public Parameter(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public T value() {
        return value;
    }

    private T type() {
        return (T) value.getClass();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ( !obj.getClass().isInstance(this) ) {
            return false;
        }

        Parameter<T> other = (Parameter<T>) obj;

        if (other.type() != type()) {
            return false;
        }

        return other.name.compareTo(name) == 0;
    }

    @Override
    public int hashCode() {
        return 31 + name.hashCode();
    }
}
