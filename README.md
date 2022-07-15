# Flink Workload Generator

A sample workload runner for generating JSON messages onto a Kafka topic, or as a new line delimited file.

Available as a Docker image at https://quay.io/repository/nictownsend/flink-workload-generator

This runner takes a JSON object with custom handlebars templates for generating field values.

## Building

- Clone this project
- Navigate to the root directory of the project and run the following command:

  `mvn package`

- This will create a `flink-workload-generator.jar` file inside the `target` directory.

## Running

```java -jar target/flink-workload-generator.jar <options>```

### Options

| Parameter                | Shorthand | Longhand           | Type                  | Env Var            | Default           | Description                                                                                             |
|--------------------------|-----------|--------------------|-----------------------|--------------------|-------------------|---------------------------------------------------------------------------------------------------------|
| Help                     | -h        | --help             | `N/A`                 | `N/A`              | `N/A`             | Lists the available parameters                                                                          |
| Generate producer config | -g        | --gen-config       | `boolean`             | `N/A`              | `false`           | Generates a producer config file                                                                        |
| Runtime mode             | -m        | --mode             | `"BATCH", "PRODUCER"` | `RUNTIME_MODE`     | `BATCH`           | Write to either a file or to Kafka topic                                                                |
|                          |           |                    |                       |                    |                   |                                                                                                         |
| Producer config          | -c        | --producer-config  | `string`              | `PRODUCER_CONFIG`  | `producer.config` | Path to producer configuration file                                                                     |
| Topic                    | -t        | --topic            | `string`              | `TOPIC`            | `N/A`             | The name of the topic to produce to                                                                     |
| Number of producers      | -n        | --num-producers    | `integer`             | `NUM_PRODUCERS`    | `1`               | The number of producers to use                                                                          |
| Throughput               | -T        | --throughput       | `integer`             | `THROUGHPUT`       | `-1`              | Throttle each producer to produce at most *THROUGHPUT* records per second. -1 means as fast as possible |
|                          |           |                    |                       |                    |                   |                                                                                                         |
| Output file              | -o        | --output-file      | `string`              | `OUTPUT-FILE`      | `output.txt`      | File to write generated messages to                                                                     |
|                          |           |                    |                       |                    |                   |                                                                                                         |
| Payload template         | -f        | --payload-template | `string`              | `PAYLOAD_TEMPLATE` | `payload.hbs`     | Path to the payload template file                                                                       |
| Number of records        | -r        | --num-records      | `integer`             | `NUM_RECORDS`      | `100`             | Number of records to be generated (in batch mode) or to be sent in total across all producers           |

## Payload templating

A template file will be used to generate JSON messages.

### Example template
```
{
 "ts": "{{fake-datetime this start="01-10-2020'T'13:00:05" end="02-10-2020'T'13:00:05"}}",
 "url": "{{oneof "/home" "/closure" "/join-us"}}",
 "customer_id": {{fake-int this end=1000000}},
  "txn_id": "{{fake-uuid this}}"
}
```

This will generate sample messages that use the handlebars template functions to generate values. E.g:

```
{
  "ts": "01-10-2020T15:00:15",
  "url": "/home",
  "customer_id": 1456,
  "txn_id": "rt4-t553-gg33"
}
```

### Custom template functions

