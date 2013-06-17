
JPA support
-----------

Plugin page: [http://artifacts.griffon-framework.org/plugin/jpa](http://artifacts.griffon-framework.org/plugin/jpa)


The JPA plugin enables lightweight access to RDBMS using [JPA][1].
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in
`$appdir/griffon-app/conf`:

 * JpaConfig.groovy - contains the EntitiManager properties.
 * BootstrapJpa.groovy - defines init/destroy hooks for data to be manipulated
during app startup/shutdown.

A new dynamic method named `withJpa` will be injected into all controllers,
giving you access to a `javax.persistence.EntityManager` object, with which
you'll be able to make calls to the database. Remember to make all database
calls off the UI thread otherwise your application may appear unresponsive
when doing long computations inside the UI thread.

This method is aware of multiple databases. If no persistenceUnit is specified
when calling it then the default database will be selected. Here are two
example usages, the first queries against the default database while the second
queries a database whose name has been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withJpa { persistenceUnit, entityManager -> ... }
            withJpa('internal') { persistenceUnit, entityManager -> ... }
        }
    }

The following list enumerates all the variants of the injected method

 * `<R> R withJpa(Closure<R> stmts)`
 * `<R> R withJpa(CallableWithArgs<R> stmts)`
 * `<R> R withJpa(String jpaServerName, Closure<R> stmts)`
 * `<R> R withJpa(String jpaServerName, CallableWithArgs<R> stmts)`

These methods are also accessible to any component through the singleton
`griffon.plugins.jpa.JpaConnector`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `JpaEnhancer.enhance(metaClassInstance, jpaProviderInstance)`.

Configuration
-------------
### JpaAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.jpa.JpaAware`. This transformation injects the
`griffon.plugins.jpa.JpaContributionHandler` interface and default
behavior that fulfills the contract.

### JPA Provider

You must configure a suitable JPA provider for your application, this plugin
will not define a default implementation for you. Here's an example setting up
Eclipselink as a provider

__griffon-app/conf/BuildConfig.groovy__

        griffon.project.dependency.resolution = {
            inherits "global"
            log "warn"
            repositories {
                griffonHome()
                mavenCentral()
                mavenRepo 'http://download.eclipse.org/rt/eclipselink/maven.repo/'
            }
            dependencies {
                compile('org.eclipse.persistence:eclipselink:2.4.0-RC2') {
                    exclude 'javax.persistence'
                }
                compile('com.h2database:h2:1.3.167')
            }
        }

### Dynamic method injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.jpa.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.jpa.JpaContributionHandler`.

### Events

The following events will be triggered by this addon

 * JpaConnectStart[config, persistenceUnit] - triggered before connecting to
   the database
 * JpaConnectEnd[persistenceUnit, entityManagerFactory] - triggered after
   connecting to the database
 * JpaDisconnectStart[config, persistenceUnit, entityManagerFactory] - triggered
   before disconnecting from the database
 * JpaDisconnectEnd[config, persistenceUnit] - triggered after disconnecting
   from the database

### Multiple Persistence Units

The config file `JpaConfig.groovy` defines a default persistenceUnit block. As
the name implies this is the persistenceUnit used by default, however you can
configure named persistenceUnits by adding a new config block. For example
connecting to a persistenceUnit whose name is 'internal' can be done in this way

    persistenceUnits {
        internal {
            factory {
                // EntityManagerFactory options
            }
            entityManager {
                // EntityManager options
            }
        }
    }

This block can be used inside the `environments()` block in the same way as the
default persistenceUnit block is used.

### Configuration Storage

The plugin will load and store the contents of `JpaConfig.groovy` inside the
application's configuration, under the `pluginConfig` namespace. You may retrieve
and/or update values using

    app.config.pluginConfig.jpa

### Connect at Startup

The plugin will attempt a connection to the default database at startup. If this
behavior is not desired then specify the following configuration flag in
`Config.groovy`

    griffon.jpa.connect.onstartup = false

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/jpa][2]

Testing
-------

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`JpaEnhancer.enhance(metaClassInstance, jpaProviderInstance)` where
`jpaProviderInstance` is of type `griffon.plugins.jpa.JpaProvider`.
The contract for this interface looks like this

    public interface JpaProvider {
        <R> R withJpa(Closure<R> closure);
        <R> R withJpa(CallableWithArgs<R> callable);
        <R> R withJpa(String persistenceUnit, Closure<R> closure);
        <R> R withJpa(String persistenceUnit, CallableWithArgs<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyJpaProvider implements JpaProvider {
        public <R> R withJpa(Closure<R> closure) { null }
        public <R> R withJpa(CallableWithArgs<R> callable) { null }
        public <R> R withJpa(String persistenceUnit, Closure<R> closure) { null }
        public <R> R withJpa(String persistenceUnit, CallableWithArgs<R> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            JpaEnhancer.enhance(service.metaClass, new MyJpaProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@JpaAware` then usage
of `JpaEnhancer` should be avoided at all costs. Simply set `jpaProviderInstance`
on the service instance directly, like so, first the service definition

    @griffon.plugins.jpa.JpaAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.jpaProvider = new MyJpaProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-jpa-compile-x.y.z.jar`, with locations

 * dsdl/jpa.dsld
 * gdsl/jpa.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][3] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][3] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/jpa-<version>/dist/griffon-jpa-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:        griffon-lombok-compile-<version>.jar:griffon-jpa-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@JpaAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][4]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@JpaAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][3] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-jpa-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/jpa-<version>/dist/griffon-jpa-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@JpaAware`.


[1]: http://docs.oracle.com/javaee/6/api/javax/persistence/package-summary.html
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/jpa
[3]: /plugin/lombok
[4]: http://netbeans.org/kb/docs/java/annotations-lombok.html

### Building

This project requires all of its dependencies be available from maven compatible repositories.
Some of these dependencies have not been pushed to the Maven Central Repository, however you
can obtain them from [lombok-dev-deps][lombok-dev-deps].

Follow the instructions found there to install the required dependencies into your local Maven
repository before attempting to build this plugin.

[lombok-dev-deps]: https://github.com/aalmiray/lombok-dev-deps