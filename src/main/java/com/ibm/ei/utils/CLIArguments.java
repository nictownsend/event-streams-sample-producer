package com.ibm.ei.utils;

import static com.ibm.ei.producer.config.PayloadConfig.NUM_RECORDS;
import static com.ibm.ei.producer.config.PayloadConfig.PAYLOAD_TEMPLATE;
import static com.ibm.ei.producer.config.ProducerConfig.NUM_THREADS;
import static com.ibm.ei.producer.config.ProducerConfig.PRODUCER_CONFIG;
import static com.ibm.ei.producer.config.ProducerConfig.THROUGHPUT;
import static com.ibm.ei.producer.config.ProducerConfig.TOPIC;

import com.ibm.ei.producer.config.PayloadConfig;
import java.util.Locale;
import java.util.ResourceBundle;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;

public class CLIArguments {

  private static final String DEFAULT_PRODUCER_CONFIG = "producer.config";
  private static final Integer DEFAULT_THROUGHPUT = -1;
  private static final Integer DEFAULT_NUMBER_THREADS = 1;
  private static final Integer DEFAULT_NUM_RECORDS = 100;

  public static final String BATCH = "batch";
  public static final String GEN_CONFIG = "genConfig";

  private static final ResourceBundle translations =
      ResourceBundle.getBundle("MessageBundle", Locale.getDefault());

  public static ArgumentParser argParser() {
    ArgumentParser parser =
        ArgumentParsers.newFor("kafka-sample-producer")
            .singleMetavar(true)
            .build()
            .defaultHelp(true)
            .description(translations.getString("runner.summary"));

    ArgumentGroup propertiesFileSection =
        parser.addArgumentGroup(translations.getString("runner.configSection"));

    propertiesFileSection
        .addArgument("-g", "--gen-config")
        .action(Arguments.storeTrue())
        .required(false)
        .type(Arguments.booleanType())
        .dest(GEN_CONFIG)
        .help(translations.getString("runner.genConfig.help"));

    ArgumentGroup producerParams =
        parser.addArgumentGroup(translations.getString("runner.producerConfigSection"));

    producerParams
        .addArgument("-t", "--topic")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .dest(TOPIC)
        .help(translations.getString("runner.topic.help"));

    producerParams
        .addArgument("-c", "--producer-config")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .setDefault(DEFAULT_PRODUCER_CONFIG)
        .dest(PRODUCER_CONFIG)
        .help(translations.getString("runner.producerConfigFile.help"));

    producerParams
        .addArgument("-T", "--throughput")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .dest(THROUGHPUT)
        .setDefault(DEFAULT_THROUGHPUT)
        .help(translations.getString("runner.throughput.help"));

    producerParams
        .addArgument("-x", "--num-threads")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .dest(NUM_THREADS)
        .setDefault(DEFAULT_NUMBER_THREADS)
        .help(translations.getString("runner.numThreads.help"));

    ArgumentGroup generalConfig =
        parser.addArgumentGroup(translations.getString("runner.generalConfigSection"));

    generalConfig
        .addArgument("-n", "--num-records")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .dest(NUM_RECORDS)
        .setDefault(DEFAULT_NUM_RECORDS)
        .help(translations.getString("runner.numRecords.help"));

    generalConfig
        .addArgument("-b", "--batch")
        .action(Arguments.storeTrue())
        .required(false)
        .type(Boolean.class)
        .setDefault(false)
        .dest(BATCH)
        .help(translations.getString("runner.batchMode.help"));

    ArgumentGroup payloadOptions =
        parser
            .addArgumentGroup("Payload configuration")
            .description(translations.getString("runner.payload.options"));

    payloadOptions
        .addArgument("-f", "--payload-template")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .dest(PAYLOAD_TEMPLATE)
        .help(translations.getString("runner.payloadTemplate.help"));

    payloadOptions
        .addArgument("-s", "--start-timestamp")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .dest(PayloadConfig.START_TIMESTAMP)
        .help(translations.getString("runner.payloadStartTimestamp.help"));

    MutuallyExclusiveGroup dateRange = parser.addMutuallyExclusiveGroup("Date range");

    dateRange
        .addArgument("-e", "--end-timestamp")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .dest(PayloadConfig.END_TIMESTAMP)
        .help(translations.getString("runner.payloadEndTimestamp.help"));

    dateRange
        .addArgument("-i", "--interval")
        .action(Arguments.store())
        .required(false)
        .type(Long.class)
        .dest(PayloadConfig.TIMESTAMP_INTERVAL)
        .help(translations.getString("runner.payloadEndTimestamp.help"));

    return parser;
  }
}
