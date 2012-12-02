/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by getApplication()licable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */

/**
 * @author Andres Almiray
 */
class JpaGriffonPlugin {
    // the plugin version
    String version = '0.2.1'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.1.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [:]
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-jpa-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'JPA support'
    String description = '''
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

This method is also accessible to any component through the singleton
`griffon.plugins.jpa.JpaConnector`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `JpaEnhancer.enhance(metaClassInstance, hibernateProviderInstance)`.

Configuration
-------------

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

The `withJpa()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in
`griffon-app/conf/Config.groovy`

    griffon.jpa.injectInto = ['controller', 'service']

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

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/jpa][2]

Testing
-------

The `withJpa()` dynamic method will _not_ be automatically injected during unit
testing, because addons are simply not initialized for this kind of tests.
However you can use `JpaEnhancer.enhance(metaClassInstance, jpaProviderInstance)`
where  `jpaProviderInstance` is of type `griffon.plugins.jpa.JpaProvider`. The
contract for this interface looks like this

    public interface JpaProvider {
        Object withJpa(Closure closure);
        Object withJpa(String persistenceUnit, Closure closure);
        <T> T withJpa(CallableWithArgs<T> callable);
        <T> T withJpa(String persistenceUnit, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyJpaProvider implements JpaProvider {
        Object withJpa(String persistenceUnit = 'default', Closure closure) { null }
        public <T> T withJpa(String persistenceUnit = 'default', CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            JpaEnhancer.enhance(service.metaClass, new MyJpaProvider())
            // exercise service methods
        }
    }


[1]: http://docs.oracle.com/javaee/6/api/javax/persistence/package-summary.html
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/jpa
'''
}
