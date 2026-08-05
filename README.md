# dissco-virtual-collection-service

This service will provide functionality to Create, Update or Remove EntityRelationships to a Virtual
Collection.
The application is built with Spring Boot and uses Spring Data Elasticsearch to interact with the
Elasticsearch instance.
It also uses Spring AMQP to interact with the RabbitMQ message broker.
The application is designed to be deployed in a Kubernetes environment and can be run as a
container.

It covers two functions.

1. A new virtual collection has been created and digital specimen need to be updated.

- The application retrieves the messages from the queue
- It will call elasticSearch based on the filter in the Virtual Collection
- Add the EntityRelationship to the digital specimen
- Publish the updated digital specimen to the processing service

2. The second function will check if it needs to add an ER to any incoming Specimen

- On startup it will cache all VC's (will refresh every hour)
- For each specimen we will check with the Virtual Collection in the cache if the specimen matches
  any filters
- If the specimen matches a filter the ER to the VC will be added
- Specimen are published to the processing service

## Run locally

To run the system locally, it can be run from an IDEA.
Clone the code and fill in the application properties (see below).
The application needs a connection to a Postgres database, RabbitMQ, and Elasticsearch.

### Domain Object generation

DiSSCo uses JSON schemas to generate domain objects (e.g. Digital Specimens, Digital Media, etc)
based on the openDS specification. These files are stored in the
`/target/generated-sources/jsonschema2pojo directory`, and must be generated before running locally.
The following steps indicate how to generate these objects.

### Importing Up To-Date JSON Schemas

The JSON schemas are stored in `/resources/json-schemas`. The source of truth for JSON schemas is
the [DiSSCO Schemas Site](https://schemas.dissco.tech/schemas/fdo-type/). If the JSON schema has
changed, the changes can be downloaded using the maven runner script.

1. **Update the pom.xml**: The exec-maven-plugin in the pom indicated which version of the schema to
   download. If the version has changed, update the pom.
2. **Run the exec plugin**: Before the plugin can be run, the code must be compiled. Run the
   following in the terminal (or via the IDE interface):

```
mvn compile 
mvn exec:java
```

### Building POJOs

DiSSCo uses the [JsonSchema2Pojo](https://github.com/joelittlejohn/jsonschema2pojo) plugin to
generate domain objects based on our JSON Schemas. Once the JSON schemas have been updated, you can
run the following from the terminal (or via the IDE interface):

```
mvn clean
mvn jsonschema2pojo:generate
```

## Run as Container

The application can also be run as container.
It will require the environmental values described below.
The container can be built with the Dockerfile, which can be found in the root of the project.

## Environmental variables

The following specific properties can be configured:

```
# Database properties
spring.datasource.url=# The JDBC url to the PostgreSQL database to connect with
spring.datasource.username=# The login username to use for connecting with the database
spring.datasource.password=# The login password to use for connecting with the database

#Elasticsearch properties
elasticsearch.hostname=# The hostname of the Elasticsearch cluster
elasticsearch.port=# The port of the Elasticsearch cluster

#RabbitMQ properties
rabbitmq.exchangeName=# Default value is digital-specimen-exchange, can be overwritten
rabbitmq.routingKeyName=# Default value is digital-specimen, can be overwritten
rabbitmq.ingestion-queue-name=# Default value is virtual-collection-ingestion-queue, can be overwritten
rabbitmq.queue-name=# Default value is virtual-collection-queue, can be overwritten
spring.rabbitmq.username=# Username to connect to RabbitMQ
spring.rabbitmq.password=# Password to connect to RabbitMQ
spring.rabbitmq.host=# Hostname of RabbitMQ

#Endpoints
web.handle-endpoint=# Path to handle endpoint
web.annotation-endpoint=# Path to annotation proccessor endpoint
web.mas-endpoint=# Path to MAS endpoint

#Application Properties
application.name=# Default value DiSSCo Virtual Collection Service. The name of the application, used for provenance
application.pid=# Default value https://doi.org/10.5281/zenodo.17182153. The PID of the application, used for provenance
