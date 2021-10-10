package nextstep.jdbc.utils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nextstep.jdbc.RowMapper;

public class MockResultSet {

    private final Map<String, Integer> columnIndices;
    private final Object[][] data;
    private int rowIndex;
    private boolean isClosed;

    /**
     * Creates the mock ResultSet.
     *
     * @param columnNames the names of the columns
     * @param data reference for ResultSet to retrieve data from
     * @return a mocked ResultSet
     * @throws SQLException thrown when methods are called with closed result set
     */
    public static ResultSet of(final String[] columnNames, final Object[][] data)
        throws SQLException {
        return new MockResultSet(columnNames, data).buildMock();
    }

    /**
     * Creates the mock ResultSet with Sample class that has name and number as its fields.
     *
     * @param quantity quantity of results in ResultSet with name and number "0" & 0, "1" & 1 and so
     *                 on.
     * @return a mocked ResultSet
     * @throws SQLException thrown when methods are called with closed result set
     */
    public static ResultSet ofSample(int quantity) throws SQLException {
        String[] columnNames = new String[]{"name", "number"};
        Object[][] data = new Object[quantity][columnNames.length];
        for (int i = 0; i < quantity; i++) {
            data[i][0] = String.valueOf(i);
            data[i][1] = i;
        }
        return of(columnNames, data);
    }

    public static RowMapper<Sample> sampleRowMapper() {
        return rs -> new Sample(
            rs.getString("name"),
            rs.getInt("number")
        );
    }

    private MockResultSet(final String[] columnNames, final Object[][] data) {
        // create a map of column name to column index
        this.columnIndices = IntStream.range(0, columnNames.length)
            .boxed()
            .collect(Collectors.toMap(
                k -> columnNames[k],
                Function.identity(),
                (a, b) -> {
                    throw new RuntimeException("Duplicate column " + a);
                },
                LinkedHashMap::new
            ));
        this.data = data;
        this.rowIndex = -1;
    }

    private ResultSet buildMock() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);

        // mock resultSet.close()
        doAnswer(invocation -> isClosed = true).when(resultSet).close();

        // mock resultSet.next()
        doAnswer(invocation -> {
            checkClosed();
            rowIndex++;
            return rowIndex < data.length;
        }).when(resultSet).next();

        // mock resultSet.getString(columnName)
        doAnswer(invocation -> {
            checkClosed();
            String columnName = invocation.getArgument(0, String.class);
            int columnIndex = columnIndices.get(columnName);
            return data[rowIndex][columnIndex];
        }).when(resultSet).getString(anyString());

        // mock resultSet.getInt(columnName)
        doAnswer(invocation -> {
            checkClosed();
            String columnName = invocation.getArgument(0, String.class);
            int columnIndex = columnIndices.get(columnName);
            return data[rowIndex][columnIndex];
        }).when(resultSet).getInt(anyString());

        // mock resultSet.getObject(columnIndex)
        doAnswer(invocation -> {
            checkClosed();
            int index = invocation.getArgument(0, Integer.class);
            return data[rowIndex][index - 1];
        }).when(resultSet).getObject(anyInt());

        final var resultSetMetaData = mock(ResultSetMetaData.class);

        // mock resultSetMetaData.getColumnCount()
        doReturn(columnIndices.size()).when(resultSetMetaData).getColumnCount();

        // mock resultSet.getMetaData()
        doReturn(resultSetMetaData).when(resultSet).getMetaData();

        return resultSet;
    }

    private void checkClosed() throws SQLException {
        if (isClosed) {
            throw new SQLException();
        }
    }

    public static class Sample {

        private final String name;
        private final int number;

        public Sample(String name, int number) {
            this.name = name;
            this.number = number;
        }
    }
}