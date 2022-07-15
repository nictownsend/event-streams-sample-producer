package com.ibm.ei.producer.config;

import static com.ibm.ei.utils.Configuration.NUM_PRODUCERS;
import static com.ibm.ei.utils.Configuration.PRODUCER_CONFIG;
import static com.ibm.ei.utils.Configuration.THROUGHPUT;
import static com.ibm.ei.utils.Configuration.TOPIC;

import com.ibm.ei.utils.Configuration;

public class ProducerConfig {

  private String topic;
  private Integer numThreads;
  private Integer throughput;
  private String configFilePath;

  private ProducerConfig() {}

  public static ProducerConfig createProducerConfig(Configuration ns) {
    ProducerConfig config = new ProducerConfig();

    config.setTopic(ns.getString(TOPIC));
    config.setThroughput(ns.getInt(THROUGHPUT));
    config.setConfigFilePath(ns.getString(PRODUCER_CONFIG));
    config.setNumThreads(ns.getInt(NUM_PRODUCERS));

    return config;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Integer getNumThreads() {
    return numThreads;
  }

  public void setNumThreads(Integer numThreads) {
    this.numThreads = numThreads;
  }

  public Integer getThroughput() {
    return throughput;
  }

  public void setThroughput(Integer throughput) {
    this.throughput = throughput;
  }

  public String getConfigFilePath() {
    return configFilePath;
  }

  public void setConfigFilePath(String configFilePath) {
    this.configFilePath = configFilePath;
  }
}
