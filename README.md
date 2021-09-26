# ratis-shell
A project provide a shell to talk to ratis server

# how to build
```Console
mvn clean package
```

# how to run

```Console
bin/ratis sh elect -peers localhost:19200,localhost:19201,localhost:19202 -address localhost:19201
```

Or

```Console`
java -jar ./target/ratis-shell-1.0.0-jar-with-dependencies.jar  elect -peers localhost:19200,localhost:19201,localhost:19202 -address localhost:19201 
```