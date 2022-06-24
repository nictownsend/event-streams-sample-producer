package com.ibm.ei.producer;

import com.github.javafaker.Faker;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.ibm.ei.producer.config.PayloadConfig;
import com.ibm.ei.utils.FakeDate;
import com.ibm.ei.utils.FakeNumber;
import com.ibm.ei.utils.ThrowingFunction;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadGenerator {

  private static final Logger logger = LoggerFactory.getLogger(PayloadGenerator.class);

  private static final String ARGS_START = "start";
  private static final String ARGS_END = "end";
  private static final String ARGS_MIN = "min";
  private static final String ARGS_MAX = "max";
  private static final String ARGS_SEQUENTIAL = "sequential";
  private static final String ARGS_INCREMENT = "increment";
  private static final String ARGS_ID = "id";

  private final Handlebars handlebars;
  private final Faker faker = new Faker();
  private final File path;

  public PayloadGenerator(PayloadConfig config) {
    this.path = new File(config.getTemplateFilePath());

    TemplateLoader loader = new FileTemplateLoader(path.getParent(), "");
    this.handlebars = new Handlebars(loader);

    Map<Integer, FakeDate> fakeDates = new HashMap<>();
    Map<Integer, FakeDate> fakeTimestamps = new HashMap<>();
    Map<Integer, FakeNumber> fakeInts = new HashMap<>();
    Map<Integer, FakeNumber> fakeLongs = new HashMap<>();
    Map<Integer, FakeNumber> fakeDoubles = new HashMap<>();

    handlebars.registerHelper(
        "fake-date",
        (o, options) -> {
          boolean sequential = options.hash(ARGS_SEQUENTIAL, false);
          Date start =
              Optional.ofNullable(options.<String>hash(ARGS_START))
                  .map(ThrowingFunction.unchecked(PayloadConfig.DATE_FORMAT::parse))
                  .orElse(Calendar.getInstance().getTime());
          Date end =
              Optional.ofNullable(options.<String>hash(ARGS_END))
                  .map(ThrowingFunction.unchecked(PayloadConfig.DATE_FORMAT::parse))
                  .orElseGet(
                      () -> {
                        final Calendar now = Calendar.getInstance();
                        now.add(Calendar.HOUR, 1);
                        return now.getTime();
                      });

          if (sequential) {
            int id = options.hash(ARGS_ID, 0);
            FakeDate time =
                Optional.ofNullable(fakeDates.get(id))
                    .orElse(new FakeDate(start, end, config.getNumRecords()));
            fakeDates.put(id, time);
            return timestamp(time);
          }

          return timestamp(start, end);
        });

    handlebars.registerHelper(
        "fake-datetime",
        (o, options) -> {
          boolean sequential = options.hash(ARGS_SEQUENTIAL, false);
          Date start =
              Optional.ofNullable(options.<String>hash(ARGS_START))
                  .map(ThrowingFunction.unchecked(PayloadConfig.TIMESTAMP_FORMAT::parse))
                  .orElse(Calendar.getInstance().getTime());
          Date end =
              Optional.ofNullable(options.<String>hash(ARGS_END))
                  .map(ThrowingFunction.unchecked(PayloadConfig.TIMESTAMP_FORMAT::parse))
                  .orElseGet(
                      () -> {
                        final Calendar now = Calendar.getInstance();
                        now.add(Calendar.HOUR, 1);
                        return now.getTime();
                      });

          if (sequential) {
            int id = options.hash(ARGS_ID, 0);
            FakeDate time =
                Optional.ofNullable(fakeTimestamps.get(id))
                    .orElse(new FakeDate(start, end, config.getNumRecords()));
            fakeTimestamps.put(id, time);
            return timestamp(time);
          }

          return timestamp(start, end);
        });

    handlebars.registerHelper(
        "fake-int",
        (o, options) -> {
          boolean sequential = options.hash(ARGS_SEQUENTIAL, false);
          int min = options.hash(ARGS_MIN, Integer.MIN_VALUE);
          int max = options.hash(ARGS_MAX, Integer.MAX_VALUE);

          if (sequential) {
            int id = options.hash(ARGS_ID, 0);
            int increment = options.hash(ARGS_INCREMENT, 1);
            FakeNumber next =
                Optional.ofNullable(fakeInts.get(id)).orElse(new FakeNumber(min, max, increment));
            fakeInts.put(id, next);
            return next.next();
          }

          return faker.number().numberBetween(min, max);
        });

    handlebars.registerHelper(
        "fake-long",
        (o, options) -> {
          boolean sequential = options.hash(ARGS_SEQUENTIAL, false);
          long min = options.hash(ARGS_MIN, Long.MIN_VALUE);
          long max = options.hash(ARGS_MAX, Long.MAX_VALUE);

          if (sequential) {
            int id = options.hash(ARGS_ID, 0);
            long increment = options.hash(ARGS_INCREMENT, 1l);
            FakeNumber next =
                Optional.ofNullable(fakeLongs.get(id)).orElse(new FakeNumber(min, max, increment));
            fakeLongs.put(id, next);
            return next.next();
          }

          return faker.number().numberBetween(min, max);
        });

    handlebars.registerHelper(
        "fake-double",
        (o, options) -> {
          boolean sequential = options.hash(ARGS_SEQUENTIAL, false);
          long min = options.hash(ARGS_MIN, Double.MIN_VALUE);
          long max = options.hash(ARGS_MAX, Double.MAX_VALUE);

          if (sequential) {
            int id = options.hash(ARGS_ID, 0);
            double increment = options.hash(ARGS_INCREMENT, 1.0);
            FakeNumber next =
                Optional.ofNullable(fakeDoubles.get(id))
                    .orElse(new FakeNumber(min, max, increment));
            fakeDoubles.put(id, next);
            return next.next();
          }

          return faker.number().randomDouble(2, min, max);
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

  private Timestamp timestamp(Date time) {
    return new Timestamp(time.getTime());
  }

  private Timestamp timestamp(Date start, Date end) {
    return timestamp(faker.date().between(start, end));
  }

  public String generatePayload() throws IOException {

    Template template = handlebars.compile(path.getName());
    return template.apply(null);
  }
}
