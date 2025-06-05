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

public interface SQLConstantsPostgres {
    //
    // Postgres SQL
    //
    String SQL_POSTGRES_SELECT_COLUMNS = 
                "SELECT DISTINCT n.nspname as owner, t.relname table_name, c.attname column_name,\n" +
                "        col.udt_name data_type, coalesce(col.character_maximum_length,col.numeric_precision) data_length,\n" +
                "                coalesce(col.numeric_precision,44) data_precision, coalesce(col.numeric_scale,22) data_scale,\n" +
                "        CASE WHEN c.attnotnull THEN 'Y' ELSE 'N' END nullable,\n" +
                "        CASE WHEN i.indisprimary THEN 'Y' ELSE 'N' END pk\n" +
                "FROM pg_class t\n" +
                "     JOIN pg_attribute c ON (t.oid=c.attrelid)\n" +
                "     JOIN pg_namespace n ON (t.relnamespace=n.oid)\n" +
                "     JOIN information_schema.columns col ON (col.table_schema=n.nspname AND col.table_name=t.relname AND col.column_name=c.attname)\n" +
                "     LEFT OUTER JOIN pg_index i ON (i.indrelid=c.attrelid AND c.attnum = any(i.indkey) AND i.indisprimary)\n" +
                "WHERE lower(n.nspname)=lower(?)\n" +
                "      AND lower(t.relname)=lower(?)\n" +
                "ORDER BY n.nspname, t.relname, c.attname";

    String SQL_POSTGRES_SELECT_TABLES = 
                "SELECT table_schema as owner, table_name\n" +
                "FROM  information_schema.tables\n" +
                "WHERE lower(table_schema)=lower(?)\n" +
                "      AND table_type != 'VIEW'\n" +
                "ORDER BY table_schema, table_name";

    String SQL_POSTGRES_SELECT_TABLE = 
                "SELECT table_schema as owner, table_name\n" +
                "FROM  information_schema.tables\n" +
                "WHERE lower(table_schema)=lower(?)\n" +
                "      AND table_type != 'VIEW'\n" +
                "      AND lower(table_name) = lower(?)\n" +
                "ORDER BY table_schema, table_name";

    String SQL_POSTGRES_SELECT_VERSION = "SELECT v.ver[2]::numeric version FROM (SELECT string_to_array(version(),' ') AS ver) v";

}
