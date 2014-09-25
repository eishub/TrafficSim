package microModel.util;

import com.google.common.collect.Range;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class TableDataTest {
    private Long[][] twoDimLongArray;

    @Before
    public void init() {
        twoDimLongArray = new Long[][] { {1L,2L},
                                         {2L,3L},
                                         {3L,4L},
                                         {4L,5L},
                                         {5L,6L} };

    }


    @Test
    public void sizeTest() {
        TableData<Long> data = new TableData<Long>(twoDimLongArray);
        Assert.assertEquals(5, data.rowSize());
        Assert.assertEquals(2, data.columnSize());
    }

    @Test
    public void containTest() {
        TableData<Long> data = new TableData<Long>(twoDimLongArray);

        Assert.assertTrue(data.columnContains(0, 1L));
        Assert.assertTrue(data.columnContains(0, 2L));
        Assert.assertTrue(data.columnContains(0, 3L));
        Assert.assertTrue(data.columnContains(0, 4L));
        Assert.assertTrue(data.columnContains(0, 5L));
        Assert.assertFalse(data.columnContains(0, 6L));

        Assert.assertTrue(data.columnContains(1, 2L));
        Assert.assertTrue(data.columnContains(1, 3L));
        Assert.assertTrue(data.columnContains(1, 4L));
        Assert.assertTrue(data.columnContains(1, 5L));
        Assert.assertTrue(data.columnContains(1, 6L));
        Assert.assertFalse(data.columnContains(1, 1L));

        Assert.assertTrue(data.rowContains(0, 1L));
        Assert.assertTrue(data.rowContains(0, 2L));

        Assert.assertTrue(data.rowContains(1, 2L));
        Assert.assertTrue(data.rowContains(1, 3L));

        Assert.assertTrue(data.rowContains(2, 3L));
        Assert.assertTrue(data.rowContains(2, 4L));

        Assert.assertFalse(data.rowContains(0, 3L));
        Assert.assertFalse(data.rowContains(1, 6L));
    }


    @Test
    public void filterTest() {
        //Two Dimensional long table
        TableData<Long> data = new TableData<Long>(twoDimLongArray);

        //Filter first column
        TableData result = data.filter(0, Range.closed(2L, 4L));
        Assert.assertFalse(result.columnContains(0, 1L));
        Assert.assertFalse(result.columnContains(0, 5L));
        Assert.assertFalse(result.columnContains(1, 2L));
        Assert.assertFalse(result.columnContains(1, 6L));
        Assert.assertEquals(3, result.rowSize());
        Assert.assertEquals(2, result.columnSize());

        //Filter Second Column
        result = data.filter(1, Range.closed(3L, 5L));
        Assert.assertFalse(result.columnContains(0, 1L));
        Assert.assertFalse(result.columnContains(0, 5L));
        Assert.assertFalse(result.columnContains(1, 2L));
        Assert.assertFalse(result.columnContains(1, 6L));
        Assert.assertEquals(3, result.rowSize());
        Assert.assertEquals(2, result.columnSize());
    }

    @Test
    public void addRowTest() {
        //Two Dimensional double table
        TableData<Long> data = new TableData<Long>(twoDimLongArray);
        int rows = data.rowSize();
        data.addRow(new Long[] {6L,7L});
        Assert.assertEquals(rows + 1, data.rowSize());

        TableData result = data.filter(0, Range.singleton(6L));
        Assert.assertTrue(result.rowContains(0,6L));
        Assert.assertTrue(result.rowContains(0,7L));
        Assert.assertFalse(result.rowContains(0, 1L));
        Assert.assertFalse(result.rowContains(0, 2L));
        Assert.assertFalse(result.rowContains(0, 3L));
        Assert.assertFalse(result.rowContains(0, 4L));
        Assert.assertFalse(result.rowContains(0, 5L));
    }

    @Test
    public void addColumnTest() {
        //Two Dimensional double table
        TableData<Long> data = new TableData<Long>(twoDimLongArray);
        int columns = data.columnSize();
        data.addColumn(new Long[]{3L, 4L, 5L, 6L, 7L});
        Assert.assertEquals(columns + 1, data.columnSize());

        TableData result = data.filter(0, Range.singleton(5L));
        Assert.assertTrue(result.rowContains(0,6L));
        Assert.assertTrue(result.rowContains(0,7L));
        Assert.assertFalse(result.rowContains(0, 1L));
        Assert.assertFalse(result.rowContains(0, 2L));
        Assert.assertFalse(result.rowContains(0, 3L));
        Assert.assertFalse(result.rowContains(0, 4L));
    }

}
