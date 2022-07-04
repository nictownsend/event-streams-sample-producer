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