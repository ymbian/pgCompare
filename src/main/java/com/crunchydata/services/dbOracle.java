/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crunchydata.services;


import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Properties;

import com.crunchydata.models.ColumnMetadata;
import com.crunchydata.models.DCTableMap;
import com.crunchydata.util.Logging;

import org.json.JSONObject;

import static com.crunchydata.util.ColumnUtility.*;
import static com.crunchydata.util.DataUtility.ShouldQuoteString;

/**
 * Utility class for interacting with Oracle databases.
 * This class provides methods for database connection, SQL query generation,
 * column information retrieval, and data type mapping.
 *
 *
 * @author Brian Pace
 */
public class dbOracle {
    public static final String nativeCase = "upper";

    private static final String THREAD_NAME = "dbOracle";

    /**
     * Builds a SQL query for loading data from an Oracle table.
     *
     * @param useDatabaseHash Whether to use MD5 hash for database columns.
     * @return SQL query string for loading data from the specified table.
     */
    public static String buildLoadSQL (Boolean useDatabaseHash, DCTableMap tableMap, ColumnMetadata columnMetadata) {
        String sql = "SELECT ";

        if (useDatabaseHash) {
            sql += "LOWER(STANDARD_HASH(" +  columnMetadata.getPk() + ",'MD5')) pk_hash, " + columnMetadata.getPkJSON() + " pk, LOWER(STANDARD_HASH(" + columnMetadata.getColumn() + ",'MD5')) column_hash FROM " + ShouldQuoteString(tableMap.isSchemaPreserveCase(), tableMap.getSchemaName()) + "." + ShouldQuoteString(tableMap.isTablePreserveCase(),tableMap.getTableName()) + " WHERE 1=1 ";
        } else {
            sql +=  columnMetadata.getPk() + " pk_hash, " + columnMetadata.getPkJSON() + " pk, " + columnMetadata.getColumn() + " FROM " + ShouldQuoteString(tableMap.isSchemaPreserveCase(), tableMap.getSchemaName()) + "." + ShouldQuoteString(tableMap.isTablePreserveCase(),tableMap.getTableName()) + " WHERE 1=1 ";
        }

        if (tableMap.getTableFilter() != null && !tableMap.getTableFilter().isEmpty()) {
            sql += " AND " + tableMap.getTableFilter();
        }

        return sql;
    }

    /**
     * Generates a column value expression for Oracle based on the column's data type.
     *
     * @param column JSONObject containing column information.
     * @return String representing the column value expression.
     */
    public static String columnValueMapOracle(Properties Props, JSONObject column) {
        String colExpression;
        String columnName = ShouldQuoteString(column.getBoolean("preserveCase"), column.getString("columnName"));

        if ( Arrays.asList(numericTypes).contains(column.getString("dataType").toLowerCase()) ) {

            colExpression = switch (column.getString("dataType").toLowerCase()) {
                case "float", "binary_float", "binary_double" ->
                        "lower(nvl(trim(to_char(" +columnName + ",'0.999999EEEE')),' '))";
                default ->
                        Props.getProperty("number-cast").equals("notation") ? "lower(nvl(trim(to_char(" + columnName+ ",'0.9999999999EEEE')),' '))" : "nvl(trim(to_char(" + columnName+ ",'" + Props.getProperty("standard-number-format") + "')),' ')";
            };


        } else if ( Arrays.asList(booleanTypes).contains(column.getString("dataType").toLowerCase()) ) {
            colExpression = "nvl(to_char(" + columnName + "),'0')";
        } else if ( Arrays.asList(timestampTypes).contains(column.getString("dataType").toLowerCase()) ) {
            if (column.getString("dataType").toLowerCase().contains("time zone") || column.getString("dataType").toLowerCase().contains("tz") ) {
                colExpression = "nvl(to_char(" + columnName + " at time zone 'UTC','MMDDYYYYHH24MISS'),' ')";
            } else {
                colExpression = "nvl(to_char(" + columnName + ",'MMDDYYYYHH24MISS'),' ')";
            }
        } else if ( Arrays.asList(charTypes).contains(column.getString("dataType").toLowerCase()) ) {
            if (column.getString("dataType").toLowerCase().contains("lob")) {
                colExpression = "nvl(trim(to_char(" + columnName + ")),' ')";
            } else {
                if (column.getInt("dataLength") > 1) {
                    colExpression = "nvl(trim(" + columnName + "),' ')";
                } else {
                    colExpression = "nvl(" + columnName + ",' ')";
                }
            }
        } else if ( Arrays.asList(binaryTypes).contains(column.getString("dataType").toLowerCase()) ) {
            colExpression = "case when dbms_lob.getlength(" + columnName +") = 0 or " + columnName + " is null then ' ' else lower(dbms_crypto.hash(" + columnName + ",2)) end";
        } else {
            colExpression = "nvl(" + columnName + ",' ')";
        }

        return colExpression;

    }

    /**
     * Establishes a connection to an Oracle database using the provided connection properties.
     *
     * @param connectionProperties Properties containing database connection information.
     * @param destType             Type of destination (e.g., source, target).
     * @return Connection object to Oracle database.
     */
    public static Connection getConnection(Properties connectionProperties, String destType) {
        Connection conn = null;
        String url = "jdbc:oracle:thin:@//"+connectionProperties.getProperty(destType+"-host")+":"+connectionProperties.getProperty(destType+"-port")+"/"+connectionProperties.getProperty(destType+"-dbname");
        Properties dbProps = new Properties();

        dbProps.setProperty("user",connectionProperties.getProperty(destType+"-user"));
        dbProps.setProperty("password",connectionProperties.getProperty(destType+"-password"));

        try {
            conn = DriverManager.getConnection(url,dbProps);
        } catch (Exception e) {
            Logging.write("severe", THREAD_NAME, String.format("Error connecting to Oracle %s",e.getMessage()));
        }

        return conn;

    }

}
