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

import static com.ibm.ei.utils.Configuration.BATCH_MODE;
import static com.ibm.ei.utils.Configuration.GEN_CONFIG;
import static com.ibm.ei.utils.Configuration.OUTPUT_PATH;
import static com.ibm.ei.utils.Configuration.PRODUCER_CONFIG;
import static com.ibm.ei.utils.Configuration.RUNTIME_MODE;

import com.ibm.ei.producer.config.PayloadConfig;
import com.ibm.ei.producer.config.ProducerConfig;
import com.ibm.ei.utils.Configuration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {

  private static final Logger logger = LoggerFactory.getLogger(Runner.class);
  private static final ResourceBundle translations =
      ResourceBundle.getBundle("MessageBundle", Locale.getDefault());

  public static void main(String[] args) throws IOException {

    List<ProducerThread> producers = new ArrayList<>();

    try {
      Configuration runtimeArgs = new Configuration(args);
      runtimeArgs.validate();

      ProducerConfig producerConfig = ProducerConfig.createProducerConfig(runtimeArgs);
      PayloadConfig payloadConfig = PayloadConfig.createPayloadConfig(runtimeArgs);

      boolean isBatch = runtimeArgs.getString(RUNTIME_MODE).equals(BATCH_MODE);

      String batchOutputPath = runtimeArgs.getString(OUTPUT_PATH);

      if (runtimeArgs.getBoolean(GEN_CONFIG)) {
        try {
          FileUtils.writeStringToFile(
              new File(runtimeArgs.getString(PRODUCER_CONFIG)),
              IOUtils.resourceToString("/producer.config.template", null),
              "UTF-8");
          logger.info(translations.getString("runner.fileGenerated"));
        } catch (IOException exception) {
          logger.error(translations.getString("runner.fileGenerationFail"), exception);
        }
        System.exit(0);
      }

      PayloadGenerator generator = new PayloadGenerator(payloadConfig);
      logger.info("Generating messages");
      BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

      for (int i = 0; i < payloadConfig.getNumRecords(); i++) {
        String generated = generator.generatePayload();
        String flattened = new JSONObject(generated).toString();
        messageQueue.add(flattened);
        if (i % (payloadConfig.getNumRecords() / 10) == 0) {
          String progress =
              Double.valueOf(
                          (Integer.valueOf(i).floatValue() / payloadConfig.getNumRecords()) * 100)
                      .intValue()
                  + "%";
          logger.info(progress);
        }
      }

      if (isBatch) {
        try {
          final File output = new File(batchOutputPath);
          FileUtils.writeStringToFile(
              output,
              messageQueue.stream().collect(Collectors.joining(System.lineSeparator())),
              "UTF-8");
          logger.info(
              translations.getString("runner.outputGenerated"),
              messageQueue.size(),
              output.getAbsolutePath());
        } catch (IOException exception) {
          logger.error(translations.getString("runner.outputGenerationFail"), exception);
        }
        System.exit(0);
      }

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
      logger.warn(error.getLocalizedMessage());
      error.getParser().printHelp();
      if (args.length == 0) {
        System.exit(0);
      } else {
        System.exit(1);
      }
    }
  }
}
