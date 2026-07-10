/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.fluo.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.fluo.accumulo.util.ColumnType;
import org.apache.fluo.api.data.Bytes;
import org.apache.fluo.api.data.Column;
import org.apache.fluo.api.data.RowColumn;
import org.apache.fluo.core.util.ByteUtil;
import org.apache.fluo.core.util.CachedBytesConverter;
import org.apache.fluo.core.util.CachedColumnConverter;
import org.apache.fluo.core.util.ColumnUtil;
import org.apache.fluo.core.util.UtilWaitThread;

public class ParallelSnapshotScanner {

  private Environment env;
  private long startTs;
  private Collection<Bytes> rows;
  private Set<Column> columns;
  private TxStats stats;
  private List<Range> rangesToScan = new ArrayList<>();
  private Function<ByteSequence, Bytes> rowConverter;
  private Function<Key, Column> columnConverter;
  private Map<Bytes, Set<Column>> readLocksSeen;
  private Consumer<Entry<Key, Value>> writeLocksSeen;

  private Authorizations authorizations;

  ParallelSnapshotScanner(Collection<Bytes> rows, Set<Column> columns, Environment env,
      long startTs, TxStats stats, Map<Bytes, Set<Column>> readLocksSeen,
      Consumer<Entry<Key, Value>> writeLocksSeen, Authorizations authorizations) {
    this.rows = rows;
    this.columns = columns;
    this.env = env;
    this.startTs = startTs;
    this.stats = stats;
    this.rowConverter = new CachedBytesConverter(rows);
    this.columnConverter = new CachedColumnConverter(columns);
    this.readLocksSeen = readLocksSeen;
    this.writeLocksSeen = writeLocksSeen;
    this.authorizations = authorizations;
  }

  ParallelSnapshotScanner(Collection<Bytes> rows, Set<Column> columns, Environment env,
      long startTs, TxStats stats, Map<Bytes, Set<Column>> readLocksSeen,
      Consumer<Entry<Key, Value>> writeLocksSeen) {
    this(rows, columns, env, startTs, stats, readLocksSeen, writeLocksSeen,
        env.getAuthorizations());
  }

  ParallelSnapshotScanner(Collection<RowColumn> cells, Environment env, long startTs, TxStats stats,
      Map<Bytes, Set<Column>> readLocksSeen, Consumer<Entry<Key, Value>> writeLocksSeen) {
    this(cells, env, startTs, stats, readLocksSeen, writeLocksSeen, env.getAuthorizations());
  }

  ParallelSnapshotScanner(Collection<RowColumn> cells, Environment env, long startTs, TxStats stats,
      Map<Bytes, Set<Column>> readLocksSeen, Consumer<Entry<Key, Value>> writeLocksSeen,
      Authorizations authorizations) {
    for (RowColumn rc : cells) {
      byte[] r = rc.getRow().toArray();
      byte[] cf = rc.getColumn().getFamily().toArray();
      byte[] cq = rc.getColumn().getQualifier().toArray();
      byte[] cv = new byte[0];
      byte[] cv2 = new byte[] {(byte) 0xff};

      Key start = new Key(r, cf, cq, cv, Long.MAX_VALUE, false, false);
      Key end = new Key(r, cf, cq, cv2, Long.MIN_VALUE, false, false);

      rangesToScan.add(new Range(start, true, end, true));
    }
    this.rows = null;
    this.env = env;
    this.startTs = startTs;
    this.stats = stats;
    this.rowConverter = ByteUtil::toBytes;
    this.columnConverter = ColumnUtil::convert;
    this.readLocksSeen = readLocksSeen;
    this.writeLocksSeen = writeLocksSeen;
    this.authorizations = authorizations;
  }

  private BatchScanner setupBatchScanner() {

    BatchScanner scanner;
    try {
      scanner =
          env.getAccumuloClient().createBatchScanner(env.getTable(), env.getAuthorizations(),
                  env.getConfiguration().getNumParallelScanThreads());
    } catch (TableNotFoundException e) {
      throw new RuntimeException(e);
    }

    scanner.clearColumns();
    scanner.clearScanIterators();

    if (!rangesToScan.isEmpty()) {
      scanner.setRanges(rangesToScan);
      SnapshotScanner.setupScanner(scanner, Collections.<Column>emptySet(), startTs, true);
    } else if (rows != null) {
      List<Range> ranges = new ArrayList<>(rows.size());

      for (Bytes row : rows) {
        ranges.add(Range.exact(ByteUtil.toText(row)));
      }

      scanner.setRanges(ranges);

      SnapshotScanner.setupScanner(scanner, columns, startTs, true);
    } else {
      return null;
    }

    return scanner;
  }

  Map<Bytes, Map<Column, Bytes>> scan() {

    long waitTime = SnapshotScanner.INITIAL_WAIT_TIME;
    long startTime = System.currentTimeMillis();

    Map<Bytes, Map<Column, Bytes>> ret = new HashMap<>();

    while (true) {
      List<Entry<Key, Value>> locks = new ArrayList<>();

      scan(ret, locks);

      if (!locks.isEmpty()) {

        boolean resolvedAll = LockResolver.resolveLocks(env, startTs, stats, locks, startTime);

        if (!resolvedAll) {
          UtilWaitThread.sleep(waitTime);
          stats.incrementLockWaitTime(waitTime);
          waitTime = Math.min(SnapshotScanner.MAX_WAIT_TIME, waitTime * 2);
        }

        // retain the rows that were locked for future scans
        rangesToScan.clear();
        rows = null;
        for (Entry<Key, Value> entry : locks) {
          Key start = new Key(entry.getKey());
          start.setTimestamp(Long.MAX_VALUE);
          Key end = new Key(entry.getKey());
          end.setTimestamp(Long.MIN_VALUE);
          rangesToScan.add(new Range(start, true, end, true));
        }

        continue;
      }

      for (Map<Column, Bytes> cols : ret.values()) {
        stats.incrementEntriesReturned(cols.size());
      }

      return ret;
    }
  }

  private void scan(Map<Bytes, Map<Column, Bytes>> ret, List<Entry<Key, Value>> locks) {

    BatchScanner bs = setupBatchScanner();
    try {
      for (Entry<Key, Value> entry : bs) {
        Bytes row = rowConverter.apply(entry.getKey().getRowData());
        Column col = columnConverter.apply(entry.getKey());

        ColumnType colType = ColumnType.from(entry.getKey());
        switch (colType) {
          case LOCK:
            locks.add(entry);
            writeLocksSeen.accept(entry);
            break;
          case DATA:
            ret.computeIfAbsent(row, k -> new HashMap<>()).put(col,
                Bytes.of(entry.getValue().get()));
            break;
          case RLOCK:
            readLocksSeen.computeIfAbsent(row, k -> new HashSet<>()).add(col);
            break;
          default:
            throw new IllegalArgumentException("Unexpected column type " + colType);
        }
      }
    } finally {
      bs.close();
    }
  }
}
