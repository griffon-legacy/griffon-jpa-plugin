/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.jpa

import javax.persistence.Persistence
import javax.persistence.EntityManagerFactory

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.CallableWithArgs
import griffon.util.ConfigUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
final class JpaConnector implements JpaProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JpaConnector)

    private bootstrap

    Object withJpa(String persistenceUnit = 'default', Closure closure) {
        return EntityManagerFactoryHolder.instance.withJpa(persistenceUnit, closure)
    }

    public <T> T withJpa(String persistenceUnit = 'default', CallableWithArgs<T> callable) {
        return EntityManagerFactoryHolder.instance.withJpa(persistenceUnit, callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        ConfigUtils.loadConfigWithI18n('JpaConfig')
    }

    private ConfigObject narrowConfig(ConfigObject config, String persistenceUnit) {
        return persistenceUnit == 'default' ? config.persistenceUnit : config.persistenceUnits[persistenceUnit]
    }

    Map<String, Object> connect(GriffonApplication app, ConfigObject config, String persistenceUnit = 'default') {
        if (EntityManagerFactoryHolder.instance.isEntityManagerConnected(persistenceUnit)) {
            return EntityManagerFactoryHolder.instance.getEntityManager(persistenceUnit)
        }

        config = narrowConfig(config, persistenceUnit)
        app.event('JpaConnectStart', [config, persistenceUnit])
        Map<String, Object> emConfig = createEntityManager(config)
        EntityManagerFactoryHolder.instance.setEntityManager(persistenceUnit, emConfig)
        bootstrap = app.class.classLoader.loadClass('BootstrapJpa').newInstance()
        bootstrap.metaClass.app = app
        EntityManagerFactoryHolder.instance.withJpa(persistenceUnit) { pu, em -> bootstrap.init(pu, em) }
        app.event('JpaConnectEnd', [persistenceUnit, emConfig.factory])
        emConfig
    }

    void disconnect(GriffonApplication app, ConfigObject config, String persistenceUnit = 'default') {
        if (EntityManagerFactoryHolder.instance.isEntityManagerConnected(persistenceUnit)) {
            config = narrowConfig(config, persistenceUnit)
            Map<String, Object> emConfig = EntityManagerFactoryHolder.instance.getEntityManager(persistenceUnit)
            app.event('JpaDisconnectStart', [config, persistenceUnit, emConfig.factory])
            EntityManagerFactoryHolder.instance.withJpa(persistenceUnit) { pu, em -> bootstrap.destroy(pu, em) }
            destroyEntityManager(config, emConfig.factory)
            EntityManagerFactoryHolder.instance.disconnectEntityManager(persistenceUnit)
            app.event('JpaDisconnectEnd', [config, persistenceUnit])
        }
    }

    Map<String, Object> createEntityManager(ConfigObject config, String persistenceUnit = 'default') {
        [
            factory: Persistence.createEntityManagerFactory(persistenceUnit, config.factory ?: [:]),
            entityManager: config.entityManager ?: [:]
        ]
    }

    void destroyEntityManager(ConfigObject config, EntityManagerFactory entityManagerFactory) {
        entityManagerFactory.close()
    }
}
