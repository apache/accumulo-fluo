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
package io.fluo.core.oracle;

import java.net.InetSocketAddress;

import io.fluo.core.impl.ZookeeperConstants;
import io.fluo.core.impl.Environment;
import io.fluo.core.impl.CuratorCnxnListener;
import io.fluo.core.thrift.OracleService;
import com.google.common.annotations.VisibleForTesting;
import io.fluo.core.util.Halt;
import io.fluo.core.util.HostUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Oracle server is the responsible for providing incrementing logical timestamps to clients. It should never
 * give the same timestamp to two clients and it should always provide an incrementing timestamp.
 * <p/>
 * If multiple oracle servers are run, they will choose a leader and clients will automatically connect
 * to that leader. If the leader goes down, the client will automatically fail over to the next leader.
 * In the case where an oracle fails over, the next oracle will begin a new block of timestamps.
 */
public class OracleServer extends LeaderSelectorListenerAdapter implements OracleService.Iface, PathChildrenCacheListener {

  public static final long ORACLE_MAX_READ_BUFFER_BYTES = 2048;

  private volatile long currentTs = 0;
  private volatile long maxTs = 0;
  private final Environment env;
  private final CuratorCnxnListener cnxnListener;
  private final String tsPath;
  private final String oraclePath;

  private Thread serverThread;
  private THsHaServer server;
  private volatile boolean started = false;

  private LeaderSelector leaderSelector;
  private PathChildrenCache pathChildrenCache;
  private CuratorFramework curatorFramework;
  private Participant currentLeader;

  private volatile boolean isLeader = false;

  private static Logger log = LoggerFactory.getLogger(OracleServer.class);

  public OracleServer(Environment env) throws Exception {
    this.env = env;
    this.cnxnListener = new CuratorCnxnListener();
    this.tsPath = ZookeeperConstants.timestampPath(env.getZookeeperRoot());
    this.oraclePath = ZookeeperConstants.oraclePath(env.getZookeeperRoot());
  }

  private void allocateTimestamp() throws Exception {
    Stat stat = new Stat();
    byte[] d = curatorFramework.getData().storingStatIn(stat).forPath(tsPath);

    // TODO check that d is expected
    // TODO check that still server when setting
    // TODO make num allocated variable... when a server first starts allocate a small amount... the longer it runs and the busier it is, allocate bigger blocks

    long newMax = Long.parseLong(new String(d)) + 1000;

    curatorFramework.setData()
        .withVersion(stat.getVersion())
        .forPath(tsPath, (newMax + "").getBytes("UTF-8"));

    maxTs = newMax;

    if (!isLeader) {
      throw new IllegalStateException();
    }
  }

  @Override
  public synchronized long getTimestamps(String id, int num) throws TException {

    if (!started) {
      throw new IllegalStateException();
    }

    if (!id.equals(env.getFluoInstanceID())) {
      throw new IllegalArgumentException();
    }

    if (!isLeader) {
      throw new IllegalStateException();
    }

    try {
      while (num + currentTs >= maxTs) {
        allocateTimestamp();
      }

      long tmp = currentTs;
      currentTs += num;

      return tmp;
    } catch (Exception e) {
      throw new TException(e);
    }
  }

  @Override public boolean isLeader() throws TException {
    return isLeader;
  }

  @VisibleForTesting
  public boolean isConnected() {
    return (started && cnxnListener.isConnected());
  }

  private InetSocketAddress startServer() throws TTransportException {

    InetSocketAddress addr = new InetSocketAddress(env.getOraclePort());

    TNonblockingServerSocket socket = new TNonblockingServerSocket(addr);

    THsHaServer.Args serverArgs = new THsHaServer.Args(socket);
    TProcessor processor = new OracleService.Processor<OracleService.Iface>(this);
    serverArgs.processor(processor);
    serverArgs.maxReadBufferBytes = ORACLE_MAX_READ_BUFFER_BYTES;
    serverArgs.inputProtocolFactory(new TCompactProtocol.Factory());
    serverArgs.outputProtocolFactory(new TCompactProtocol.Factory());
    server = new THsHaServer(serverArgs);

    Runnable st = new Runnable() {
      @Override
      public void run() {
        server.serve();
      }
    };

    serverThread = new Thread(st);
    serverThread.setDaemon(true);
    serverThread.start();

    return addr;
  }

