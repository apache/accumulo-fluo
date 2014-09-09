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
package io.fluo.cluster;

import java.io.File;
import java.net.URI;

import com.beust.jcommander.JCommander;
import io.fluo.api.config.FluoConfiguration;
import io.fluo.cluster.util.Logging;
import io.fluo.core.impl.Environment;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.ResourceSpecification;
import org.apache.twill.api.ResourceSpecification.SizeUnit;
import org.apache.twill.api.TwillApplication;
import org.apache.twill.api.TwillController;
import org.apache.twill.api.TwillPreparer;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.api.TwillSpecification;
import org.apache.twill.api.TwillSpecification.Builder.MoreFile;
import org.apache.twill.yarn.YarnTwillRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool to start a distributed Fluo workers in YARN
 */
public class WorkerApp implements TwillApplication {

  private static Logger log = LoggerFactory.getLogger(WorkerApp.class);
  private AppOptions options;
  private FluoConfiguration config;

  public WorkerApp(AppOptions options, FluoConfiguration config) {
    this.options = options;
    this.config = config;
  }

  public TwillSpecification configure() {
    int numInstances = config.getWorkerInstances();
    int maxMemoryMB = config.getWorkerMaxMemory();

    ResourceSpecification workerResources = ResourceSpecification.Builder.with()
        .setVirtualCores(1)
        .setMemory(maxMemoryMB, SizeUnit.MEGA)
        .setInstances(numInstances).build();

    log.info("Starting "+numInstances+" workers with "+maxMemoryMB+"MB of memory");

    MoreFile moreFile = TwillSpecification.Builder.with()
        .setName("FluoWorker").withRunnable()
        .add(new WorkerRunnable(), workerResources)
        .withLocalFiles()
        .add("./conf/fluo.properties", new File(String.format("%s/conf/fluo.properties", options.getFluoHome())));

    File confDir = new File(String.format("%s/conf", options.getFluoHome()));
    for (File f : confDir.listFiles()) {
      if (f.isFile() && (f.getName().equals("fluo.properties") == false)) {
        moreFile = moreFile.add(String.format("./conf/%s", f.getName()), f);
      }
    }

    return moreFile.apply().anyOrder().build();
  }

  public static void main(String[] args) throws ConfigurationException, Exception {

    AppOptions options = new AppOptions();
    JCommander jcommand = new JCommander(options, args);

    if (options.displayHelp()) {
      jcommand.usage();
      System.exit(-1);
    }

    Logging.init("worker", options.getFluoHome() + "/conf", "STDOUT");

    File configFile = new File(options.getFluoHome() + "/conf/fluo.properties");
    FluoConfiguration config = new FluoConfiguration(configFile);
    if (!config.hasRequiredWorkerProps()) {
      log.error("fluo.properties is missing required properties for worker");
      System.exit(-1);
    }
    Environment env = new Environment(config);

    YarnConfiguration yarnConfig = new YarnConfiguration();
    yarnConfig.addResource(new Path(options.getHadoopPrefix()+"/etc/hadoop/core-site.xml"));
    yarnConfig.addResource(new Path(options.getHadoopPrefix()+"/etc/hadoop/yarn-site.xml"));

    TwillRunnerService twillRunner = new YarnTwillRunnerService(yarnConfig, env.getZookeepers());
    twillRunner.startAndWait();

    TwillPreparer preparer = twillRunner.prepare(new WorkerApp(options, config));

    // Add any observer jars found in lib observers
    File observerDir = new File(options.getFluoHome()+"/lib/observers");
    for (File f : observerDir.listFiles()) {
      String jarPath = "file:"+f.getCanonicalPath();
      log.debug("Adding observer jar "+jarPath+" to YARN app");
      preparer.withResources(new URI(jarPath));
    }

    TwillController controller = preparer.start();
    controller.start();

    while (controller.isRunning() == false) {
      Thread.sleep(2000);
    }
    env.close();
    System.exit(0);
  }
}
