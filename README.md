# debezium-test

### Info

This repository is playground for the debezium-kafka-postgresql.

Read the following:

1. Tutorial: https://debezium.io/documentation/reference/stable/tutorial.html
2. Postgres connector config: https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-example-configuration
3. Avro serialization: https://debezium.io/documentation/reference/stable/configuration/avro.html
4. Apicurio schema registry: https://www.apicur.io/registry/docs/apicurio-registry/2.4.x/index.html
5. Examples: https://github.com/debezium/debezium-examples

### 1. Set up

```sh
docker compose up -d
```

### 2. Start Postgres connector

```shell
curl -i -X POST \
  -H "Accept:application/json" \
  -H  "Content-Type:application/json" \
  http://localhost:58083/connectors/ \
  -d @register-postgres.json
```

Alternatively, you can use the avro connector.
This connector uses the `Apicurio` schema registry.

**Firstly you need to create the `apicurio` schema in the database
(see [7. Apicurio schema registry](#7-apicurio-schema-registry))**

Attach the connector with:
```shell
curl -i -X POST \
  -H "Accept:application/json" \
  -H  "Content-Type:application/json" \
  http://localhost:58083/connectors/ \
  -d @register-postgres-avro.json
```

### 3. Verify that `inventory-connector` is registered

```shell
curl -H "Accept:application/json" localhost:58083/connectors/
```

### 4. Review the connector’s tasks

```shell
curl -i -X GET -H "Accept:application/json" localhost:58083/connectors/inventory-connector
```

You should see something like the following

```text
HTTP/1.1 200 OK
Date: Thu, 06 Feb 2020 22:12:03 GMT
Content-Type: application/json
Content-Length: 531
Server: Jetty(9.4.20.v20190813)

{
  "name": "inventory-connector",
  ...
  "tasks": [
    {
      "connector": "inventory-connector",  
      "task": 0
    }
  ]
}
```

### 5. kafka-ui

kafka-ui dashboard: http://localhost:58000/

### 6. debezium-ui

debezium-ui dashboard: http://localhost:58001/

### 7. Apicurio schema registry

The registry is not going to start up.
First, we need to create a schema with name `apicurio` under the `postgres` database.
Then just restart the service using:
```shell
docker compose start apicurio
```


apicurio-ui dashboard: http://localhost:58002/

### Shut down

```shell
docker compose down -v
```

### Notes
Connector for PostgreSQL does not create events for schema changes.

#### Add column with default value
Add column with default value should be avoided. Instead, we need to do the following:
  1. Alter the table and add the column with null value.
  2. Update all rows.
  3. Alter table set not null and default value.
  
```sql
alter table customers add column test text null;
update customers set test='test';
alter table customers alter column test set not null;
alter table customers alter column test set default 'test';
```

#### Support for schema in json fields
Check here: https://github.com/birdiecare/connect-smts
Also: https://jcustenborder.github.io/kafka-connect-documentation/projects/kafka-connect-json-schema/transformations/FromJson.html

#### Kafka connect plugins
By default, the directory `/kafka/connect` is used as plugin directory by the Debezium Docker image for Kafka Connect.
So any additional connectors you may wish to use should be added to that directory.

Alternatively,
you can add further directories to the plugin path
by specifying the `KAFKA_CONNECT_PLUGINS_DIR` environment variable when starting the container
(e.g. `-e KAFKA_CONNECT_PLUGINS_DIR=/kafka/connect/,/path/to/further/plugins`).
When using the container image for Kafka Connect provided by Confluent,
you can specify the `CONNECT_PLUGIN_PATH` environment variable to achieve the same.
