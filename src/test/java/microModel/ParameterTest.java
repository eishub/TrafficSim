package microModel;


import microModel.core.Parameter;
import org.junit.Assert;
import org.junit.Test;

public class ParameterTest {

    @Test
    public void testEqualsMethodParametersOfSameGenericType() {

        Parameter<Double> pd1 = new Parameter<Double>("name", 1.0);
        Parameter<Double> pd2 = new Parameter<Double>("name", 2.0);

        Assert.assertTrue(pd1.equals(pd2));

        pd1 = new Parameter<Double>("name1", 1.0);
        pd2 = new Parameter<Double>("name2", 2.0);

        Assert.assertFalse(pd1.equals(pd2));
    }

    @Test
    public void testEqualsMethodParametersOfDifferentGenericType() {
        Parameter<Double> pd = new Parameter<Double>("name", 1.0);
        Parameter<Integer> pi = new Parameter<Integer>("name",1);

        Assert.assertFalse(pd.equals(pi));
    }

    @Test
    public void testJavaEqualsOverrideContract() {
    /*
        The Equals contract

        1) Reflexive : Object must be equal to itself.
        2) Symmetric : if a.equals(b) is true then b.equals(a) must be true.
        3) Transitive : if a.equals(b) is true and b.equals(c) is true then c.equals(a) must be true.
        4) Consistent : multiple invocation of equals() method must result same value until any of properties are modified.
        5) Null comparison : comparing any object to null must be false and should not result in NullPointerException.
    */
        //1 Reflexive
        Parameter<Double> pd = new Parameter<Double>("name", 1.0);
        Assert.assertTrue(pd.equals(pd));


        //2 Symmetric
        Parameter<Double> pd1 = new Parameter<Double>("name", 1.0);
        Parameter<Double> pd2 = new Parameter<Double>("name", 2.0);
        Assert.assertTrue(pd1.equals(pd2));
        Assert.assertTrue(pd2.equals(pd1));

        //3 Transitive
        pd1 = new Parameter<Double>("name", 1.0);
        pd2 = new Parameter<Double>("name", 2.0);
        Parameter<Double> pd3 = new Parameter<Double>("name", 3.0);
        Assert.assertTrue(pd1.equals(pd2));
        Assert.assertTrue(pd2.equals(pd3));
        Assert.assertTrue(pd1.equals(pd3));

        //4 Consitent
        Assert.assertTrue(pd1.equals(pd2));
        Assert.assertTrue(pd1.equals(pd2));
        Assert.assertTrue(pd1.equals(pd2));

        //5 Null check
        Assert.assertFalse(pd1.equals(null));

    }

    @Test
    public void testJavaHashCodeOverrideMethod() {
    /*
        1) If two objects are equal by equals() method then there hashcode must be same.
        2) If two objects are not equal by equals() method then there hashcode could be same or different.
    */
        Parameter<Double> pd1 = new Parameter<Double>("name", 1.0);
        Parameter<Double> pd2 = new Parameter<Double>("name", 2.0);
        Assert.assertTrue(pd1.equals(pd2));
        Assert.assertEquals("Hash codes of equal objects must be the same", pd1.hashCode(), pd2.hashCode());
    }

}