| field type | helper                                                                    | default values                                                                                  | notes                                                                                                                                                                                                                                                                                                                          |
|------------|---------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| uuid       | `{{fake-uuid this}}`                                                      |                                                                                                 |                                                                                                                                                                                                                                                                                                                                |
| timestamp  | `{{fake-date this <start=> <end=> <sequential=true> <id=> }}`             | `start: now`, `end: 1 hour from now`, `sequential: false`, `id: 0`                              | Generate a date between the two dates. Date format: `dd-MM-yyyy`.<br/>If using `sequential` then the date range will be distributed equally across all generated payloads. If you are using the helper multiple times in a payload, you can add a unique <id> to ensure each usage has its own sequence.                       |
|            | `{{fake-datetime this <start=> <end=> <sequential=true> <id=> }}`         | `start: now`, `end: 1 hour from now`, `sequential: false`, `id: 0`                              | Random timestamp between two timestamps. Datetime format: `dd-M-yyyy'T'HH:mm:ss`.<br/>If using `sequential` then the timestamp range will be distributed equally across all generated payloads.  If you are using the helper multiple times in a payload, you can add a unique <id> to ensure each usage has its own sequence. |
| int        | `{{fake-int this <min=> <max=> <sequential=true> <increment=>  <id=>}}`   | `min: Java Integer.min`, `max: Java Integer.max`, `sequential: false`, `increment: 1l`, `id: 0` | Random integer between two values (inclusive).<br/>If using `sequential` then the sequence will increase by the increment and restart if max is reached. If you are using the helper multiple times in a payload, you can add a unique <id> to ensure each usage has its own sequence.                                         |
| long       | `{{fake-long this <min=> <max=> <sequential=true> <increment=> <id=>}}`   | `min: Java Long.min`, `max: Java Long.max`, `sequential: false`, `increment: 1`, `id: 0`        | Random long between two values (inclusive).<br/>If using `sequential` then the sequence will increase by the increment and restart if max is reached. If you are using the helper multiple times in a payload, you can add a unique <id> to ensure each usage has its own sequence.                                            |
| double     | `{{fake-double this <min=> <max=> <sequential=true> <increment=> <id=>}}` | `min: Java Long.min`, `max: Java Long.max`, `sequential: false`, `increment: 1.0`, `id: 0`      | Random double between two values (inclusive) to 2 decimal places.<br/>If using `sequential` then the sequence will increase by the increment and restart if max is reached. If you are using the helper multiple times in a payload, you can add a unique <id> to ensure each usage has its own sequence.                      |
| first name | `{{fake-firstName this}}`                                                 |                                                                                                 |                                                                                                                                                                                                                                                                                                                                |
| last name  | `{{fake-lastName this}}`                                                  |                                                                                                 |                                                                                                                                                                                                                                                                                                                                |
| full name  | `{{fake-fullName this}}`                                                  |                                                                                                 |                                                                                                                                                                                                                                                                                                                                |

## Producer Configuration

If you are running against a Kafka topic, you will need to generate a producer configuration file.

Use `--gen-config` to generate a skeleton `producer.config` in the current directory. Edit to supply the required values for your Kafka cluster.

## Consuming the output in a Flink SQL job

### Kafka
```
create table <name> (<col> TYPE, ...) with ('connector' = 'kafka','topic' = '<topic>','properties.bootstrap.servers' = '<bootstrap>', 'properties.group.id' = '<group.id>','scan.startup.mode' = 'earliest-offset','format' = 'json');
```

### File
```
create table <name> (<col> TYPE, ...) with ('connector' = 'filesystem','path' = 'file:///<path-to-file>','format' = 'json');
```
*Note - the file must be available to read from both the Job manager and Task manager.*

## Deploying on Openshift
The `deployment` folder contains scripts to deploy a flink session cluster, a flink sql client, and the workload generator onto a k8s environment.

Pre-req:
- Install the flink k8s operator onto your Openshift - https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/docs/try-flink-kubernetes-operator/quick-start/
- Provide `anyuid` access to the default flink service account: `oc adm policy add-scc-to-user anyuid system:serviceaccount:flink`
- Install `yq` v4 onto your machine
- Edit `deployment/payload.hbs` to represent the required payload template and optionally `deployment/producer.config` if you are pointing to a Kafka cluster.
- Create a rwx PVC named `flink-files` - this is shared across the workload generator and Flink to allow files generated in `batch` mode to be read using the `filesystem` connector.

Usage: `./deploy.sh <REPO> <IMAGE> <TAG> <NAMESPACE>`
 
Steps: 
- Builds the JAR file
- Builds and pushes a docker image to the repo
- Deploys a flink cluster, sql client, and workload generator

Params:
- `<REPO>`: The docker repository (e.g `quay.io/myusername`)
- `<IMAGE>`: Image name
- `<TAG>`: Image tag
- `<NAMESPACE>`: k8s ns to deploy into

Note - add `web.cancel.enable: true` to the `flink-config-session-cluster/flink.conf` to enable cancelling jobs from the UI.

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
