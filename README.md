# kafka-sample-producer

A sample workload producer for generating test data from a JSON schema on a Kafka topic.

Available as a Docker image at https://quay.io/repository/nictownsend/kafka-sample-producer

This producer takes a JSON Schema and will generate random messages that conform to the schema.

## Building

- Clone this project
- Navigate to the root directory of the project and run the following command:

   `mvn clean install`

- This will create a`kakfa-sample-producer.jar` file inside the `target` directory.

## Producer Configuration

Run `java -jar target/kakfa-sample-producer.jar --gen-config` to generate `producer.config`

The following configuration options might be required:

| Attribute                             | Description                                                                                                            |
|  ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `bootstrap.servers`                     | The addressed used by the producer application to connect to Kafka. |
| `ssl.truststore.location`       | The location (path and filename) of the Kafka certificate. |
| `ssl.truststore.password`       | The password of the Kafka certificate. |
| `security.protocol`             | The security protocol to use for connections. e.g `SSL`, `SASL_SSL` |
| `sasl.mechanism`              | The SASL mechanism to use for connections. e.g `SCRAM-SHA-512`, `SASL_PLAIN`   |
| `sasl.jaas.config`            | The SASL configuration details. `org.apache.kafka.common.security.scram.ScramLoginModule required username="<username>" password="<password>";`, with the `<username>` and `<password>` replaced with the SCRAM credentials if using SCRAM. For Kafka 2019.4.2 and earlier this should be set to `org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="<password>";` with the `<password>` replaced by an API key.
| `ssl.keystore.location`       | The location (path and filename) of the `user.p12` keystore file if using TLS credentials.   |
| `ssl.keystore.password`       | The password for the keystore if using TLS credentials.   |

## Payload templating

A pure JSON schema can be used to generate entirely random data. However, for more useful test cases it is better to constrain the input with a JSON schema generated from a template.

A payload can be generated in a two-step process:
1. JSON schema template to produce a constrained JSON schema with Handlebars helpers
2. JSON object generated from the schema
    - using `enum` to restrict possible values
    - using single `enum` with a Handlerbars helper to generate a single value
    
A new JSON schema will be generated for each payload - so for example, you can use `fake-int` with `enum` to create a field in the schema with a single value - but it will be unique each time the schema is generated.

### Custom template functions

| field type | helper | notes |
| --- | --- | --- |
| uuid | `{{fake-uuid this}}` |  |
| timestamp |`{{fake-date this <after> <before>}}` | Date format: `dd-MM-yyyy` |
|  |`{{fake-datetime this <after> <before>}}` |Datetime format: `dd-M-yyyy'T'HH:mm:ss` |
| int |`{{fake-int this <min> <max>}}` | |
| double | `{{fake-double this <min> <max>}}`| | 
| first name |`{{fake-firstName this}}` | |
| last name |`{{fake-lastName this}}` | |
| full name |`{{fake-fullName this}}` | | 

### Example

```
{
  "type": "object",
  "properties": {
    "date": {
      "enum": ["{{faker-date this "21-06-2022" "24-06-2022"}}"]
    },
    "url" : {
        "enum": ["/home", "/closure", "/join-us"]
    },
    "customer_id": {
        "enum": ["{{faker-int this 0 50}}"]
    },
    "txn_id": {
        "enum": ["{{faker-uuid this}}"]
    }
  },
  "required": ["date", "url", "customer_id", "txn_id"]
}
```

### Notes
- `required` used to enforce fields to be generated. If a property is not marked as required, it is not guaranteed to be in a generated payload.
- `enum` used with Handlebars to generate a fixed field in the schema

## Running

```java -jar target/kakfa-sample-producer.jar <options>```

###  Options

| Parameter             | Shorthand | Longhand              | Type     | Description                                                                                                                               | Default          |
| --------------------- | --------- | --------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| Topic                 | -t        | --topic               | `string` | The name of the topic to produce to                                                                                                         | `60000`          |
| Payload Template      | -f        | --payload-template    | `string` | File to read the message payloads from. This works only for UTF-8 encoded text files. Payloads will be read from this  file and a payload will be randomly selected when sending messages. |   |
| Throughput            | -T        | --throughput          | `integer`| Throttle maximum message throughput to *approximately* *THROUGHPUT* messages per second. -1 means as fast as possible                     | `-1`             |                                                                         | `loadtest`       |
| Num Records           | -n        | --num-records         | `integer`| The total number of messages to be sent (across all threads)     
| Producer Config       | -c        | --producer-config     | `string` | Path to producer configuration file                                                                                                       | `producer.config`|
| Num Threads           | -x        | --num-threads         | `integer`| The number of producer threads to run                                                                                                     | `1`              |
| Help                  | -h        | --help                | `N/A`    | Lists the available parameters                                                                                                            |                  |
| Gen Config            | -g        | --gen-config          | `N/A`    | Generates the configuration file required to run the tool                                                                                 |                  |


### Environment Overrides for Docker/Kubernetes

Setting the following environment variables will override the value used for each parameter.

| Parameter             | Environment Variable |
| --------------------- | -------------------- |
| Throughput            | ES_THROUGHPUT        |
| Num Records           | ES_NUM_RECORDS       |
| Topic                 | ES_TOPIC             |
| Num Threads           | ES_NUM_THREADS       |
| Producer Config       | ES_PRODUCER_CONFIG   |
| Payload Template      | ES_PAYLOAD_TEMPLATE  |

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
