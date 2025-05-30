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
 * Utility class for interacting with Postgres databases.
 * This class provides methods for database connection, SQL query generation,
 * column information retrieval, and data type mapping.
 *
 * @author Brian Pace
 */
public class dbPostgres {
    public static final String nativeCase = "lower";

    private static final String THREAD_NAME = "dbPostgres";

    /**
     * Builds a SQL query for loading data from a Postgres table.
     *
     * @param useDatabaseHash Whether to use MD5 hash for database columns.
     * @return SQL query string for loading data from the specified table.
     */
    public static String buildLoadSQL (Boolean useDatabaseHash, DCTableMap tableMap, ColumnMetadata columnMetadata) {
        String sql = "SELECT ";

        if (useDatabaseHash) {
            sql += "md5(concat_ws('|'," + columnMetadata.getPk() + ")) pk_hash, " + columnMetadata.getPkJSON() + " pk, md5(concat_ws(''," + columnMetadata.getColumn() + ")) FROM " +  ShouldQuoteString(tableMap.isSchemaPreserveCase(), tableMap.getSchemaName()) + "." + ShouldQuoteString(tableMap.isTablePreserveCase(),tableMap.getTableName()) + " WHERE 1=1 ";
        } else {
            sql +=  columnMetadata.getPk() + " as pk_hash, " + columnMetadata.getPkJSON() + " as pk, " + columnMetadata.getColumn() + " FROM " + ShouldQuoteString(tableMap.isSchemaPreserveCase(), tableMap.getSchemaName()) + "." + ShouldQuoteString(tableMap.isTablePreserveCase(),tableMap.getTableName()) + " WHERE 1=1 ";
        }

        if (tableMap.getTableFilter() != null && !tableMap.getTableFilter().isEmpty()) {
            sql += " AND " + tableMap.getTableFilter();
        }

        return sql;
    }

    /**
     * Generates a column value expression for Postgres based on the column's data type.
     *
     * @param column JSONObject containing column information.
     * @return String representing the column value expression.
     */
    public static String columnValueMapPostgres(Properties Props, JSONObject column) {
        String colExpression;
        String columnName = ShouldQuoteString(column.getBoolean("preserveCase"), column.getString("columnName"));

        if ( Arrays.asList(numericTypes).contains(column.getString("dataType").toLowerCase()) ) {
            colExpression = switch (column.getString("dataType").toLowerCase()) {
                case "float4", "float8" ->
                        "coalesce(trim(to_char(" + columnName + ",'0.999999EEEE')),' ')";
                default ->
                        Props.getProperty("number-cast").equals("notation") ? "coalesce(trim(to_char(" + columnName + ",'0.9999999999EEEE')),' ')" : "coalesce(trim(to_char(trim_scale(" + columnName + "),'"+ Props.getProperty("standard-number-format") + "')),' ')";
            };

        } else if ( Arrays.asList(booleanTypes).contains(column.getString("dataType").toLowerCase()) ) {
            String booleanConvert = "case when coalesce(" + columnName + "::text,'0') = 'true' then 1 else 0 end";
            colExpression = Props.getProperty("number-cast").equals("notation") ?  "coalesce(trim(to_char(" + booleanConvert + ",'0.9999999999EEEE')),' ')" : "coalesce(trim(to_char(trim_scale(" + booleanConvert + "),'"+ Props.getProperty("standard-number-format") + "')),' ')";
        } else if ( Arrays.asList(timestampTypes).contains(column.getString("dataType").toLowerCase()) ) {
            if (column.getString("dataType").toLowerCase().contains("time zone") || column.getString("dataType").toLowerCase().contains("tz") ) {
                colExpression = "coalesce(to_char(" + columnName + " at time zone 'UTC','MMDDYYYYHH24MISS'),' ')";
            } else {
                colExpression = "coalesce(to_char(" + columnName + ",'MMDDYYYYHH24MISS'),' ')";
            }
        } else if ( Arrays.asList(charTypes).contains(column.getString("dataType").toLowerCase()) ) {
            colExpression = "coalesce(case when length(coalesce(trim(" + columnName + "::text),''))=0 then ' ' else trim(" + columnName + "::text) end,' ')";
        } else if ( Arrays.asList(binaryTypes).contains(column.getString("dataType").toLowerCase()) ) {
            colExpression = "coalesce(md5(" + columnName +"), ' ')";
        } else {
            colExpression = "coalesce(case when length(coalesce(" + columnName + "::text,''))=0 then ' ' else " + columnName + "::text end,' ')";
        }

        return colExpression;

    }

    /**
     * Establishes a connection to a Postgres database using the provided connection properties.
     *
     * @param connectionProperties Properties containing database connection information.
     * @param destType             Type of destination (e.g., source, target).
     * @return Connection object to Postgres database.
     */
    public static Connection getConnection(Properties connectionProperties, String destType, String module) {
        Connection conn = null;
        String url = "jdbc:postgresql://"+connectionProperties.getProperty(destType+"-host")+":"+connectionProperties.getProperty(destType+"-port")+"/"+connectionProperties.getProperty(destType+"-dbname")+"?sslmode="+connectionProperties.getProperty(destType+"-sslmode");
        Properties dbProps = new Properties();

        dbProps.setProperty("user",connectionProperties.getProperty(destType+"-user"));
        dbProps.setProperty("password",connectionProperties.getProperty(destType+"-password"));
        dbProps.setProperty("options","-c search_path="+connectionProperties.getProperty(destType+"-schema")+",public,pg_catalog");
        dbProps.setProperty("reWriteBatchedInserts", "true");
        dbProps.setProperty("preparedStatementCacheQueries", "5");
        dbProps.setProperty("ApplicationName", "pgCompare - " + module);
        dbProps.setProperty("synchronous_commit", "off");

        try {
            conn = DriverManager.getConnection(url,dbProps);
            conn.setAutoCommit(false);
        } catch (Exception e) {
            Logging.write("severe", THREAD_NAME, String.format("Error connecting to Postgres:  %s", e.getMessage()));
        }

        return conn;

    }


}
