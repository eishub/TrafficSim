package microModel.util;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TableData<T extends Comparable<T>> {

    private Table<Integer, Integer, T> data;
    private int rowSize = 0;
    private int columnSize = 0;

    public TableData() {
        data = HashBasedTable.create();
    }

    private TableData(Table<Integer, Integer, T> table) {
        data = HashBasedTable.create(table);
        rowSize = data.rowMap().size();
        columnSize = data.columnMap().size();
    }

    public TableData(T[][] twoDimData) {
        data = HashBasedTable.create();
        for (int r = 0; r < twoDimData.length; r++) {
            T[] row = twoDimData[r];
            for (int c = 0; c < row.length; c++) {
                T value = row[c];
                data.put(r, c, value);
            }
        }
        rowSize = twoDimData.length;
        columnSize = twoDimData[0].length;
    }

    public static <T1 extends Comparable<T1>> T1[][] asArray(TableData<T1> table, T1[][] array) {
        int r = table.rowSize();
        int c = table.columnSize();

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                array[i][j] = table.get(i, j);
            }
        }

        return array;
    }

    public static List<Long> add(List<Long> column1, List<Long> column2) {
        if (column1.size() != column2.size()) {
            return null;

        }
        List<Long> result = new ArrayList<Long>();
        for (int i = 0; i < column1.size(); i++) {
            result.add(column1.get(i) + column2.get(i));
        }
        return result;
    }

    public static List<Long> subtract(List<Long> column1, List<Long> column2) {
        if (column1.size() != column2.size()) {
            return null;
        }
        List<Long> result = new ArrayList<Long>();
        for (int i = 0; i < column1.size(); i++) {
            result.add(column1.get(i) - column2.get(i));
        }
        return result;
    }


    /**
     * <p>
     * Filters out the data rows that do not satisfy the range parameter and returns the result as a new TableData instance.
     * (Similar to a Database select query).
     * </p>
     *
     * @param column The column over which the range check is applied.
     * @param range  The range to apply for the filtering.
     * @return The result of applying the range filter as a new TableData instance.
     */
    public TableData<T> filter(Integer column, Range<T> range) {
        ArrayList<Integer> satisfyingRows = Lists.newArrayList();
        for (Integer row : data.rowKeySet()) {
            if (range.contains(data.get(row, column))) {
                satisfyingRows.add(row);
            }
        }
        Collections.sort(satisfyingRows);

        HashBasedTable<Integer, Integer, T> filteredResults = HashBasedTable.create();
        for (int i = 0; i < satisfyingRows.size(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                filteredResults.put(i, j, data.get(satisfyingRows.get(i), j));
            }
        }
        TableData<T> result = new TableData<T>(filteredResults);
        result.rowSize = satisfyingRows.size();
        result.columnSize = columnSize;
        return result;
    }

    public int rowSize() {
        return rowSize;
    }

    public int columnSize() {
        return columnSize;
    }

    public boolean columnContains(Integer columnNumber, T value) {
        return data.column(columnNumber).containsValue(value);
    }

    public boolean rowContains(Integer rowNumber, T value) {
        return data.row(rowNumber).containsValue(value);
    }

    /**
     * Adds and entire row of values to the table.
     *
     * @param row
     */
    public void addRow(T[] row) {

        int rowLength = row.length;
        if (columnSize() < rowLength ) {
            columnSize = rowLength;
        }
        int index = rowSize++;
        for (int c = 0; c < rowLength; c++) {
            data.put(index, c, row[c]);
        }
    }

    /**
     * Adds an entire column of values to the table.
     *
     * @param column
     */
    public void addColumn(T[] column) {
        int columnLength = column.length;
        if (rowSize() < columnLength) {
            rowSize = columnLength;
        }
        int index = columnSize++;
        for (int r = 0; r < columnLength; r++) {
            data.put(r, index, column[r]);
        }
    }

    /**
     * Replaces the context of an existing column.
     * @param columnNumber the column to be replaced
     * @param column the contect to be put in the column.
     */
    public void setColumn(int columnNumber, T[] column) {
        for (int r = 0; r < column.length; r++ ) {
            data.put(r, columnNumber, column[r]);
        }
    }

    /**
     * Adds a cell to the table.
     *
     * @param row    the row index for the value to be inserted.
     * @param column the column index for the value to be inserted.
     * @param value  the value to be inserted.
     */
    private void put(Integer row, Integer column, T value) {
        data.put(row, column, value);
    }

    public T get(Integer row, Integer column) {
        return data.get(row, column);
    }

    /**
     * Returns an entire row of the Table
     *
     * @param r The row to return
     * @return The specified row as a list
     */
    public List<T> getRow(Integer r) {
        //TODO: check it the order is kept correct when iterataing over the values.
        return Lists.newArrayList(data.row(r).values());
    }

    /**
     * Returns an entire column of the table
     *
     * @param c The column to return
     * @return The specified column as a list.
     */
    public List<T> getColumn(Integer c) {
        //TODO: check it the order is kept correct when iterataing over the values.
        return Lists.newArrayList(data.column(c).values());
    }


}
