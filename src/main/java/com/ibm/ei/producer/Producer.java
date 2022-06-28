/*
 * Copyright 2018 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.ei.producer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.common.utils.Exit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer {

  private static final Logger logger = LoggerFactory.getLogger(Producer.class);
  private static final ResourceBundle producerTranslations =
      ResourceBundle.getBundle("MessageBundle", Locale.getDefault());

  private static final Integer DEFAULT_THROUGHPUT = -1;
  private static final Integer DEFAULT_NUMBER_THREADS = 1;
  private static final String DEFAULT_PRODUCER_CONFIG = "producer.config";
  private static final Integer DEFAULT_NUM_RECORDS = 100;

  private static final String TOPIC = "topic";
  private static final String PRODUCER_CONFIG = "producerConfig";
  private static final String THROUGHPUT = "throughput";
  private static final String NUM_THREADS = "numThreads";
  private static final String PAYLOAD_TEMPLATE = "payloadTemplate";
  private static final String NUM_RECORDS = "numRecords";
  private static final String GEN_CONFIG = "genConfig";

  private String topic;
  private Integer numThreads;
  private Integer throughput;
  private String configFilePath;
  private String templateFilePath;
  private Integer numRecords;

  public static void main(String[] args) {
    Producer producer = new Producer();
    ArgumentParser parser = argParser();
    List<ProducerThread> producers = new ArrayList<>();

    Thread gracefulEnd =
        new Thread(
            () -> {
              final Integer totalCount =
                  producers
                      .stream()
                      .map(ProducerThread::messageCount)
                      .collect(Collectors.summingInt(Integer::intValue));
              logger.info(
                  "Sent {} records in total across {} producers", totalCount, producers.size());
            });

    Runtime.getRuntime().addShutdownHook(gracefulEnd);

    try {
      Namespace res = parser.parseArgs(args);

      producer.setTopic(res.getString(TOPIC));
      producer.setThroughput(res.getInt(THROUGHPUT));
      producer.setConfigFilePath(res.getString(PRODUCER_CONFIG));
      producer.setNumThreads(res.getInt(NUM_THREADS));
      producer.setPayloadTemplateFilePath(res.getString(PAYLOAD_TEMPLATE));
      producer.setNumRecords(res.getInt(NUM_RECORDS));
      overrideArgumentsWithEnvVars(producer);

      if (res.getBoolean("genConfig")) {
        try {
          FileUtils.writeStringToFile(
              new File("producer.config"),
              IOUtils.resourceToString("/producer.config.template", null),
              "UTF-8");
          System.out.println(producerTranslations.getString("producer.fileGenerated"));
        } catch (IOException exception) {
          System.err.println(producerTranslations.getString("producer.fileGenerationFail"));
          logger.error(producerTranslations.getString("producer.envar.warning"), exception);
        }

      } else {
        // if one of the required arguments is missing, log and exit.
        if (Objects.isNull(producer.topic)
            || Objects.isNull(producer.configFilePath)
            || Objects.isNull(producer.templateFilePath)) {
          System.out.println(producerTranslations.getString("producer.argsMissing"));
          parser.printHelp();
          Exit.exit(0);
          // check the inputs for the fields we can - if invalid log and exit
        } else if (producer.numThreads < 1) {
          System.out.println(producerTranslations.getString("producer.invalidThreads"));
          parser.printHelp();
          Exit.exit(0);
        } else if (producer.throughput == 0 || producer.throughput < -1) {
          System.out.println(producerTranslations.getString("producer.invalidThroughput"));
          parser.printHelp();
          Exit.exit(0);
        } else {
          ThreadGroup producersGroup = new ThreadGroup("Producers");
          logger.info("Starting {} producers", producer.numThreads);
          for (int i = 0; i < producer.numThreads; i++) {
            PayloadGenerator generator = new PayloadGenerator(producer.getTemplateFilePath());
            ProducerThread producerThread =
                new ProducerThread(
                    producersGroup, String.format("producer%d", i), producer, generator);
            producerThread.start();
            producers.add(producerThread);
          }
        }
      }
    } catch (ArgumentParserException error) {
      error.printStackTrace();
      if (args.length == 0) {
        parser.printHelp();
        Exit.exit(0);
      } else {
        parser.handleError(error);
        Exit.exit(1);
      }
    }
  }

  private static ArgumentParser argParser() {
    ArgumentParser parser =
        ArgumentParsers.newFor("kafka-sample-producer")
            .singleMetavar(true)
            .build()
            .defaultHelp(true)
            .description(producerTranslations.getString("producer.summary"));

    ArgumentGroup propertiesFileSection =
        parser.addArgumentGroup(producerTranslations.getString("producer.configSection"));

    propertiesFileSection
        .addArgument("-g", "--gen-config")
        .action(Arguments.storeTrue())
        .required(false)
        .type(Arguments.booleanType())
        .dest(GEN_CONFIG)
        .help(producerTranslations.getString("producer.genConfig.help"));

    ArgumentGroup requiredParams =
        parser.addArgumentGroup(producerTranslations.getString("producer.requiredConfigSection"));

    requiredParams
        .addArgument("-t", "--topic")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .dest(TOPIC)
        .help(producerTranslations.getString("producer.topic.help"));

    requiredParams
        .addArgument("-c", "--producer-config")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .setDefault(DEFAULT_PRODUCER_CONFIG)
        .dest(PRODUCER_CONFIG)
        .help(producerTranslations.getString("producer.producerConfigFile.help"));

    ArgumentGroup generalConfig =
        parser.addArgumentGroup(producerTranslations.getString("producer.generalConfigSection"));

    generalConfig
        .addArgument("-T", "--throughput")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .dest(THROUGHPUT)
        .setDefault(DEFAULT_THROUGHPUT)
        .help(producerTranslations.getString("producer.throughput.help"));

    generalConfig
        .addArgument("-x", "--num-threads")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .dest(NUM_THREADS)
        .setDefault(DEFAULT_NUMBER_THREADS)
        .help(producerTranslations.getString("producer.numThreads.help"));

    generalConfig
        .addArgument("-n", "--num-records")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .dest(NUM_RECORDS)
        .setDefault(DEFAULT_NUM_RECORDS)
        .help(producerTranslations.getString("producer.numRecords.help"));

    MutuallyExclusiveGroup payloadOptions =
        parser
            .addMutuallyExclusiveGroup()
            .description(producerTranslations.getString("producer.payload.options"));

    payloadOptions
        .addArgument("-f", "--payload-template")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .dest(PAYLOAD_TEMPLATE)
        .help(producerTranslations.getString("producer.payloadTemplate.help"));

    return parser;
  }

  private static void overrideArgumentsWithEnvVars(Producer producer) {
    Map<String, String> env = System.getenv();

    if (env.containsKey("TOPIC")) {
      producer.setTopic(env.get("TOPIC"));
    }
    if (env.containsKey("NUM_THREADS")) {
      producer.setNumThreads(Integer.parseInt(env.get("NUM_THREADS")));
    }
    if (env.containsKey("PRODUCER_CONFIG")) {
      producer.setConfigFilePath(env.get("PRODUCER_CONFIG"));
    }
    if (env.containsKey("THROUGHPUT")) {
      producer.setThroughput(Integer.parseInt(env.get("THROUGHPUT")));
    }
    if (env.containsKey("PAYLOAD_TEMPLATE")) {
      producer.setPayloadTemplateFilePath(env.get("PAYLOAD_TEMPLATE"));
    }
    if (env.containsKey("NUM_RECORDS")) {
      producer.setNumRecords(Integer.parseInt(env.get("NUM_RECORDS")));
    }
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public void setNumThreads(Integer numThreads) {
    this.numThreads = numThreads;
  }

  public void setThroughput(Integer throughput) {
    this.throughput = throughput;
  }

  public void setConfigFilePath(String configFilePath) {
    this.configFilePath = configFilePath;
  }

  public void setPayloadTemplateFilePath(String templateFilePath) {
    this.templateFilePath = templateFilePath;
  }

  private void setNumRecords(Integer numRecords) {
    this.numRecords = numRecords;
  }

  public String getConfigFilePath() {
    return this.configFilePath;
  }

  public String getTopic() {
    return this.topic;
  }

  public String getTemplateFilePath() {
    return this.templateFilePath;
  }

  public int getThroughput() {
    return this.throughput;
  }

  public int getProducerRecordCount() {
    return Math.round(this.numRecords / this.numThreads);
  }
}
