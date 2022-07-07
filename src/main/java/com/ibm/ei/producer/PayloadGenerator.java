package com.ibm.ei.producer;

import com.github.javafaker.Faker;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.ibm.ei.producer.config.PayloadConfig;
import com.ibm.ei.utils.FakeDate;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadGenerator {

  private static final Logger logger = LoggerFactory.getLogger(PayloadGenerator.class);
  private final Handlebars handlebars;
  private final Faker faker = new Faker();
  private final PayloadConfig config;

  public PayloadGenerator(PayloadConfig config) throws ParseException {
    this.config = config;

    TemplateLoader loader = new FileTemplateLoader("", "");
    this.handlebars = new Handlebars(loader);

    String startTime = config.getStartTimestamp();
    Date startDate = new Date();

    if (Objects.nonNull(startTime)) {
      startDate.setTime(PayloadConfig.TIMESTAMP_FORMAT.parse(startTime).getTime());
    }

    long interval = config.getTimestampInterval();

    String endTime = config.getEndTimestamp();

    if (Objects.nonNull(endTime)) {
      Date end = new Date();
      end.setTime(PayloadConfig.TIMESTAMP_FORMAT.parse(endTime).getTime());
      long diff = (end.getTime() - startDate.getTime());
      interval = diff / config.getNumRecords();
    }

    System.err.println(
        "Generating "
            + config.getNumRecords()
            + " messages with interval = "
            + interval
            + ", startTime = "
            + startDate.toString()
            + ", endTime = "
            + endTime);

    FakeDate fixedStart = new FakeDate(startDate.getTime(), interval);

    handlebars.registerHelper(
        "fake-date-random",
        (o, options) -> {
          String start = options.param(0);
          String end = options.param(1);
          return timestamp(start, end, PayloadConfig.DATE_FORMAT);
        });

    handlebars.registerHelper(
        "fake-datetime-random",
        (o, options) -> {
          String start = options.param(0);
          String end = options.param(1);
          return timestamp(start, end, PayloadConfig.TIMESTAMP_FORMAT);
        });

    handlebars.registerHelper(
        "fake-datetime-sequential", (o, options) -> timestamp(fixedStart.getTime()));

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
    handlebars.registerHelper(
        "oneof",
        (o, options) -> {
          Object[] choices = options.params;
          int index = Long.valueOf(Math.round(Math.random() * (choices.length - 1))).intValue();
          return choices[index];
        });
  }

  private Timestamp timestamp(long time) {
    return new Timestamp(time);
  }

  private Timestamp timestamp(String start, String end, SimpleDateFormat format) {

    try {
      final Date generated = faker.date().between(format.parse(start), format.parse(end));
      return timestamp(generated.getTime());
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String generatePayload() throws IOException {

    Template template = handlebars.compile(this.config.getTemplateFilePath());
    return template.apply(null);
  }
}
