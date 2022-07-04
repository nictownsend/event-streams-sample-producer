package com.ibm.ei.producer.config;

import net.sourceforge.argparse4j.inf.Namespace;

import java.util.Map;

public class PayloadConfig {
  public static final String START_TIMESTAMP = "startTimestamp";
  public static final String END_TIMESTAMP = "endTimestamp";
  public static final String PAYLOAD_TEMPLATE = "payloadTemplate";
  public static final String NUM_RECORDS = "numRecords";

  private String templateFilePath;
  private Integer numRecords;


  public static PayloadConfig createPayloadConfig(Namespace ns) {
    PayloadConfig config = new PayloadConfig();

    config.setTemplateFilePath(ns.getString(PAYLOAD_TEMPLATE));
    config.setNumRecords(ns.getInt(NUM_RECORDS));

    return config;
  }

  private PayloadConfig() {}

  public PayloadConfig overrideWithEnvVars(Map<String, String> env) {
    if (env.containsKey("PAYLOAD_TEMPLATE")) {
      setTemplateFilePath(env.get("PAYLOAD_TEMPLATE"));
    }
    if (env.containsKey("NUM_RECORDS")) {
      setNumRecords(Integer.parseInt(env.get("NUM_RECORDS")));
    }
    return this;
  }

  public String getTemplateFilePath() {
    return templateFilePath;
  }

  public void setTemplateFilePath(String templateFilePath) {
    this.templateFilePath = templateFilePath;
  }

  public Integer getNumRecords() {
    return numRecords;
  }

  public void setNumRecords(Integer numRecords) {
    this.numRecords = numRecords;
  }
}
