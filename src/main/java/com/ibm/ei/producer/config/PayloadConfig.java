package com.ibm.ei.producer.config;

import static com.ibm.ei.utils.Configuration.NUM_RECORDS;
import static com.ibm.ei.utils.Configuration.PAYLOAD_TEMPLATE;

import com.ibm.ei.utils.Configuration;
import java.text.SimpleDateFormat;

public class PayloadConfig {
  public static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss");
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

  private String templateFilePath;
  private Integer numRecords;

  public static PayloadConfig createPayloadConfig(Configuration ns) {
    PayloadConfig config = new PayloadConfig();

    config.setTemplateFilePath(ns.getString(PAYLOAD_TEMPLATE));
    config.setNumRecords(ns.getInt(NUM_RECORDS));

    return config;
  }

  private PayloadConfig() {}

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
