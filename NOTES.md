create table txns (customer_id INTEGER, url STRING, txn_id STRING, ts TIMESTAMP(3), WATERMARK FOR ts AS ts - INTERVAL '5' SECOND ) with ('connector' = 'kafka','topic' = 'txns','properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink-env.svc:9092', 'properties.group.id' = 'flinkGroup','scan.startup.mode' = 'earliest-offset','format' = 'json');

create table txn_count (window_start TIMESTAMP(3), window_end TIMESTAMP(3), customer_id INTEGER, txCount BIGINT) with ('connector' = 'kafka','topic' = 'txn_count','properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink-env.svc:9092','format' = 'json');


insert into txn_count (select window_start, window_end, customer_id, count(customer_id) as txCount
  from table ( hop ( table txns, descriptor(ts), interval '1' minutes, interval '10' minutes))
  group by window_start, window_end, customer_id 
  having count(customer_id) > 3);





create table txns (customer_id INTEGER, url STRING, txn_id STRING, ts TIMESTAMP(3), WATERMARK FOR ts AS ts - INTERVAL '5' SECOND ) with ('connector' = 'filesystem','path' = 'file:///opt/flink/files/input.json','format' = 'json');

create table txn_count (window_start TIMESTAMP(3), window_end TIMESTAMP(3), customer_id INTEGER, txCount BIGINT) with ('connector' = 'filesystem','path' = 'file:///opt/flink/files/output.json','format' = 'json');


insert into txn_count (select window_start, window_end, customer_id, count(customer_id) as txCount
  from table ( hop ( table txns, descriptor(ts), interval '1' minutes, interval '10' minutes))
  group by window_start, window_end, customer_id 
  having count(customer_id) > 3);


create table txns_test (customer_id INTEGER, url STRING, txn_id STRING, ts TIMESTAMP(3), WATERMARK FOR ts AS ts - INTERVAL '5' SECOND ) with ('connector' = 'filesystem','path' = 'file:///opt/flink/files/output.json','format' = 'json');


https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/table/functions/systemfunctions/#aggregate-functions


==== UPSERT KAFKA ====

create table partsCountInput (counts INTEGER, id INTEGER) with ('connector' = 'kafka','topic' = 'partsCountInput','properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink-env.svc:9092', 'properties.group.id' = 'flinkGroup','scan.startup.mode' = 'earliest-offset','format' = 'json');

create table partsCountOutput (counts INTEGER, id INTEGER PRIMARY KEY) with ('connector' = 'upsert-kafka','topic' = 'partsCountOutput','properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink-env.svc:9092', 'properties.group.id' = 'flinkGroup', 'key.format' = 'raw', 'value.format' ='json');


{"counts": 1, "id" : 2}
{"counts": 5, "id" : 2}
{"counts": 300, "id" : 2}

CreateTime:1657546182903	Partition:0	Offset:0	NO_HEADERS	{"id":1}	{"id":1,"counts":1}
CreateTime:1657546182908	Partition:0	Offset:1	NO_HEADERS	{"id":1}	{"id":1,"counts":2}
CreateTime:1657546182908	Partition:0	Offset:2	NO_HEADERS	{"id":1}	{"id":1,"counts":3}
CreateTime:1657546182908	Partition:0	Offset:3	NO_HEADERS	{"id":2}	{"id":2,"counts":3}
CreateTime:1657546182908	Partition:0	Offset:4	NO_HEADERS	{"id":2}	{"id":2,"counts":5}
CreateTime:1657546182909	Partition:0	Offset:5	NO_HEADERS	{"id":2}	{"id":2,"counts":3}
CreateTime:1657546182909	Partition:0	Offset:6	NO_HEADERS	{"id":3}	{"id":3,"counts":3}

org.apache.flink.table.api.TableException: Table sink 'default_catalog.default_database.partsCountOutput' doesn't support consuming update changes which is produced by node GroupAggregate(groupBy=[id], select=[id, LAST_VALUE(counts) AS EXPR$1])