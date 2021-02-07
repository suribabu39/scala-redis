# Distributed Redis Cluster in Scala 

## Key features

- Connects to redis-cluster
- Executes SET, GET, EXPIRE commands over any provided container

## Requirements

- java
- sbt (get it at http://www.scala-sbt.org/)
- redis v6.0.10 (able to run in standalone, cluster mode)

## How to run?

## Generate Inputs
Generate text files (.txt format) with redis commands(SET, GET, EXPIRE) and ave it in a directory and pass it to application through system variable.

`USE <node>` can be used to specify through which node below commands to be executed. 
`<node>` will be name of the node with prefix as `node` and index of containser. Eg: node1, node2

Sample file:

```
USE node1
SET key10 value10
GET key10
EXPIRE key10 1
```

Refer to `PROJECT_ROOT/files/` directory for sample files.

### Using jar
1. Start the redis cluster 
2. Configure the redis nodes in `$PROJECT_ROOT/src/main/resources/application.conf`
    ```conf
    connections=["127.0.0.1:30001","127.0.0.1:30002","127.0.0.1:30003"]
    ```
3. Build jar `sbt clean assembly`
4. Run the jar 
    ```bash
    java -cp target/scala-2.12/RedisClient-assembly-0.1.jar  -DinputDir=<directory-path> -Dconcurrency=<int>  "com.Main"
    # inputDir - directory with txt files having redis commands
    # concurrency - thread pool size for executing files
    ```


### Using sbt 

```
sbt "runMain com.Main" -DinputDir=<directory-path> -Dconcurrency=<int>
```