  public synchronized void start() throws Exception {
    if (started)
      throw new IllegalStateException();

    InetSocketAddress addr = startServer();

    curatorFramework = CuratorFrameworkFactory.newClient(env.getConnector().getInstance().getZooKeepers(), new ExponentialBackoffRetry(1000, 10));
    curatorFramework.getConnectionStateListenable().addListener(cnxnListener);
    curatorFramework.start();

    while (!cnxnListener.isConnected())
      Thread.sleep(200);

    leaderSelector = new LeaderSelector(curatorFramework, env.getZookeeperRoot() + ZookeeperConstants.ORACLE_SERVER, this);
    String leaderId = HostUtil.getHostName() + ":" + addr.getPort();
    leaderSelector.setId(leaderId);
    log.info("Leader ID = " + leaderId);
    leaderSelector.start();

    pathChildrenCache = new PathChildrenCache(curatorFramework, oraclePath, true);
    pathChildrenCache.getListenable().addListener(this);
    pathChildrenCache.start();

    while (!cnxnListener.isConnected()) {
      Thread.sleep(200);
    }

    log.info("Listening " + addr);
    started = true;
  }

  public synchronized void stop() throws Exception {
    if (started) {

      server.stop();
      serverThread.join();

      started = false;

      currentLeader = null;
      if (curatorFramework.getState().equals(CuratorFrameworkState.STARTED)) {
        pathChildrenCache.getListenable().removeListener(this);
        pathChildrenCache.close();
        leaderSelector.close();
        curatorFramework.getConnectionStateListenable().removeListener(this);
        curatorFramework.close();
      }
      log.info("Oracle server has been stopped.");
    }
  }

  private OracleService.Client getOracleClient(String host, int port) {
    try {
      TTransport transport = new TFastFramedTransport(new TSocket(host, port));
      transport.open();
      TProtocol protocol = new TCompactProtocol(transport);
      log.info("Former leader was reachable at " + host + ":" + port);
      return new OracleService.Client(protocol);
    } catch (TTransportException e) {
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  /**
   * Upon an oracle being elected the leader, it will need to adjust its starting timestamp to the last timestamp
   * set in zookeeper.
   *
   * @param curatorFramework
   * @throws Exception
   */
  @Override
  public void takeLeadership(CuratorFramework curatorFramework) throws Exception {

    try {
      // sanity check- make sure previous oracle is no longer listening for connections
      if (currentLeader != null) {
        String[] address = currentLeader.getId().split(":");
        String host = address[0];
        int port = Integer.parseInt(address[1]);

        OracleService.Client client = getOracleClient(host, port);
        if(client != null) {
          try {
            while(client.isLeader()) {
              Thread.sleep(500);
            }
          } catch(Exception e) {
            // TODO: do something here?
          }
        }
      }

      synchronized (this) {
        byte[] d = curatorFramework.getData().forPath(tsPath);
        currentTs = maxTs = Long.parseLong(new String(d));
      }

      isLeader = true;

      while (started) {
        Thread.sleep(100); // if leadership is lost, then curator will interrupt the thread that called this method
      }

    } finally {
      isLeader = false;

      if(started)
        Halt.halt("Oracle has lost leadership unexpectedly and is now halting.");    // if we stopped the server manually, we shouldn't halt
    }
  }

  @Override
  public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {

    try {
      if (isConnected() && (event.equals(PathChildrenCacheEvent.Type.CHILD_ADDED) ||
                            event.equals(PathChildrenCacheEvent.Type.CHILD_REMOVED) ||
                            event.equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) ) {
        synchronized (this) {
          Participant participant = leaderSelector.getLeader();
          if (isLeader(participant) && !leaderSelector.hasLeadership()) {   // in case current instance becomes leader, we want to know who came before it.
            currentLeader = participant;
          }
        }
      }
    } catch(InterruptedException e) {
      log.warn("Oracle leadership watcher has been interrupted unexpectedly");
    }
  }

  private boolean isLeader(Participant participant) {
    return participant != null && participant.isLeader();
  }

}
