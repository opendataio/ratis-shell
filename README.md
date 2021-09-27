# ratis-shell
A project provide a shell to talk to ratis server

# how to build
```Console
mvn clean package
```

# how to run

- Prepare a config file
```Console
cp ./conf/ratis-shell-site.properties.template ./conf/ratis-shell-site.properties
```

- Use ratis-shell by shell script
```Console
bin/ratis sh elect -peers localhost:19200,localhost:19201,localhost:19202 -address localhost:19201 -groupid 02511d47-d67c-49a3-9011-abb3109a44c1
# or if you set ratis-shell-site.properties config file
bin/ratis sh elect -serviceid alluxio-master -address localhost:19201
```

- Use ratis-shell by jar

```Console
java -jar ./target/ratis-shell-1.0.0-jar-with-dependencies.jar  elect -peers localhost:19200,localhost:19201,localhost:19202 -address localhost:19201
# or if you set ratis-shell-site.properties config file
java -Dratis.shell.home=./ -jar ./target/ratis-shell-1.0.0-jar-with-dependencies.jar elect -serviceid alluxio-master -address localhost:19201
```

# wiki

https://github.com/opendataio/ratis-shell/wiki