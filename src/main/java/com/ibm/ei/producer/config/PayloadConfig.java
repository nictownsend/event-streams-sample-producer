package com.ibm.ei.producer.config;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import net.sourceforge.argparse4j.inf.Namespace;

public class PayloadConfig {
  public static final String START_TIMESTAMP = "startTimestamp";
  public static final String END_TIMESTAMP = "endTimestamp";
  public static final String PAYLOAD_TEMPLATE = "payloadTemplate";
  public static final String NUM_RECORDS = "numRecords";
  public static final String TIMESTAMP_INTERVAL = "timestampInterval";
  public static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss");
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

  private String templateFilePath;
  private Integer numRecords;

  private String endTimestamp;
  private long timestampInterval = 1L;
  private String setStartTimestamp;

  public static PayloadConfig createPayloadConfig(Namespace ns) {
    PayloadConfig config = new PayloadConfig();

    config.setTemplateFilePath(ns.getString(PAYLOAD_TEMPLATE));
    Optional.ofNullable(ns.getInt(NUM_RECORDS)).ifPresent(config::setNumRecords);
    Optional.ofNullable(ns.getString(START_TIMESTAMP)).ifPresent(config::setStartTimestamp);
    Optional.ofNullable(ns.getString(END_TIMESTAMP)).ifPresent(config::setEndTimestamp);
    Optional.ofNullable(ns.getLong(TIMESTAMP_INTERVAL)).ifPresent(config::setTimestampInterval);

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

    if (env.containsKey("START_TIMESTAMP")) {
      setStartTimestamp(env.get("START_TIMESTAMP"));
    }

    if (env.containsKey("END_TIMESTAMP")) {
      setEndTimestamp(env.get("END_TIMESTAMP"));
    }

    if (env.containsKey("TIMESTAMP_INTERVAL")) {
      setTimestampInterval(Integer.parseInt(env.get("TIMESTAMP_INTERVAL")));
    }
    return this;
  }

  public String getEndTimestamp() {
    return endTimestamp;
  }

  private void setEndTimestamp(String timestamp) {
    this.endTimestamp = timestamp;
  }

  public long getTimestampInterval() {
    return timestampInterval;
  }

  private void setTimestampInterval(long timestampInterval) {
    this.timestampInterval = timestampInterval;
  }

  public String getStartTimestamp() {
    return setStartTimestamp;
  }

  private void setStartTimestamp(String timestamp) {
    this.setStartTimestamp = timestamp;
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
