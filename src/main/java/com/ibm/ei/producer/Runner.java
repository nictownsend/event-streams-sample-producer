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

import static com.ibm.ei.utils.CLIArguments.GEN_CONFIG;

import com.ibm.ei.producer.config.PayloadConfig;
import com.ibm.ei.producer.config.ProducerConfig;
import com.ibm.ei.utils.CLIArguments;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {

  private static final Logger logger = LoggerFactory.getLogger(Runner.class);
  private static final ResourceBundle translations =
      ResourceBundle.getBundle("MessageBundle", Locale.getDefault());

  public static void main(String[] args) throws IOException, ParseException {

    ArgumentParser parser = CLIArguments.argParser();
    List<ProducerThread> producers = new ArrayList<>();

    Thread gracefulEnd =
        new Thread(
            () -> {
              final Integer totalCount =
                  producers
                      .stream()
                      .map(ProducerThread::messageCount)
                      .mapToInt(Integer::intValue)
                      .sum();
              logger.info(
                  "Sent {} records in total across {} producers", totalCount, producers.size());
            });

    Runtime.getRuntime().addShutdownHook(gracefulEnd);

    try {
      Namespace ns = parser.parseArgs(args);

      ProducerConfig producerConfig = ProducerConfig.createProducerConfig(ns);
      producerConfig.overrideWithEnvVars(System.getenv());

      PayloadConfig payloadConfig = PayloadConfig.createPayloadConfig(ns);
      payloadConfig.overrideWithEnvVars(System.getenv());

      boolean isBatch =
          Optional.ofNullable(System.getenv().get("BATCH"))
              .map(b -> Boolean.valueOf(b))
              .orElseGet(ns.get("BATCH"));

      if (ns.getBoolean(GEN_CONFIG)) {
        try {
          FileUtils.writeStringToFile(
              new File("runner.config"),
              IOUtils.resourceToString("/producer.config.template", null),
              "UTF-8");
          logger.info(translations.getString("runner.fileGenerated"));
        } catch (IOException exception) {
          logger.error(translations.getString("runner.fileGenerationFail"), exception);
        }
        System.exit(0);
      }

      // if one of the required arguments is missing, log and exit.
      if ((!isBatch
              && (Objects.isNull(producerConfig.getTopic())
                  || Objects.isNull(producerConfig.getConfigFilePath())))
          || Objects.isNull(payloadConfig.getTemplateFilePath())) {
        System.out.println(translations.getString("runner.argsMissing"));
        parser.printHelp();
        System.exit(0);
      }

      if (producerConfig.getNumThreads() < 1) {
        System.out.println(translations.getString("runner.invalidThreads"));
        parser.printHelp();
        System.exit(0);
      }

      if (producerConfig.getThroughput() == 0 || producerConfig.getThroughput() < -1) {
        System.out.println(translations.getString("runner.invalidThroughput"));
        parser.printHelp();
        System.exit(0);
      }

      PayloadGenerator generator = new PayloadGenerator(payloadConfig);
      logger.info("Generating messages");
      BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

      for (int i = 0; i < payloadConfig.getNumRecords(); i++) {
        String generated = generator.generatePayload();
        String flattened = new JSONObject(generated).toString();
        if (isBatch) {
          System.out.println(flattened);
        } else {
          messageQueue.add(flattened);
        }
        if (i % (payloadConfig.getNumRecords() / 10) == 0) {
          String progress =
              Double.valueOf(
                          (Integer.valueOf(i).floatValue() / payloadConfig.getNumRecords()) * 100)
                      .intValue()
                  + "%";
          if (isBatch) {
            System.err.println(progress);
          }
          logger.info(progress);
        }
      }

      if (isBatch) {
        System.exit(0);
      }

      ThreadGroup producersGroup = new ThreadGroup("Producers");
      logger.info("Starting {} producers to send messages", producerConfig.getNumThreads());
      for (int i = 0; i < producerConfig.getNumThreads(); i++) {
        ProducerThread producerThread =
            new ProducerThread(
                producersGroup,
                String.format("producer%d", i),
                producerConfig,
                payloadConfig,
                messageQueue);
        producerThread.start();
        producers.add(producerThread);
      }
    } catch (ArgumentParserException error) {
      error.printStackTrace();
      if (args.length == 0) {
        parser.printHelp();
        System.exit(0);
      } else {
        parser.handleError(error);
        System.exit(1);
      }
    }
  }
}
