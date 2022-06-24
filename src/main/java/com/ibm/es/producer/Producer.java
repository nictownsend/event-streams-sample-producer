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
package com.ibm.es.producer;

import java.io.File;
import java.io.IOException;
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

  private String topic;
  private Integer numThreads;
  private Integer throughput;
  private String configFilePath;
  private String templateFilePath;

  public static void main(String[] args) {
    Producer producer = new Producer();
    ArgumentParser parser = argParser();
    List<ProducerThread> producers = List.of();

    Thread gracefulEnd =
        new Thread(
            () -> {
              final Integer totalCount = producers
                      .stream()
                      .map(
                              t -> {
                                logger.info("{} : sent {} messages", t.getName(), t.messageCount());
                                return t.messageCount();
                              })
                      .collect(Collectors.summingInt(Integer::intValue));
              logger.info("Sent {} total messages", totalCount);
            });

    Runtime.getRuntime().addShutdownHook(gracefulEnd);

    try {
      Namespace res = parser.parseArgs(args);

      producer.setTopic(res.getString("topic"));
      producer.setThroughput(res.getInt("throughput"));
      producer.setConfigFilePath(res.getString("producerConfigFile"));
      producer.setNumThreads(res.getInt("numThreads"));
      producer.setTemplateFilePath(res.getString("payloadTemplate"));
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
        ArgumentParsers.newFor("es-producer")
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
        .dest("genConfig")
        .help(producerTranslations.getString("producer.genConfig.help"));

    ArgumentGroup requiredParams =
        parser.addArgumentGroup(producerTranslations.getString("producer.requiredConfigSection"));

    requiredParams
        .addArgument("-t", "--topic")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("TOPIC")
        .help(producerTranslations.getString("producer.topic.help"));

    requiredParams
        .addArgument("-c", "--producer-config")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("CONFIG-FILE")
        .dest("producerConfigFile")
        .help(producerTranslations.getString("producer.producerConfigFile.help"));

    ArgumentGroup generalConfig =
        parser.addArgumentGroup(producerTranslations.getString("producer.generalConfigSection"));

    generalConfig
        .addArgument("-T", "--throughput")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .metavar("THROUGHPUT")
        .setDefault(DEFAULT_THROUGHPUT)
        .help(producerTranslations.getString("producer.throughput.help"));

    generalConfig
        .addArgument("-x", "--num-threads")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .metavar("NUM_THREADS")
        .dest("numThreads")
        .setDefault(DEFAULT_NUMBER_THREADS)
        .help(producerTranslations.getString("producer.numThreads.help"));

    MutuallyExclusiveGroup payloadOptions =
        parser
            .addMutuallyExclusiveGroup()
            .description(producerTranslations.getString("producer.payload.options"));

    payloadOptions
        .addArgument("-f", "--payload-template-file")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("PAYLOAD-TEMPLATE")
        .dest("payloadTemplate")
        .help(producerTranslations.getString("producer.payloadTemplate.help"));

    return parser;
  }

  private static void overrideArgumentsWithEnvVars(Producer producer) {
    Map<String, String> env = System.getenv();

    if (env.containsKey("ES_TOPIC")) {
      producer.setTopic(env.get("ES_TOPIC"));
    }
    if (env.containsKey("ES_NUM_THREADS")) {
      producer.setNumThreads(Integer.parseInt(env.get("ES_NUM_THREADS")));
    }
    if (env.containsKey("ES_PRODUCER_CONFIG")) {
      producer.setConfigFilePath(env.get("ES_PRODUCER_CONFIG"));
    }
    if (env.containsKey("ES_THROUGHPUT")) {
      producer.setThroughput(Integer.parseInt(env.get("ES_THROUGHPUT")));
    }
    if (env.containsKey("ES_TEMPLATE_FILE_PATH")) {
      producer.setTemplateFilePath(env.get("ES_TEMPLATE_FILE_PATH"));
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

  public void setTemplateFilePath(String templateFilePath) {
    this.templateFilePath = templateFilePath;
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
}
