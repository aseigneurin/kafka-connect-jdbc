/**
 * Copyright 2015 Datamountaineer.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.datamountaineer.streamreactor.connect.jdbc.sink.writer.dialect;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.apache.kafka.connect.data.Schema;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

/**
 * Created by andrew@datamountaineer.com on 17/05/16.
 * kafka-connect-jdbc
 */
public class PostgreSQLDialect extends DbDialect {
  private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDialect.class);

  public PostgreSQLDialect() {
    super(getSqlTypeMap());
  }

  private static Map<Schema.Type, String> getSqlTypeMap() {
    Map<Schema.Type, String> map = new HashMap<>();
    map.put(Schema.Type.INT8, "SMALLINT");
    map.put(Schema.Type.INT16, "SMALLINT");
    map.put(Schema.Type.INT32, "INT");
    map.put(Schema.Type.INT64, "BIGINT");
    map.put(Schema.Type.FLOAT32, "FLOAT");
    map.put(Schema.Type.FLOAT64, "DOUBLE PRECISION");
    map.put(Schema.Type.BOOLEAN, "BOOLEAN");
    map.put(Schema.Type.STRING, "TEXT");
    map.put(Schema.Type.BYTES, "BYTEA");
    return map;
  }


  @Override
  public String getUpsertQuery(final String table, final List<String> nonKeyColumns, final List<String> keyColumns) {
    if (table == null || table.trim().length() == 0)
      throw new IllegalArgumentException("<table=> is not valid. A non null non empty string expected");

    if (keyColumns == null || keyColumns.size() == 0) {
      throw new IllegalArgumentException("<keyColumns> is invalid. Need to be non null, non empty and be a subset of <columns>");
    }


    final String queryColumns = Joiner.on(",").join(Iterables.concat(nonKeyColumns, keyColumns));
    final String bindingValues = Joiner.on(",").join(Collections.nCopies(nonKeyColumns.size() + keyColumns.size(), "?"));

    String updateSet = null;
    if (nonKeyColumns.size() > 0) {
      final StringBuilder updateSetBuilder = new StringBuilder();
      updateSetBuilder.append(String.format("%s=EXCLUDED.%s", nonKeyColumns.get(0), nonKeyColumns.get(0)));
      for (int i = 1; i < nonKeyColumns.size(); ++i) {
        updateSetBuilder.append(String.format(",%s=EXCLUDED.%s", nonKeyColumns.get(i), nonKeyColumns.get(i)));
      }
      updateSet = updateSetBuilder.toString();
    }

    String sql = "INSERT INTO " + table + " (" + queryColumns + ") " +
                 "VALUES (" + bindingValues + ") " +
                 "ON CONFLICT (" + Joiner.on(",").join(Iterables.concat(keyColumns)) + ") DO UPDATE SET " + updateSet;


    logger.debug("Prepared sql: " + sql);

    return sql;
  }
}
