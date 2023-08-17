# debezium-test

### Info

This repository is playground for the debezium-kafka-postgresql.

Read the following:

1. Tutorial: https://debezium.io/documentation/reference/stable/tutorial.html
2. Postgres connector config: https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-example-configuration
3. Avro serialization: https://debezium.io/documentation/reference/stable/configuration/avro.html
4. Examples: https://github.com/debezium/debezium-examples

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

### 7. Shut down

```shell
docker compose down -v
```