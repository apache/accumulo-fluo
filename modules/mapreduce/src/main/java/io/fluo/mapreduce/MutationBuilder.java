/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fluo.mapreduce;

import io.fluo.accumulo.util.ColumnConstants;
import io.fluo.accumulo.values.WriteValue;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.core.util.ByteUtil;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;

/**
 * This class allows building Accumulo mutations that are in the Fluo data format. This class is intended to be used with {@link AccumuloOutputFormat}
 * in order to seed an initialized Fluo table on which no transactions have executed.
 */

public class MutationBuilder {

  private Mutation mutation;

  public MutationBuilder(Bytes row) {
    if (row.isBackedByArray())
      mutation = new Mutation(row.getBackingArray(), row.offset(), row.length());
    else
      mutation = new Mutation(row.toArray());
  }

  public MutationBuilder put(Column col, Bytes value) {

    Text fam = ByteUtil.toText(col.getFamily());
    Text qual = ByteUtil.toText(col.getQualifier());

    mutation.put(fam, qual, col.getVisibilityParsed(), ColumnConstants.DATA_PREFIX | 0, new Value(value.toArray()));
    mutation.put(fam, qual, col.getVisibilityParsed(), ColumnConstants.WRITE_PREFIX | 1, new Value(WriteValue.encode(0, false, false)));

    return this;
  }

  Mutation build() {
    Mutation ret = mutation;
    mutation = null;
    return ret;
  }
}
