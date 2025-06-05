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

package com.crunchydata.util;

public interface SQLConstantsDB2 {
    //
    // DB2 SQL
    //
    String SQL_DB2_SELECT_COLUMNS = 
            "SELECT trim(c.TABSCHEMA) AS owner,\n" +
            "       c.TABNAME AS table_name,\n" +
            "       c.COLNAME AS column_name,\n" +
            "       LOWER(c.TYPENAME) AS data_type,\n" +
            "       c.LENGTH AS data_length,\n" +
            "       COALESCE(c.length, 44) AS data_precision,\n" +
            "       COALESCE(c.SCALE, 22) AS data_scale,\n" +
            "       c.NULLS AS nullable,\n" +
            "       CASE WHEN pkc.COLNAME IS NULL THEN 'N' ELSE 'Y' END AS pk\n" +
            "FROM SYSCAT.COLUMNS c\n" +
            "     LEFT JOIN (SELECT k.TABSCHEMA, k.TABNAME, k.COLNAME\n" +
            "                FROM SYSCAT.KEYCOLUSE k\n" +
            "                     JOIN SYSCAT.TABCONST tc ON (k.CONSTNAME = tc.CONSTNAME AND tc.TYPE = 'P')\n" +
            "               ) pkc ON (c.TABSCHEMA = pkc.TABSCHEMA AND c.TABNAME = pkc.TABNAME AND c.COLNAME = pkc.COLNAME)\n" +
            "WHERE LOWER(c.TABSCHEMA) = LOWER(?)\n" +
            "  AND LOWER(c.TABNAME) = LOWER(?)\n" +
            "ORDER BY c.TABSCHEMA, c.TABNAME, c.COLNAME";

    String SQL_DB2_SELECT_TABLES = 
                "SELECT trim(TABSCHEMA) AS owner, TABNAME AS table_name\n" +
                "FROM SYSCAT.TABLES\n" +
                "WHERE LOWER(TABSCHEMA) = LOWER(?)\n" +
                "ORDER BY TABSCHEMA, TABNAME";

    String SQL_DB2_SELECT_TABLE = 
                "SELECT trim(TABSCHEMA) AS owner, TABNAME AS table_name\n" +
                "FROM SYSCAT.TABLES\n" +
                "WHERE LOWER(TABSCHEMA) = LOWER(?)\n" +
                "      AND LOWER(TABNAME) = LOWER(?)\n" +
                "ORDER BY TABSCHEMA, TABNAME";

    String SQL_DB2_SELECT_VERSION = "SELECT service_level AS version FROM SYSIBMADM.ENV_INST_INFO";

}
