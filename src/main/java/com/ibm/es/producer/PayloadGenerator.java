package com.ibm.es.producer;

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
import java.util.Random;
import net.jimblackler.jsongenerator.Configuration;
import net.jimblackler.jsongenerator.DefaultConfig;
import net.jimblackler.jsongenerator.Generator;
import net.jimblackler.jsongenerator.JsonGeneratorException;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class PayloadGenerator {

  private final Handlebars handlebars;
  private final Faker faker = new Faker();
  private final String payloadFilePath;
  private final SchemaStore schemaStore;
  private final Configuration config;

  public PayloadGenerator(String payloadFilePath) {
    this.payloadFilePath = payloadFilePath;
    config = DefaultConfig.build().setGenerateMinimal(false).setNonRequiredPropertyChance(2f).get();
    schemaStore = new SchemaStore(true);
    TemplateLoader loader = new FileTemplateLoader("", "");
    handlebars = new Handlebars(loader);

    handlebars.registerHelper(
        "faker-date",
        (o, options) -> {
          String start = options.param(0);
          String end = options.param(1);
          SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
          try {
            final Date generated = faker.date().between(date.parse(start), date.parse(end));
            return new Timestamp(generated.getTime());
          } catch (ParseException e) {
            e.printStackTrace();
            return "";
          }
        });

    handlebars.registerHelper(
        "faker-int",
        (o, options) -> {
          Integer min = options.param(0);
          Integer max = options.param(1);
          return faker.number().numberBetween(min, max);
        });

    handlebars.registerHelper(
        "faker-double",
        (o, options) -> {
          Integer min = options.param(0);
          Integer max = options.param(1);
          Integer decimals = options.param(2, 2);
          return faker.number().randomDouble(decimals, min, max);
        });

    handlebars.registerHelper("faker-uuid", (o, options) -> faker.idNumber().valid());
    handlebars.registerHelper("faker-firstName", (o, options) -> faker.name().firstName());
    handlebars.registerHelper("faker-lastName", (o, options) -> faker.name().lastName());
    handlebars.registerHelper("faker-fullName", (o, options) -> faker.name().fullName());
  }

  public String generatePayload() throws JsonGeneratorException, GenerationException, IOException {

    Template template = handlebars.compile(payloadFilePath);
    final String schema = template.apply(null);
    Schema loadedSchema = schemaStore.loadSchemaJson(schema);
    Generator generator = new Generator(config, schemaStore, new Random());
    final Object generated = generator.generate(loadedSchema, 10);
    return generated.toString();
  }
}
