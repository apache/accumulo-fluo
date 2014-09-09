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
package io.fluo.api.observer;

import java.util.Map;

import io.fluo.api.client.Transaction;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;

/**
 * An observer is created for each worker thread and re-used for the lifetime of a worker thread.
 *
 * Consider extending {@link AbstractObserver} instead of implementing this. The abstract class will shield you from the addition of interface methods.
 */
public interface Observer {

  public static enum NotificationType {
    WEAK, STRONG
  }

  public static class ObservedColumn {
    private Column col;
    private NotificationType notificationType;

    public ObservedColumn(Column col, NotificationType notificationType) {
      this.col = col;
      this.notificationType = notificationType;
    }

    public Column getColumn() {
      return col;
    }

    public NotificationType getType() {
      return notificationType;
    }
  }

  public void init(Map<String,String> config) throws Exception;

  public void process(Transaction tx, Bytes row, Column col) throws Exception;

  /**
   * It's safe to assume that {@link #init(Map)} will be called before this method. If the return value of the method is derived from what's passed to
   * {@link #init(Map)}, then the derivation process should be deterministic.
   *
   * @return The column that will trigger this observer. During initialization this information is stored in zookeeper so that workers have a consistent view.
   *         If a worker loads an Observer and the information returned differs from whats in zookeeper then an exception will be thrown.
   */
  public ObservedColumn getObservedColumn();

  public void close();
}
