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

package org.apache.fluo.api.config;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.observer.ObserverProvider;
import org.apache.fluo.api.observer.ObserverProvider.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration helper class for Fluo
 *
 * @since 1.0.0
 */
public class FluoConfiguration extends SimpleConfiguration {

  private static final long serialVersionUID = 1L;

  private static final Logger log = LoggerFactory.getLogger(FluoConfiguration.class);

  public static final String FLUO_PREFIX = "fluo";

  // Client properties
  private static final String CLIENT_PREFIX = FLUO_PREFIX + ".client";
  /**
   * @deprecated since 1.2.0 replaced by fluo.connection.application.name
   */
  @Deprecated
  public static final String CLIENT_APPLICATION_NAME_PROP = CLIENT_PREFIX + ".application.name";
  /**
   * @deprecated since 1.2.0 replaced by fluo.accumulo.password
   */
  @Deprecated
  public static final String CLIENT_ACCUMULO_PASSWORD_PROP = CLIENT_PREFIX + ".accumulo.password";
  /**
   * @deprecated since 1.2.0 replaced by fluo.accumulo.user
   */
  @Deprecated
  public static final String CLIENT_ACCUMULO_USER_PROP = CLIENT_PREFIX + ".accumulo.user";
  /**
   * @deprecated since 1.2.0 replaced by fluo.accumulo.instance
   */
  @Deprecated
  public static final String CLIENT_ACCUMULO_INSTANCE_PROP = CLIENT_PREFIX + ".accumulo.instance";
  /**
   * @deprecated since 1.2.0 replaced by fluo.accumulo.zookeepers
   */
  @Deprecated
  public static final String CLIENT_ACCUMULO_ZOOKEEPERS_PROP =
      CLIENT_PREFIX + ".accumulo.zookeepers";
  /**
   * @deprecated since 1.2.0 replaced by fluo.connection.zookeeper.timeout
   */
  @Deprecated
  public static final String CLIENT_ZOOKEEPER_TIMEOUT_PROP = CLIENT_PREFIX + ".zookeeper.timeout";
  /**
   * @deprecated since 1.2.0 replaced by fluo.connection.zookeepers
   */
  @Deprecated
  public static final String CLIENT_ZOOKEEPER_CONNECT_PROP = CLIENT_PREFIX + ".zookeeper.connect";
  /**
   * @deprecated since 1.2.0 replaced by fluo.connection.retry.timeout.ms
   */
  @Deprecated
  public static final String CLIENT_RETRY_TIMEOUT_MS_PROP = CLIENT_PREFIX + ".retry.timeout.ms";
  @Deprecated
  public static final int CLIENT_ZOOKEEPER_TIMEOUT_DEFAULT = 30000;
  @Deprecated
  public static final String CLIENT_ACCUMULO_ZOOKEEPERS_DEFAULT = "localhost";
  @Deprecated
  public static final String CLIENT_ZOOKEEPER_CONNECT_DEFAULT = "localhost/fluo";
  @Deprecated
  public static final int CLIENT_RETRY_TIMEOUT_MS_DEFAULT = -1;

  // Connection properties
  /**
   * @since 1.2.0
   */
  public static final String CONNECTION_PREFIX = FLUO_PREFIX + ".connection";
  /**
   * @since 1.2.0
   */
  public static final String CONNECTION_APPLICATION_NAME_PROP =
      CONNECTION_PREFIX + ".application.name";
  /**
   * @since 1.2.0
   */
  public static final String CONNECTION_ZOOKEEPER_TIMEOUT_PROP =
      CONNECTION_PREFIX + ".zookeeper.timeout";

  /**
   * @since 1.2.0
   */
  public static final String CONNECTION_ZOOKEEPER_SECRET = CONNECTION_PREFIX + ".zookeeper.secret";

  /**
   * @since 1.2.0
   */
  public static final String CONNECTION_ZOOKEEPERS_PROP = CONNECTION_PREFIX + ".zookeepers";

  /**
   * @since 1.2.0
   */
  public static final String CONNECTION_RETRY_TIMEOUT_MS_PROP =
      CONNECTION_PREFIX + ".retry.timeout.ms";
  public static final int CONNECTION_ZOOKEEPER_TIMEOUT_DEFAULT = 30000;
  public static final String CONNECTION_ZOOKEEPERS_DEFAULT = "localhost/fluo";
  public static final int CONNECTION_RETRY_TIMEOUT_MS_DEFAULT = -1;

  // Accumulo properties
  private static final String ACCUMULO_PREFIX = FLUO_PREFIX + ".accumulo";
  /**
   * @since 1.2.0
   */
  public static final String ACCUMULO_INSTANCE_PROP = ACCUMULO_PREFIX + ".instance";
  /**
   * @since 1.2.0
   */
  public static final String ACCUMULO_TABLE_PROP = ACCUMULO_PREFIX + ".table";
  /**
   * @since 1.2.0
   */
  public static final String ACCUMULO_PASSWORD_PROP = ACCUMULO_PREFIX + ".password";
  /**
   * @since 1.2.0
   */
  public static final String ACCUMULO_USER_PROP = ACCUMULO_PREFIX + ".user";

  public static final String ACCUMULO_AUTH_PROP = ACCUMULO_PREFIX + ".auths";

  /**
   * @since 1.2.0
   */
  public static final String ACCUMULO_ZOOKEEPERS_PROP = ACCUMULO_PREFIX + ".zookeepers";
  /**
   * @since 1.2.0
   */
  public static final String ACCUMULO_JARS_PROP = ACCUMULO_PREFIX + ".jars";
  // Accumulo defaults
  public static final String ACCUMULO_ZOOKEEPERS_DEFAULT = "localhost";
  public static final String ACCUMULO_JARS_DEFAULT = "";

