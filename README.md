Keycloak
========

Keycloak is an SSO Service for web apps and REST services. For more information visit [http://keycloak.org](http://keycloak.org).  


Building
--------

Ensure you have JDK 8 (or newer), Maven 3.2.1 (or newer) and Git installed

    java -version
    mvn -version
    git --version
    
First clone the Keycloak repository:
    
    git clone https://github.com/keycloak/keycloak.git
    cd keycloak
    
To build Keycloak run:

    mvn install
    
This will build all modules and run the testsuite. 

To build the distribution run:

    mvn install -Pdistribution
    
Once completed you will find distribution archives in `distribution`.


Starting Keycloak
-----------------

To start Keycloak during development first build as specficied above, then run:

    mvn -f testsuite/integration/pom.xml exec:java -Pkeycloak-server 


To start Keycloak from the appliance distribution first build the distribution it as specified above, then run:

    tar xfz distribution/appliance-dist/target/keycloak-appliance-dist-all-<VERSION>.tar.gz
    cd keycloak-appliance-dist-all-<VERSION>/keycloak
    bin/standalone.sh
    
To stop the server press `Ctrl + C`.


Contributing
------------

* See [Hacking on Keycloak](misc/HackingOnKeycloak.md)


Documentation
-------------

* [User Guide, Admin REST API and Javadocs](http://keycloak.jboss.org/docs)
* Developer documentation
    * [Hacking on Keycloak](misc/HackingOnKeycloak.md) - how to become a Keycloak contributor
    * [Testsuite](misc/Testsuite.md) - details about testsuite, but also how to quickly run Keycloak during development and a few test tools (OTP generation, LDAP server, Mail server)
    * [Database Testing](misc/DatabaseTesting.md) - how to do testing of Keycloak on different databases
    * [Updating Database](misc/UpdatingDatabaseSchema.md) - how to change the Keycloak database
    * [Release Process](misc/ReleaseProcess.md) - how to release Keycloak


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)