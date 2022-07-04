package com.ibm.ei.producer.config;

import net.sourceforge.argparse4j.inf.Namespace;

import java.util.Map;

public class ProducerConfig {

  private String topic;
  private Integer numThreads;
  private Integer throughput;
  private String configFilePath;

  public static final String TOPIC = "topic";
  public static final String PRODUCER_CONFIG = "producerConfig";
  public static final String THROUGHPUT = "throughput";
  public static final String NUM_THREADS = "numThreads";

  private ProducerConfig() {}

  public static ProducerConfig createProducerConfig(Namespace ns) {
    ProducerConfig config = new ProducerConfig();

    config.setTopic(ns.getString(TOPIC));
    config.setThroughput(ns.getInt(THROUGHPUT));
    config.setConfigFilePath(ns.getString(PRODUCER_CONFIG));
    config.setNumThreads(ns.getInt(NUM_THREADS));

    return config;
  }

  public ProducerConfig overrideWithEnvVars(Map<String, String> env) {
    if (env.containsKey("TOPIC")) {
      setTopic(env.get("TOPIC"));
    }
    if (env.containsKey("NUM_THREADS")) {
      setNumThreads(Integer.parseInt(env.get("NUM_THREADS")));
    }
    if (env.containsKey("PRODUCER_CONFIG")) {
      setConfigFilePath(env.get("PRODUCER_CONFIG"));
    }
    if (env.containsKey("THROUGHPUT")) {
      setThroughput(Integer.parseInt(env.get("THROUGHPUT")));
    }

    return this;
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
