Building
========

* mvn clean package

Note:
  The following parameter can be added to skip compiling and running tests:

     -Dmaven.test.skip=true

  For example:

      mvn -Dmaven.test.skip=true clean package


Dependencies
============

Because the application is deployed as a 'lib' directory with JARs, any changes
in the dependency version requires a corresponding update to the RPM spec file.


Configuring and Running
=======================

Two command-line utilities have been provided -- one for encoding and one for
decoding records into / from field-stripes.

Encoding:

    java -classpath field-stripe-dev-1.0.0-SNAPSHOT.jar net.agkn.field_stripe.FileRecordEncoder \
        <IDL base path> <fully-qualified message definition> <JSON input filename> <output field-stripe path>

Decoding:

    java -classpath field-stripe-dev-1.0.0-SNAPSHOT.jar net.agkn.field_stripe.FileRecordDecoder \
        <IDL base path> <fully-qualified message definition> <field-stripe path> [<output filename>]
