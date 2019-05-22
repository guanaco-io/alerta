Release/Publish the Alerta project bundles
------------------------------------------

## Local

Publish to the local maven repository:
```sbtshell
sbt:alerta>+ publishM2
```

## Remote

Create a credentials file `${user.home}/.bintray/.credentials` and provide the correct data:

```properties
realm=Bintray API Realm @ api.bintray.com
host=api.bintray.com
user=<username>
password=<api-key>

```

Publish all supported scala versions to the repo (only release versions are allowed):
```sbtshell
sbt:alerta>+ publish
```