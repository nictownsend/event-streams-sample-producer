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

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerThread extends Thread {

  private final PayloadGenerator generator;
  private Thread thread;
  private final String threadName;
  private final Producer producer;

  private static final Logger logger = LoggerFactory.getLogger(ProducerThread.class);

  ProducerThread(
      ThreadGroup threadGroup, String threadName, Producer producer, PayloadGenerator generator) {
    super(threadGroup, threadName);
    this.threadName = threadName;
    this.producer = producer;
    this.generator = generator;
  }

  @Override
  public void run() {
    try {
      Properties props = Utils.loadProps(producer.getConfigFilePath());
      final KafkaProducer<String, String> producer = new KafkaProducer<>(props);
      int throughput = this.producer.getThroughput();
      while (!Thread.interrupted()) {
        producer.send(new ProducerRecord<>(this.producer.getTopic(), generator.generatePayload()));
        if(throughput > 0) {
          int pause = Math.round((60*1000)/throughput);
          this.thread.wait(pause);
        }
      }
    } catch (Exception error) {
      logger.error("Failed to execute", error);
    }
  }

  @Override
  public void start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }
}