  // DFS properties
  private static final String DFS_PREFIX = FLUO_PREFIX + ".dfs";
  /**
   * @since 1.2.0
   */
  public static final String DFS_ROOT_PROP = DFS_PREFIX + ".root";
  // DFS defaults
  public static final String DFS_ROOT_DEFAULT = "";

  // Administration properties
  private static final String ADMIN_PREFIX = FLUO_PREFIX + ".admin";
  /**
   * @deprecated since 1.2.0 replaced by fluo.accumulo.table
   */
  @Deprecated
  public static final String ADMIN_ACCUMULO_TABLE_PROP = ADMIN_PREFIX + ".accumulo.table";

  // Worker properties
  private static final String WORKER_PREFIX = FLUO_PREFIX + ".worker";
  public static final String WORKER_NUM_THREADS_PROP = WORKER_PREFIX + ".num.threads";
  public static final int WORKER_NUM_THREADS_DEFAULT = 10;

  public static final String PARALLEL_SCAN_THREADS_PROP = FLUO_PREFIX + ".scan.num.threads";
  public static final int NUM_PARALLEL_SCAN_THREADS_DEFAULT = 1;

  // Loader properties
  private static final String LOADER_PREFIX = FLUO_PREFIX + ".loader";
  public static final String LOADER_NUM_THREADS_PROP = LOADER_PREFIX + ".num.threads";
  public static final String LOADER_QUEUE_SIZE_PROP = LOADER_PREFIX + ".queue.size";
  public static final int LOADER_NUM_THREADS_DEFAULT = 10;
  public static final int LOADER_QUEUE_SIZE_DEFAULT = 10;

  // MiniFluo properties
  private static final String MINI_PREFIX = FLUO_PREFIX + ".mini";
  public static final String MINI_START_ACCUMULO_PROP = MINI_PREFIX + ".start.accumulo";
  public static final String MINI_DATA_DIR_PROP = MINI_PREFIX + ".data.dir";
  public static final boolean MINI_START_ACCUMULO_DEFAULT = true;
  public static final String MINI_DATA_DIR_DEFAULT = "${env:FLUO_HOME}/mini";

  /** The properties below get loaded into/from Zookeeper */
  // Observer properties
  public static final String OBSERVER_PREFIX = FLUO_PREFIX + ".observer.";
  /**
   * @since 1.1.0
   */
  public static final String OBSERVER_PROVIDER = FLUO_PREFIX + ".observer.provider";
  /**
   * @since 1.2.0
   */
  public static final String OBSERVER_INIT_DIR_PROP = FLUO_PREFIX + ".observer.init.dir";
  /**
   * @since 1.2.0
   */
  public static final String OBSERVER_JARS_URL_PROP = FLUO_PREFIX + ".observer.jars.url";
  // Observer defaults
  public static final String OBSERVER_PROVIDER_DEFAULT = "";
  public static final String OBSERVER_INIT_DIR_DEFAULT = "";
  public static final String OBSERVER_JARS_URL_DEFAULT = "";

  // Transaction
  public static final String TRANSACTION_PREFIX = FLUO_PREFIX + ".tx";
  public static final String TRANSACTION_ROLLBACK_TIME_PROP = TRANSACTION_PREFIX + ".rollback.time";
  public static final long TRANSACTION_ROLLBACK_TIME_DEFAULT = 300000;

  // Metrics
  public static final String REPORTER_PREFIX = FLUO_PREFIX + ".metrics.reporter";

  // Application config
  public static final String APP_PREFIX = FLUO_PREFIX + ".app";

  public FluoConfiguration() {
    super();
  }

  public FluoConfiguration(SimpleConfiguration other) {
    super(other);
  }

  public FluoConfiguration(InputStream in) {
    super(in);
  }

  public FluoConfiguration(File propertiesFile) {
    super(propertiesFile);
  }

  public FluoConfiguration(Map<String, String> map) {
    super(map);
  }

  public void validate() {
    // keep in alphabetical order
    getAccumuloInstance();
    getAccumuloPassword();
    getAccumuloTable();
    getAccumuloUser();
    getAccumuloZookeepers();
    getApplicationName();
    getAppZookeepers();
    getConnectionRetryTimeout();
    getLoaderQueueSize();
    getLoaderThreads();
    getObserverSpecifications();
    getTransactionRollbackTime();
    getWorkerThreads();
    getZookeeperTimeout();
  }

  /**
   * Sets the {@value #CONNECTION_APPLICATION_NAME_PROP}
   *
   * @param applicationName Must not be null
   */
  public FluoConfiguration setApplicationName(String applicationName) {
    verifyApplicationName(applicationName);
    setProperty(CONNECTION_APPLICATION_NAME_PROP, applicationName);
    return this;
  }

  /**
   * Returns the application name after verification to avoid characters Zookeeper does not like in
   * nodes and Hadoop does not like in HDFS paths.
   * <p>
   * Gets the value of the property {@value #CONNECTION_APPLICATION_NAME_PROP} if set
   *
   * @return The application name
   * @throws NoSuchElementException if the property has not been set
   */
  public String getApplicationName() {
    String applicationName;
    if (containsKey(CONNECTION_APPLICATION_NAME_PROP)) {
      applicationName = getString(CONNECTION_APPLICATION_NAME_PROP);
    } else if (containsKey(CLIENT_APPLICATION_NAME_PROP)) {
      applicationName = getString(CLIENT_APPLICATION_NAME_PROP);
    } else {
      throw new NoSuchElementException(CONNECTION_APPLICATION_NAME_PROP + " was not set");
    }
    verifyApplicationName(applicationName);
    return applicationName;
  }

