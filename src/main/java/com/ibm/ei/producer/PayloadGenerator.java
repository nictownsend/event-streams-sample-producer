package com.ibm.ei.producer;

import com.github.javafaker.Faker;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import com.ibm.ei.producer.config.PayloadConfig;
import com.ibm.ei.utils.FakeDate;
import net.jimblackler.jsongenerator.Configuration;
import net.jimblackler.jsongenerator.DefaultConfig;
import net.jimblackler.jsongenerator.Generator;
import net.jimblackler.jsongenerator.JsonGeneratorException;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadGenerator {

  private static final Logger logger = LoggerFactory.getLogger(PayloadGenerator.class);
  private final Handlebars handlebars;
  private final Faker faker = new Faker();
  private final PayloadConfig config;
  private final SchemaStore schemaStore;
  private final FakeDate startFrom;
  private final Generator generator;

  public PayloadGenerator(PayloadConfig config, FakeDate startFrom) throws GenerationException {
    this.config = config;
    this.schemaStore = new SchemaStore(false);
    this.startFrom = startFrom;

    Configuration generatorConfig = DefaultConfig.build().setGenerateMinimal(false).get();
    this.generator = new Generator(generatorConfig, schemaStore, new Random());

    TemplateLoader loader = new FileTemplateLoader("", "");
    this.handlebars = new Handlebars(loader);

    handlebars.registerHelper(
        "fake-date-random",
        (o, options) -> {
          String start = options.param(0);
          String end = options.param(1);
          SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
          return timestamp(start, end, date);
        });

    handlebars.registerHelper(
        "fake-datetime-random",
        (o, options) -> {
          String start = options.param(0);
          String end = options.param(1);
          SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss");
          return timestamp(start, end, date);
        });

    handlebars.registerHelper(
        "fake-datetime-sequential",
        (o, options) -> startFrom.getTime());

    handlebars.registerHelper(
        "fake-int",
        (o, options) -> {
          Integer min = options.param(0);
          Integer max = options.param(1);
          return faker.number().numberBetween(min, max);
        });

    handlebars.registerHelper(
        "fake-double",
        (o, options) -> {
          Integer min = options.param(0);
          Integer max = options.param(1);
          Integer decimals = options.param(2, 2);
          return faker.number().randomDouble(decimals, min, max);
        });

    handlebars.registerHelper("fake-uuid", (o, options) -> faker.idNumber().valid());
    handlebars.registerHelper("fake-firstName", (o, options) -> faker.name().firstName());
    handlebars.registerHelper("fake-lastName", (o, options) -> faker.name().lastName());
    handlebars.registerHelper("fake-fullName", (o, options) -> faker.name().fullName());
  }


  private Timestamp timestamp(String start, String end, SimpleDateFormat format) {

    try {
      final Date generated = faker.date().between(format.parse(start), format.parse(end));
      return new Timestamp(generated.getTime());
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String generatePayload() throws JsonGeneratorException, GenerationException, IOException {

    Template template = handlebars.compile(this.config.getTemplateFilePath());
    final String schema = template.apply(null);
    Schema loadedSchema = schemaStore.loadSchemaJson(schema);
    final Object generated = generator.generate(loadedSchema, 10);
    String result = new JSONObject((Map) generated).toString();
    return result;
  }
}
