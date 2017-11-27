For running WebLogic tests you need to have WLS running on port 8280 on your local machine.

## Running WLS server 

In this example we are using docker container of WLS running with this command:
```bash
docker run -d \
--name="wls-server" \
-p 8280:7001 \
-v /tmp:/tmp \
docker-registry.engineering.redhat.com/keycloak/weblogic:12.2.1.2
```

- We need to map 8280 port to port 7001 in docker as Weblogic server is running on this port
- Also we need to map /tmp directory to /tmp directory in docker. This way arquillian will move archives used in testsuite to docker filesystem so that they are deployed to WLS
- You need copy few files from docker image to you filesystem, as arquillian need them so that it is able to deploy apps. Make sure user which is running the tests has access privileges to these files. 
```bash
docker cp wls-server:/u01/oracle/wlserver/server/lib/weblogic.jar ${wl-home-path}/server/lib/
docker cp wls-server:/u01/oracle/wlserver/server/lib/wlclient.jar ${wl-home-path}/server/lib/
docker cp wls-server:/u01/oracle/wlserver/server/lib/wljmxclient.jar ${wl-home-path}/server/lib/
```
- And also our image always create new admin password when starting weblogic so you need to find out what password it generated
```bash
docker logs wls-server | grep password
```
## Running tests

1. At first we need to add our custom arquillian remote adapter to local repository. Only custom change is to always store tmp files in /tmp
```bash
git clone https://github.com/mhajas/arquillian-container-wls.git
cd arquillian-container-wls/wls-common
mvn clean install -DskipTests [-Dmaven.repo.local=/custom/repo/path]
cd ../wls-remote-12.1.x
mvn clean install -DskipTests [-Dmaven.repo.local=/custom/repo/path]
```

2. Build testsuite-arquillian
```bash
mvn clean install -f testsuite/integration-arquillian/pom.xml -DskipTests=true
```
3. Run tests
```bash
mvn clean install -f testsuite/integration-arquillian/tests/other/pom.xml -Papp-server-wls -Dwl.password=${password-got-from-wls} -Dwl.home=${wl-home-path}
```