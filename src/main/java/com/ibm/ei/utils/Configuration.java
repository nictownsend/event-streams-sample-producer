package com.ibm.ei.utils;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Configuration {

  public static final String BATCH_MODE = "BATCH";
  public static final String PRODUCER_MODE = "PRODUCER";
  public static final String RUNTIME_MODE = "RUNTIME_MODE";
  public static final String OUTPUT_PATH = "OUTPUT";
  public static final String GEN_CONFIG = "GEN_CONFIG";
  public static final String TOPIC = "TOPIC";
  public static final String PRODUCER_CONFIG = "PRODUCER_CONFIG";
  public static final String THROUGHPUT = "THROUGHPUT";
  public static final String NUM_PRODUCERS = "NUM_THREADS";
  public static final String PAYLOAD_TEMPLATE = "PAYLOAD_TEMPLATE";
  public static final String NUM_RECORDS = "NUM_RECORDS";
  private static final String DEFAULT_PRODUCER_CONFIG = "producer.config";
  private static final String DEFAULT_OUTPUT = "output.txt";
  private static final String DEFAULT_PAYLOAD = "payload.hbs";
  private static final Integer DEFAULT_THROUGHPUT = -1;
  private static final Integer DEFAULT_NUMBER_PRODUCERS = 1;
  private static final Integer DEFAULT_NUM_RECORDS = 100;
  private static final ResourceBundle translations =
      ResourceBundle.getBundle("MessageBundle", Locale.getDefault());

  private final Namespace ns;
  private final ArgumentParser parser;

  public Configuration(String[] args) throws ArgumentParserException {
    this.parser =
        ArgumentParsers.newFor("java -jar flink-workload-generator.jar")
            .singleMetavar(true)
            .build()
            .defaultHelp(true)
            .description(translations.getString("runner.summary"));

    ArgumentGroup generalConfig =
        parser.addArgumentGroup(translations.getString("runner.generalConfigSection"));

    generalConfig
        .addArgument("-g", "--gen-config")
        .action(Arguments.storeTrue())
        .type(Boolean.class)
        .setDefault(false)
        .dest(GEN_CONFIG)
        .help(translations.getString("runner.genConfig.help"));

    generalConfig
        .addArgument("-m", "--mode")
        .action(Arguments.store())
        .setDefault(BATCH_MODE)
        .choices(BATCH_MODE, PRODUCER_MODE)
        .type(String.class)
        .dest(RUNTIME_MODE)
        .help(translations.getString("runner.runtimeMode.help"));

    ArgumentGroup producerConfig =
        parser.addArgumentGroup(translations.getString("runner.producerConfigSection"));

    producerConfig
        .addArgument("-c", "--producer-config")
        .action(Arguments.store())
        .type(String.class)
        .setDefault(DEFAULT_PRODUCER_CONFIG)
        .dest(PRODUCER_CONFIG)
        .help(translations.getString("runner.producerConfigFile.help"));

    producerConfig
        .addArgument("-t", "--topic")
        .action(Arguments.store())
        .type(String.class)
        .dest(TOPIC)
        .help(translations.getString("runner.topic.help"));

    producerConfig
        .addArgument("-n", "--num-producers")
        .action(Arguments.store())
        .type(Integer.class)
        .dest(NUM_PRODUCERS)
        .setDefault(DEFAULT_NUMBER_PRODUCERS)
        .help(translations.getString("runner.numProducers.help"));

    producerConfig
        .addArgument("-T", "--throughput")
        .action(Arguments.store())
        .type(Integer.class)
        .dest(THROUGHPUT)
        .setDefault(DEFAULT_THROUGHPUT)
        .help(translations.getString("runner.throughput.help"));

    ArgumentGroup batchConfig =
        parser
            .addArgumentGroup("Batch mode")
            .description(translations.getString("runner.batchConfigSection"));

    batchConfig
        .addArgument("-o", "--output-file")
        .action(Arguments.store())
        .type(String.class)
        .dest(OUTPUT_PATH)
        .setDefault(DEFAULT_OUTPUT)
        .help(translations.getString("runner.outputFile.help"));

    ArgumentGroup payloadOptions =
        parser
            .addArgumentGroup("Payload configuration")
            .description(translations.getString("runner.payload.options"));

    payloadOptions
        .addArgument("-f", "--payload-template")
        .action(Arguments.store())
        .type(String.class)
        .dest(PAYLOAD_TEMPLATE)
        .setDefault(DEFAULT_PAYLOAD)
        .help(translations.getString("runner.payloadTemplate.help"));

    payloadOptions
        .addArgument("-r", "--num-records")
        .action(Arguments.store())
        .type(Integer.class)
        .dest(NUM_RECORDS)
        .setDefault(DEFAULT_NUM_RECORDS)
        .help(translations.getString("runner.numRecords.help"));

    this.ns = parser.parseArgs(args);
  }

  public Integer getInt(String key) {
    return Optional.ofNullable(System.getenv().get(key)).map(Integer::parseInt).orElse(ns.get(key));
  }

  public Double getDouble(String key) {
    return Optional.ofNullable(System.getenv().get(key))
        .map(Double::parseDouble)
        .orElse(ns.get(key));
  }

  public Long getLong(String key) {
    return Optional.ofNullable(System.getenv().get(key)).map(Long::parseLong).orElse(ns.get(key));
  }

  public String getString(String key) {
    return Optional.ofNullable(System.getenv().get(key)).orElse(ns.get(key));
  }

  public Boolean getBoolean(String key) {
    return Optional.ofNullable(System.getenv().get(key)).map(Boolean::valueOf).orElse(ns.get(key));
  }

  public void validate() throws ArgumentParserException {

    if (getBoolean(GEN_CONFIG)) return;

    boolean batchMode = getString(RUNTIME_MODE).equals(BATCH_MODE);

    if (Objects.isNull(getString(PAYLOAD_TEMPLATE))
        || (!batchMode
            && (Objects.isNull(getString(TOPIC)) || Objects.isNull(getString(PRODUCER_CONFIG))))
        || (batchMode && Objects.isNull(getString(OUTPUT_PATH)))) {
      throw new ArgumentParserException(translations.getString("runner.argsMissing"), parser);
    }

    if (!new File(getString(PAYLOAD_TEMPLATE)).canRead()
        || (batchMode && !new File(getString(OUTPUT_PATH)).canWrite())
        || (!batchMode && !new File(getString(PRODUCER_CONFIG)).canRead())) {
      throw new ArgumentParserException(translations.getString("runner.invalidPath"), parser);
    }

    if (getInt(NUM_PRODUCERS) < 1) {
      throw new ArgumentParserException(translations.getString("runner.invalidThreads"), parser);
    }

    if (getInt(THROUGHPUT) < 1 && getInt(THROUGHPUT) != -1) {
      throw new ArgumentParserException(translations.getString("runner.invalidThroughput"), parser);
    }
  }
}