  /**
   * Verifies application name. Avoids characters that Zookeeper does not like in nodes & Hadoop
   * does not like in HDFS paths.
   *
   * @param name Application name
   * @throws IllegalArgumentException If name contains illegal characters
   */
  private void verifyApplicationName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Application name cannot be null");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Application name length must be > 0");
    }
    String reason = null;
    char[] chars = name.toCharArray();
    char c;
    for (int i = 0; i < chars.length; i++) {
      c = chars[i];
      if (c == 0) {
        reason = "null character not allowed @" + i;
        break;
      } else if (c == '/' || c == '.' || c == ':') {
        reason = "invalid character '" + c + "'";
        break;
      } else if (c > '\u0000' && c <= '\u001f' || c >= '\u007f' && c <= '\u009F'
          || c >= '\ud800' && c <= '\uf8ff' || c >= '\ufff0' && c <= '\uffff') {
        reason = "invalid character @" + i;
        break;
      }
    }
    if (reason != null) {
      throw new IllegalArgumentException(
          "Invalid application name \"" + name + "\" caused by " + reason);
    }
  }

  /**
   * Sets the value of the property {@value #CONNECTION_ZOOKEEPERS_PROP}
   *
   * @param zookeepers The instance to use, must not be null.
   *
   */
  public FluoConfiguration setInstanceZookeepers(String zookeepers) {
    return setNonEmptyString(CONNECTION_ZOOKEEPERS_PROP, zookeepers);
  }

  /**
   * Gets the value of the property {@value #CONNECTION_ZOOKEEPERS_PROP} and if not set returns the
   * default {@value #CONNECTION_ZOOKEEPERS_DEFAULT}
   *
   * @return The zookeeper instance.
   */
  public String getInstanceZookeepers() {
    return getDepNonEmptyString(CONNECTION_ZOOKEEPERS_PROP, CLIENT_ZOOKEEPER_CONNECT_PROP,
        CONNECTION_ZOOKEEPERS_DEFAULT);
  }

  /**
   * Returns the zookeeper application name string.
   *
   * @return The zookeeper application string.
   */
  public String getAppZookeepers() {
    return getInstanceZookeepers() + "/" + getApplicationName();
  }

  /**
   * Sets the value of the property {@value #CONNECTION_ZOOKEEPER_TIMEOUT_PROP}
   *
   * @param timeout This must be a positive integer
   */
  public FluoConfiguration setZookeeperTimeout(int timeout) {
    return setPositiveInt(CONNECTION_ZOOKEEPER_TIMEOUT_PROP, timeout);
  }

  /**
   * Gets the value of the property {@value #CONNECTION_ZOOKEEPER_TIMEOUT_PROP} and if not set
   * returns the default {@value #CONNECTION_ZOOKEEPER_TIMEOUT_DEFAULT}
   */
  public int getZookeeperTimeout() {
    return getDepPositiveInt(CONNECTION_ZOOKEEPER_TIMEOUT_PROP, CLIENT_ZOOKEEPER_TIMEOUT_PROP,
        CONNECTION_ZOOKEEPER_TIMEOUT_DEFAULT);
  }

  /**
   * Get the secret configured to access data in zookeeper. If the secret is an empty string, then
   * nothing in zookeeper is locked down.
   *
   * <p>
   * Gets the value of the property {@value #CONNECTION_ZOOKEEPER_SECRET}
   *
   * @since 1.2.0
   */
  public String getZookeeperSecret() {
    return getString(CONNECTION_ZOOKEEPER_SECRET, "");
  }

  /**
   * Setting this before initializing an application will cause Fluo to lock down Zookeeper such
   * that this secret is required to read data from zookeeper. If set to an empty string, then
   * nothing in zookeeper will be locked down. This property defaults to an empty string.
   *
   * <p>
   * Sets the value of the property {@value #CONNECTION_ZOOKEEPER_SECRET}
   *
   * @since 1.2.0
   */
  public void setZookeeperSecret(String secret) {
    setProperty(CONNECTION_ZOOKEEPER_SECRET, verifyNotNull(CONNECTION_ZOOKEEPER_SECRET, secret));
  }

  @Deprecated
  public FluoConfiguration setClientRetryTimeout(int timeoutMs) {
    return setConnectionRetryTimeout(timeoutMs);
  }

  @Deprecated
  public int getClientRetryTimeout() {
    return getConnectionRetryTimeout();
  }

  /**
   * Sets the connection retry timeout property {@value #CONNECTION_RETRY_TIMEOUT_MS_PROP} in
   * milliseconds. Must be positive.
   *
   * @since 1.2.0
   */
  public FluoConfiguration setConnectionRetryTimeout(int timeoutMS) {
    Preconditions.checkArgument(timeoutMS >= -1,
        CONNECTION_RETRY_TIMEOUT_MS_PROP + " must be >= -1");
    setProperty(CONNECTION_RETRY_TIMEOUT_MS_PROP, timeoutMS);
    return this;
  }

  /**
   * Returns the value of the property {@value #CONNECTION_RETRY_TIMEOUT_MS_PROP} if it is set, else
   * the default value of {@value #CONNECTION_RETRY_TIMEOUT_MS_DEFAULT}. The integer returned
   * represents milliseconds and is always positive.
   *
   * @since 1.2.0
   */
  public int getConnectionRetryTimeout() {
    int retval;
    if (containsKey(CONNECTION_RETRY_TIMEOUT_MS_PROP)) {
      retval = getInt(CONNECTION_RETRY_TIMEOUT_MS_PROP, CONNECTION_RETRY_TIMEOUT_MS_DEFAULT);
    } else {
      retval = getInt(CLIENT_RETRY_TIMEOUT_MS_PROP, CONNECTION_RETRY_TIMEOUT_MS_DEFAULT);
    }
    Preconditions.checkArgument(retval >= -1, CONNECTION_RETRY_TIMEOUT_MS_PROP + " must be >= -1");
    return retval;
  }

  /**
   * Sets the Apache Accumulo instance property {@value #ACCUMULO_INSTANCE_PROP}
   *
   * @param accumuloInstance The instance to connect to, must not be empty
   */
  public FluoConfiguration setAccumuloInstance(String accumuloInstance) {
    return setNonEmptyString(ACCUMULO_INSTANCE_PROP, accumuloInstance);
  }

  /**
   * Gets the Apache Accumulo instance property value {@value #ACCUMULO_INSTANCE_PROP}
   */
  public String getAccumuloInstance() {
    return getDepNonEmptyString(ACCUMULO_INSTANCE_PROP, CLIENT_ACCUMULO_INSTANCE_PROP);
  }

  /**
   * Sets the value of the property {@value #ACCUMULO_USER_PROP}
   *
   * @param accumuloUser The user name to use, must not be null.
   */
  public FluoConfiguration setAccumuloUser(String accumuloUser) {
    return setNonEmptyString(ACCUMULO_USER_PROP, accumuloUser);
  }

  /**
   * Gets the value of the property {@value #ACCUMULO_USER_PROP}
   */
  public String getAccumuloUser() {
    return getDepNonEmptyString(ACCUMULO_USER_PROP, CLIENT_ACCUMULO_USER_PROP);
  }


  /**
   * Sets the Apache Accumulo password property {@value #ACCUMULO_PASSWORD_PROP}
   *
   * @param accumuloPassword The password to use, must not be null.
   */
  public FluoConfiguration setAccumuloPassword(String accumuloPassword) {
    setProperty(ACCUMULO_PASSWORD_PROP, verifyNotNull(ACCUMULO_PASSWORD_PROP, accumuloPassword));
    return this;
  }

  /**
   * Gets the Apache Accumulo password property value {@value #ACCUMULO_PASSWORD_PROP}
   *
   * @throws NoSuchElementException if {@value #ACCUMULO_PASSWORD_PROP} is not set
   */
  public String getAccumuloPassword() {
    if (containsKey(ACCUMULO_PASSWORD_PROP)) {
      return verifyNotNull(ACCUMULO_PASSWORD_PROP, getString(ACCUMULO_PASSWORD_PROP));
    } else if (containsKey(CLIENT_ACCUMULO_PASSWORD_PROP)) {
      return verifyNotNull(CLIENT_ACCUMULO_PASSWORD_PROP, getString(CLIENT_ACCUMULO_PASSWORD_PROP));
    }
    throw new NoSuchElementException(ACCUMULO_PASSWORD_PROP + " is not set!");
  }

  /**
   * @since 2.0.0
   */
  public FluoConfiguration setAccumuloAuthorizations(String... auths) {
    setProperties(ACCUMULO_AUTH_PROP, auths);
    return this;
  }

  /**
   * @since 2.0.0
   */
  public String[] getAccumuloAuthorizations() {
    if (containsKey(ACCUMULO_AUTH_PROP)) {
      return this.getProperties(ACCUMULO_AUTH_PROP);
    } else {
      return new String[0];
    }
  }

  /**
   * Sets the value of the property {@value #ACCUMULO_ZOOKEEPERS_PROP}
   *
   * @param zookeepers Must not be null
   */
  public FluoConfiguration setAccumuloZookeepers(String zookeepers) {
    return setNonEmptyString(ACCUMULO_ZOOKEEPERS_PROP, zookeepers);
  }

  /**
   * Gets the value of the property {@value #ACCUMULO_ZOOKEEPERS_PROP} if it is set, else returns
   * the value of the property {@value #ACCUMULO_ZOOKEEPERS_DEFAULT}
   */
  public String getAccumuloZookeepers() {
    return getDepNonEmptyString(ACCUMULO_ZOOKEEPERS_PROP, CLIENT_ACCUMULO_ZOOKEEPERS_PROP,
        ACCUMULO_ZOOKEEPERS_DEFAULT);
  }

  /**
   * Sets Accumulo table. This property only needs to be set for FluoAdmin as it will be stored and
   * retrieved from Zookeeper for clients.
   * <p>
   * Sets the value of the property {@value #ACCUMULO_TABLE_PROP}
   */
  public FluoConfiguration setAccumuloTable(String table) {
    return setNonEmptyString(ACCUMULO_TABLE_PROP, table);
  }

  /**
   * Gets the value of the property {@value #ACCUMULO_TABLE_PROP}
   */
  public String getAccumuloTable() {
    return getDepNonEmptyString(ACCUMULO_TABLE_PROP, ADMIN_ACCUMULO_TABLE_PROP);
  }

  /**
   * Sets paths to jars to provide to Accumulo. If not set, Fluo will find jars on classpath
   * <p>
   * Sets the value of the property {@value #ACCUMULO_JARS_PROP}
   *
   * @param path CSV list of paths, must not be null
   * @since 1.2.0
   */
  public FluoConfiguration setAccumuloJars(String path) {
    setProperty(ACCUMULO_JARS_PROP, verifyNotNull(ACCUMULO_JARS_PROP, path));
    return this;
  }

  /**
   * Gets CSV list of jar paths to provide to Accumulo
   * <p>
   * Gets the value of the property {@value #ACCUMULO_JARS_PROP} if set,
   * {@value #ACCUMULO_JARS_DEFAULT} else
   *
   * @since 1.2.0
   */
  public String getAccumuloJars() {
    return getString(ACCUMULO_JARS_PROP, ACCUMULO_JARS_DEFAULT);
  }

  /**
   * Sets the root for the Hadoop DFS value in property {@value #DFS_ROOT_PROP}
   *
   * @param dfsRoot The path for the dfs root eg: hdfs://host:port/path note: may not be empty.
   * @since 1.2.0
   */
  public FluoConfiguration setDfsRoot(String dfsRoot) {
    return setNonEmptyString(DFS_ROOT_PROP, dfsRoot);
  }

  /**
   * Gets the value of property {@value #DFS_ROOT_PROP} if set, otherwise gets the default
   * {@value #DFS_ROOT_DEFAULT}
   *
   * @since 1.2.0
   */
  public String getDfsRoot() {
    return getString(DFS_ROOT_PROP, DFS_ROOT_DEFAULT);
  }

  /**
   * Sets the number of worker threads, must be positive. The default is
   * {@value #WORKER_NUM_THREADS_DEFAULT} threads. Sets this value in the property
   * {@value #WORKER_NUM_THREADS_PROP}
   *
   * @param numThreads The number of threads to use, must be positive
   */
  public FluoConfiguration setWorkerThreads(int numThreads) {
    return setPositiveInt(WORKER_NUM_THREADS_PROP, numThreads);
  }

  /**
   * Gets the value of the property {@value #WORKER_NUM_THREADS_PROP} if set otherwise returns
   * {@value #WORKER_NUM_THREADS_DEFAULT}
   *
   * @return The number of worker threads being used.
   */
  public int getWorkerThreads() {
    return getPositiveInt(WORKER_NUM_THREADS_PROP, WORKER_NUM_THREADS_DEFAULT);
  }

  /**
   * Sets the number of threads used to execute parallel scans, must be positive. The default is
   * {@value #NUM_PARALLEL_SCAN_THREADS_DEFAULT} thread. Sets this value in the property
   * {@value #PARALLEL_SCAN_THREADS_PROP}
   *
   * @param numThreads The number of threads to use, must be positive
   * @since 2.0.0
   */
  public FluoConfiguration setNumParallelScanThreads(int numThreads) {
    return setPositiveInt(PARALLEL_SCAN_THREADS_PROP, numThreads);
  }

  /**
   * Gets the value of the property {@value #PARALLEL_SCAN_THREADS_PROP} if set otherwise returns
   * {@value #NUM_PARALLEL_SCAN_THREADS_DEFAULT}
   *
   * @return The number of threads used for parallel scans.
   * @since 2.0.0
   */
  public int getNumParallelScanThreads() {
    return getPositiveInt(PARALLEL_SCAN_THREADS_PROP, NUM_PARALLEL_SCAN_THREADS_DEFAULT);
  }

  /**
   * @deprecated since 1.1.0. Replaced by {@link #setObserverProvider(String)} and
   *             {@link #getObserverProvider()}
   */
  @Deprecated
  public List<ObserverSpecification> getObserverSpecifications() {

    List<ObserverSpecification> configList = new ArrayList<>();
    Iterator<String> iter = getKeys();

    while (iter.hasNext()) {
      String key = iter.next();
      if (key.startsWith(FluoConfiguration.OBSERVER_PREFIX)
          && !key.equals(FluoConfiguration.OBSERVER_PROVIDER)
          && !key.equals(FluoConfiguration.OBSERVER_INIT_DIR_PROP)
          && !key.equals(FluoConfiguration.OBSERVER_JARS_URL_PROP)) {
        String value = getString(key).trim();

        if (value.isEmpty()) {
          throw new IllegalArgumentException(key + " is set to empty value");
        }

        String[] fields = value.split(",");
        if (fields.length == 0) {
          throw new IllegalArgumentException(key + " has bad value: " + value);
        }

        String className = fields[0];
        if (className.isEmpty()) {
          throw new IllegalArgumentException(key + " has empty class name: " + className);
        }

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < fields.length; i++) {
          String[] kv = fields[i].split("=");
          if (kv.length != 2) {
            throw new IllegalArgumentException(key
                + " has invalid param. Expected 'key=value' but encountered '" + fields[i] + "'");
          }
          if (kv[0].isEmpty() || kv[1].isEmpty()) {
            throw new IllegalArgumentException(
                key + " has empty key or value in param: " + fields[i]);
          }
          params.put(kv[0], kv[1]);
        }

        ObserverSpecification observerSpecification = new ObserverSpecification(className, params);
        configList.add(observerSpecification);
      }
    }
    return configList;
  }

  private int getNextObserverId() {
    Iterator<String> iter1 = getKeys(OBSERVER_PREFIX.substring(0, OBSERVER_PREFIX.length() - 1));
    int max = -1;
    while (iter1.hasNext()) {
      String key = iter1.next();
      String suffix = key.substring(OBSERVER_PREFIX.length());
      if (suffix.matches("\\d+")) {
        try {
          max = Math.max(max, Integer.parseInt(suffix));
        } catch (NumberFormatException e) {
          // not a number so ignore it... will not conflict with the number used later
        }
      }
    }

    return max + 1;
  }

  /**
   * Configure the observer provider that Fluo workers will use.
   * <p>
   * Sets the property of {@value #OBSERVER_PROVIDER}
   *
   * @since 1.1.0
   *
   * @param className Name of a class that implements {@link ObserverProvider}. Must be non-null and
   *        non-empty.
   */
  public void setObserverProvider(String className) {
    setNonEmptyString(OBSERVER_PROVIDER, className);
  }

  /**
   * Calls {@link #setObserverProvider(String)} with the class name.
   *
   * @since 1.1.0
   */
  public void setObserverProvider(Class<? extends ObserverProvider> clazz) {
    setObserverProvider(clazz.getName());
  }

  /**
   * Sets directory where observers jars can found for initialization
   * <p>
   * Sets the value of the property {@value #OBSERVER_INIT_DIR_PROP}
   *
   * @param observerDir Path to directory, must not be null
   * @since 1.2.0
   */
  public FluoConfiguration setObserverInitDir(String observerDir) {
    setProperty(OBSERVER_INIT_DIR_PROP, verifyNotNull(OBSERVER_INIT_DIR_PROP, observerDir));
    return this;
  }

  /**
   * Gets directory where observer jars can be found for initialization
   * <p>
   * Gets the value of the property {@value #OBSERVER_INIT_DIR_PROP} if set,
   * {@value #OBSERVER_INIT_DIR_DEFAULT} otherwise
   *
   * @return Path to directory
   * @since 1.2.0
   */
  public String getObserverInitDir() {
    return getString(OBSERVER_INIT_DIR_PROP, OBSERVER_INIT_DIR_DEFAULT);
  }

  /**
   * Sets URL to directory where observer jars can be found
   * <p>
   * Sets the value of the property {@value #OBSERVER_JARS_URL_PROP}
   *
   * @param observerJarsUrl URL to observer jars directory, must not be null
   * @since 1.2.0
   */
  public FluoConfiguration setObserverJarsUrl(String observerJarsUrl) {
    setProperty(OBSERVER_JARS_URL_PROP, verifyNotNull(OBSERVER_JARS_URL_PROP, observerJarsUrl));
    return this;
  }

  /**
   * Gets the directory where observer jars can be found
   * <p>
   * Gets the value of the property {@value #OBSERVER_JARS_URL_PROP} if set,
   * {@value #OBSERVER_JARS_URL_DEFAULT} otherwise
   *
   * @since 1.2.0
   */
  public String getObserverJarsUrl() {
    return getString(OBSERVER_JARS_URL_PROP, OBSERVER_JARS_URL_DEFAULT);
  }

  /**
   * @return The configured {@link ObserverProvider} class name. If one was not configured, returns
   *         {@value #OBSERVER_PROVIDER_DEFAULT}
   * @since 1.1.0
   */
  public String getObserverProvider() {
    return getString(OBSERVER_PROVIDER, OBSERVER_PROVIDER_DEFAULT);
  }

  @Deprecated
  private void addObserver(ObserverSpecification oconf, int next) {
    Map<String, String> params = oconf.getConfiguration().toMap();
    StringBuilder paramString = new StringBuilder();
    for (java.util.Map.Entry<String, String> pentry : params.entrySet()) {
      paramString.append(',');
      paramString.append(pentry.getKey());
      paramString.append('=');
      paramString.append(pentry.getValue());
    }
    setProperty(OBSERVER_PREFIX + "" + next, oconf.getClassName() + paramString);
  }

  /**
   * Adds an {@link ObserverSpecification} to the configuration using a unique integer prefix that's
   * not currently in use.
   *
   * @deprecated since 1.1.0. Replaced by {@link #setObserverProvider(String)} and
   *             {@link #getObserverProvider()}
   */
  @Deprecated
  public FluoConfiguration addObserver(ObserverSpecification oconf) {
    int next = getNextObserverId();
    addObserver(oconf, next);
    return this;
  }

  /**
   * Adds multiple observers using unique integer prefixes for each.
   *
   * @deprecated since 1.1.0. Replaced by {@link #setObserverProvider(String)} and
   *             {@link #getObserverProvider()}
   */
  @Deprecated
  public FluoConfiguration addObservers(Iterable<ObserverSpecification> observers) {
    int next = getNextObserverId();
    for (ObserverSpecification oconf : observers) {
      addObserver(oconf, next++);
    }
    return this;
  }

  /**
   * Removes any configured observers.
   *
   * @deprecated since 1.1.0. Replaced by {@link #setObserverProvider(String)} and
   *             {@link #getObserverProvider()}
   */
  @Deprecated
  public FluoConfiguration clearObservers() {
    Iterator<String> iter1 = getKeys(OBSERVER_PREFIX.substring(0, OBSERVER_PREFIX.length() - 1));
    while (iter1.hasNext()) {
      String key = iter1.next();
      clearProperty(key);
    }

    return this;
  }

  /**
   * Sets the transaction rollback time, in milliseconds.
   * <p>
   * Sets the value of the property {@value #TRANSACTION_ROLLBACK_TIME_PROP}
   *
   * @param time A long representation of the duration, must be positive
   * @param tu The TimeUnit to use
   */
  public FluoConfiguration setTransactionRollbackTime(long time, TimeUnit tu) {
    return setPositiveLong(TRANSACTION_ROLLBACK_TIME_PROP, tu.toMillis(time));
  }

  /**
   * Gets the transaction rollback time, in milliseconds.
   * <p>
   * Gets the value of the property {@value #TRANSACTION_ROLLBACK_TIME_PROP} if set,
   * {@value #TRANSACTION_ROLLBACK_TIME_DEFAULT} otherwise
   *
   * @return A positive long representation of the rollback time.
   */
  public long getTransactionRollbackTime() {
    return getPositiveLong(TRANSACTION_ROLLBACK_TIME_PROP, TRANSACTION_ROLLBACK_TIME_DEFAULT);
  }

  /**
   * Sets the non negative number of threads each loader runs. If setting to zero, must also set the
   * queue size to zero.
   * <p>
   * Sets the value of the property {@value #LOADER_NUM_THREADS_PROP}
   *
   * @param numThreads Must be positive
   */
  public FluoConfiguration setLoaderThreads(int numThreads) {
    return setNonNegativeInt(LOADER_NUM_THREADS_PROP, numThreads);
  }

  /**
   * Returns the number of threads each loader runs.
   * <p>
   * Gets the value of the property {@value #LOADER_NUM_THREADS_PROP} if set,
   * {@value #LOADER_NUM_THREADS_DEFAULT} otherwise
   *
   * @return The number of threads each loader runs.
   */
  public int getLoaderThreads() {
    return getNonNegativeInt(LOADER_NUM_THREADS_PROP, LOADER_NUM_THREADS_DEFAULT);
  }

  /**
   * Sets the queue size for the loader. This should be set to zero if the number of loader threads
   * is zero.
   * <p>
   * Sets the value of the property {@value #LOADER_QUEUE_SIZE_PROP}
   *
   * @param queueSize The non negative size of the queue.
   */
  public FluoConfiguration setLoaderQueueSize(int queueSize) {
    return setNonNegativeInt(LOADER_QUEUE_SIZE_PROP, queueSize);
  }

  /**
   * Gets the loader queue size.
   * <p>
   * Gets the value of the property {@value #LOADER_QUEUE_SIZE_PROP} if set,
   * {@value #LOADER_QUEUE_SIZE_DEFAULT} otherwise
   *
   * @return the loader queue size.
   */
  public int getLoaderQueueSize() {
    return getNonNegativeInt(LOADER_QUEUE_SIZE_PROP, LOADER_QUEUE_SIZE_DEFAULT);
  }

  /**
   * @param reporter The name of the reporter to get configuration for, i.e. console, jmx, graphite.
   * @return A subset of this configuration using the prefix {@value #REPORTER_PREFIX} with the
   *         reporter parameter appended. Any change made to subset will be reflected in this
   *         configuration, but with the prefix added.
   */
  public SimpleConfiguration getReporterConfiguration(String reporter) {
    return subset(REPORTER_PREFIX + "." + reporter);
  }

  /**
   * @return A subset of this configuration using the prefix {@value #APP_PREFIX}. Any change made
   *         to subset will be reflected in this configuration, but with the prefix added. This
   *         method is useful for setting application configuration before initialization. For
   *         reading application configuration after initialization, see
   *         {@link FluoClient#getAppConfiguration()} and {@link Context#getAppConfiguration()}
   */
  public SimpleConfiguration getAppConfiguration() {
    return subset(APP_PREFIX);
  }

  /**
   * Set the value of the property {@value #MINI_START_ACCUMULO_PROP}
   *
   * @param startAccumulo Flag to mini start Accumulo or not
   */
  public FluoConfiguration setMiniStartAccumulo(boolean startAccumulo) {
    setProperty(MINI_START_ACCUMULO_PROP, startAccumulo);
    return this;
  }

  /**
   * Gets the value of the property {@value #MINI_START_ACCUMULO_PROP} if set, else gets the value
   * of {@value #MINI_START_ACCUMULO_DEFAULT}
   */
  public boolean getMiniStartAccumulo() {
    return getBoolean(MINI_START_ACCUMULO_PROP, MINI_START_ACCUMULO_DEFAULT);
  }

  /**
   * Sets the value of the property {@value #MINI_DATA_DIR_PROP}
   *
   * @param dataDir The path to the directory, must not be null
   */
  public FluoConfiguration setMiniDataDir(String dataDir) {
    return setNonEmptyString(MINI_DATA_DIR_PROP, dataDir);
  }

  /**
   * Gets the value of the property {@value #MINI_DATA_DIR_PROP} if set, otherwise gets the value of
   * the property {@value #MINI_DATA_DIR_DEFAULT}
   */
  public String getMiniDataDir() {
    return getNonEmptyString(MINI_DATA_DIR_PROP, MINI_DATA_DIR_DEFAULT);
  }

  /**
   * Logs all properties
   */
  public void print() {
    Iterator<String> iter = getKeys();
    while (iter.hasNext()) {
      String key = iter.next();
      log.info(key + " = " + getRawString(key));
    }
  }

  private boolean verifyStringPropSet(String... keys) {
    for (String key : keys) {
      if (containsKey(key) && !getString(key).isEmpty()) {
        return true;
      }
    }
    log.info(keys[0] + " is not set");
    return false;
  }

  private boolean verifyStringPropNotSet(String... keys) {
    for (String key : keys) {
      if (containsKey(key) && !getString(key).isEmpty()) {
        log.info(key + " should not be set");
        return false;
      }
    }
    return true;
  }

  /**
   * Verifies that the connection properties are set and and valid.
   *
   * @return A boolean if the requirements have been met.
   */
  public boolean hasRequiredConnectionProps() {
    boolean valid = true;
    valid &= verifyStringPropSet(CONNECTION_APPLICATION_NAME_PROP, CLIENT_APPLICATION_NAME_PROP);
    return valid;
  }

  /**
   * Returns true if required properties for FluoClient are set
   */
  public boolean hasRequiredClientProps() {
    boolean valid = true;
    valid &= verifyStringPropSet(CONNECTION_APPLICATION_NAME_PROP, CLIENT_APPLICATION_NAME_PROP);
    valid &= verifyStringPropSet(ACCUMULO_USER_PROP, CLIENT_ACCUMULO_USER_PROP);
    valid &= verifyStringPropSet(ACCUMULO_PASSWORD_PROP, CLIENT_ACCUMULO_PASSWORD_PROP);
    valid &= verifyStringPropSet(ACCUMULO_INSTANCE_PROP, CLIENT_ACCUMULO_INSTANCE_PROP);
    return valid;
  }

  /**
   * Returns true if required properties for FluoAdmin are set
   */
  public boolean hasRequiredAdminProps() {
    boolean valid = true;
    valid &= hasRequiredClientProps();
    valid &= verifyStringPropSet(ACCUMULO_TABLE_PROP);
    return valid;
  }

  /**
   * Returns true if required properties for Oracle are set
   */
  public boolean hasRequiredOracleProps() {
    boolean valid = true;
    valid &= hasRequiredClientProps();
    return valid;
  }

  /**
   * Returns true if required properties for Worker are set
   */
  public boolean hasRequiredWorkerProps() {
    boolean valid = true;
    valid &= hasRequiredClientProps();
    return valid;
  }

  /**
   * Returns true if required properties for MiniFluo are set
   */
  public boolean hasRequiredMiniFluoProps() {
    boolean valid = true;
    if (getMiniStartAccumulo()) {
      // ensure that client properties are not set since we are using MiniAccumulo
      valid &= verifyStringPropNotSet(ACCUMULO_USER_PROP, CLIENT_ACCUMULO_USER_PROP);
      valid &= verifyStringPropNotSet(ACCUMULO_PASSWORD_PROP, CLIENT_ACCUMULO_PASSWORD_PROP);
      valid &= verifyStringPropNotSet(ACCUMULO_INSTANCE_PROP, CLIENT_ACCUMULO_INSTANCE_PROP);
      valid &= verifyStringPropNotSet(ACCUMULO_ZOOKEEPERS_PROP, CLIENT_ACCUMULO_ZOOKEEPERS_PROP);
      valid &= verifyStringPropNotSet(CONNECTION_ZOOKEEPERS_PROP, CLIENT_ZOOKEEPER_CONNECT_PROP);
      if (valid == false) {
        log.error("Client properties should not be set in your configuration if MiniFluo is "
            + "configured to start its own accumulo (indicated by fluo.mini.start.accumulo being "
            + "set to true)");
      }
    } else {
      valid &= hasRequiredClientProps();
      valid &= hasRequiredAdminProps();
      valid &= hasRequiredOracleProps();
      valid &= hasRequiredWorkerProps();
    }
    return valid;
  }

  /**
   * Returns a SimpleConfiguration clientConfig with properties set from this configuration
   *
   * @return SimpleConfiguration
   */
  public SimpleConfiguration getClientConfiguration() {
    SimpleConfiguration clientConfig = new SimpleConfiguration();
    Iterator<String> iter = getKeys();
    while (iter.hasNext()) {
      String key = iter.next();
      if (key.startsWith(CONNECTION_PREFIX) || key.startsWith(ACCUMULO_PREFIX)
          || key.startsWith(CLIENT_PREFIX)) {
        clientConfig.setProperty(key, getRawString(key));
      }
    }
    return clientConfig;
  }

  /**
   * Returns configuration with all Fluo properties set to their default. NOTE - some properties do
   * not have defaults and will not be set.
   */
  public static SimpleConfiguration getDefaultConfiguration() {
    SimpleConfiguration config = new SimpleConfiguration();
    setDefaultConfiguration(config);
    return config;
  }

  /**
   * Sets all Fluo properties to their default in the given configuration. NOTE - some properties do
   * not have defaults and will not be set.
   */
  public static void setDefaultConfiguration(SimpleConfiguration config) {
    config.setProperty(CONNECTION_ZOOKEEPERS_PROP, CONNECTION_ZOOKEEPERS_DEFAULT);
    config.setProperty(CONNECTION_ZOOKEEPER_TIMEOUT_PROP, CONNECTION_ZOOKEEPER_TIMEOUT_DEFAULT);
    config.setProperty(DFS_ROOT_PROP, DFS_ROOT_DEFAULT);
    config.setProperty(ACCUMULO_ZOOKEEPERS_PROP, ACCUMULO_ZOOKEEPERS_DEFAULT);
    config.setProperty(WORKER_NUM_THREADS_PROP, WORKER_NUM_THREADS_DEFAULT);
    config.setProperty(TRANSACTION_ROLLBACK_TIME_PROP, TRANSACTION_ROLLBACK_TIME_DEFAULT);
    config.setProperty(LOADER_NUM_THREADS_PROP, LOADER_NUM_THREADS_DEFAULT);
    config.setProperty(LOADER_QUEUE_SIZE_PROP, LOADER_QUEUE_SIZE_DEFAULT);
    config.setProperty(MINI_START_ACCUMULO_PROP, MINI_START_ACCUMULO_DEFAULT);
    config.setProperty(MINI_DATA_DIR_PROP, MINI_DATA_DIR_DEFAULT);
  }

  private FluoConfiguration setNonNegativeInt(String property, int value) {
    Preconditions.checkArgument(value >= 0, property + " must be non-negative");
    setProperty(property, value);
    return this;
  }

  private int getNonNegativeInt(String property, int defaultValue) {
    int value = getInt(property, defaultValue);
    Preconditions.checkArgument(value >= 0, property + " must be non-negative");
    return value;
  }

  private FluoConfiguration setPositiveInt(String property, int value) {
    Preconditions.checkArgument(value > 0, property + " must be positive");
    setProperty(property, value);
    return this;
  }

  private int getPositiveInt(String property, int defaultValue) {
    int value = getInt(property, defaultValue);
    Preconditions.checkArgument(value > 0, property + " must be positive");
    return value;
  }

  private int getDepPositiveInt(String property, String depProperty, int defaultValue) {
    if (containsKey(property)) {
      return getInt(property, defaultValue);
    } else {
      return getInt(depProperty, defaultValue);
    }
  }

  private FluoConfiguration setPositiveLong(String property, long value) {
    Preconditions.checkArgument(value > 0, property + " must be positive");
    setProperty(property, value);
    return this;
  }

  private long getPositiveLong(String property, long defaultValue) {
    long value = getLong(property, defaultValue);
    Preconditions.checkArgument(value > 0, property + " must be positive");
    return value;
  }

  private FluoConfiguration setNonEmptyString(String property, String value) {
    Objects.requireNonNull(value, property + " cannot be null");
    Preconditions.checkArgument(!value.isEmpty(), property + " cannot be empty");
    setProperty(property, value);
    return this;
  }

  private String getNonEmptyString(String property, String defaultValue) {
    String value = getString(property, defaultValue);
    Objects.requireNonNull(value, property + " cannot be null");
    Preconditions.checkArgument(!value.isEmpty(), property + " cannot be empty");
    return value;
  }

  private String getNonEmptyString(String property) {
    String value = getString(property);
    Objects.requireNonNull(value, property + " cannot be null");
    Preconditions.checkArgument(!value.isEmpty(), property + " cannot be empty");
    return value;
  }

  private String getDepNonEmptyString(String property, String depProperty, String defaultValue) {
    return containsKey(property) ? getNonEmptyString(property, defaultValue)
        : getNonEmptyString(depProperty, defaultValue);
  }

  private String getDepNonEmptyString(String property, String depProperty) {
    if (containsKey(property)) {
      return getNonEmptyString(property);
    } else if (containsKey(depProperty)) {
      return getNonEmptyString(depProperty);
    } else {
      throw new NoSuchElementException(property + " is not set!");
    }
  }

  private static String verifyNotNull(String property, String value) {
    return Objects.requireNonNull(value, property + " cannot be null");
  }
}
