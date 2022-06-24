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

import com.ibm.ei.producer.config.PayloadConfig;
import com.ibm.ei.producer.config.ProducerConfig;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerThread extends Thread {

  private final BlockingQueue<String> messageQueue;
  private final ProducerConfig producerConfig;
  private final PayloadConfig payloadConfig;
  private AtomicInteger numberRecordsSent = new AtomicInteger(0);

  private static final Logger logger = LoggerFactory.getLogger(ProducerThread.class);
  private AtomicBoolean running = new AtomicBoolean(false);

  ProducerThread(
      ThreadGroup threadGroup,
      String threadName,
      ProducerConfig producerConfig,
      PayloadConfig payloadConfig,
      BlockingQueue<String> messages) {
    super(threadGroup, threadName);
    this.messageQueue = messages;
    this.producerConfig = producerConfig;
    this.payloadConfig = payloadConfig;
  }

  @Override
  public void run() {
    running.set(true);
    try {
      Properties props = Utils.loadProps(this.producerConfig.getConfigFilePath());
      final KafkaProducer<String, String> producer = new KafkaProducer<>(props);
      int throughput = this.producerConfig.getThroughput();

      while (running.get()) {
        final int recordCount = this.numberRecordsSent.addAndGet(1);
        if (recordCount > payloadConfig.getNumRecords() / producerConfig.getNumThreads()) {
          running.set(false);
          continue;
        }

        final String payload = this.messageQueue.poll();
        if (Objects.isNull(payload)) {
          running.set(false);
          continue;
        }

        producer.send(new ProducerRecord<>(this.producerConfig.getTopic(), payload));

        if (throughput > 0) {
          int pause = Math.round(1000 / throughput);
          try {
            Thread.sleep(pause);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    } catch (IOException e) {
      logger.error("Failed to send record", e);
    }

    logger.info("Sent {} records", this.numberRecordsSent);
  }

  @Override
  public void start() {
    logger.info("Started producer thread");
    super.start();
  }

  public int messageCount() {
    return this.numberRecordsSent.get();
  }

  @Override
  public void interrupt() {
    running.set(false);
    super.interrupt();
  }
}
