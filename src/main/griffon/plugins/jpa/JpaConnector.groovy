/*
 * Copyright 2012-2013 the original author or authors.
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
import griffon.util.ConfigUtils

/**
 * @author Andres Almiray
 */
@Singleton
final class JpaConnector {
    private static final String DEFAULT = 'default'

    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.jpa) {
            app.config.pluginConfig.jpa = ConfigUtils.loadConfigWithI18n('JpaConfig')
        }
        app.config.pluginConfig.jpa
    }

    private ConfigObject narrowConfig(ConfigObject config, String persistenceUnit) {
        return persistenceUnit == DEFAULT ? config.persistenceUnit : config.persistenceUnits[persistenceUnit]
    }

    Map<String, Object> connect(GriffonApplication app, ConfigObject config, String persistenceUnit = DEFAULT) {
        if (EntityManagerFactoryHolder.instance.isEntityManagerConnected(persistenceUnit)) {
            return EntityManagerFactoryHolder.instance.getEntityManager(persistenceUnit)
        }

        config = narrowConfig(config, persistenceUnit)
        app.event('JpaConnectStart', [config, persistenceUnit])
        Map<String, Object> emConfig = createEntityManager(config)
        EntityManagerFactoryHolder.instance.setEntityManager(persistenceUnit, emConfig)
        bootstrap = app.class.classLoader.loadClass('BootstrapJpa').newInstance()
        bootstrap.metaClass.app = app
        resolveJpaProvider(app).withJpa(persistenceUnit) { pu, em -> bootstrap.init(pu, em) }
        app.event('JpaConnectEnd', [persistenceUnit, emConfig.factory])
        emConfig
    }

    void disconnect(GriffonApplication app, ConfigObject config, String persistenceUnit = DEFAULT) {
        if (EntityManagerFactoryHolder.instance.isEntityManagerConnected(persistenceUnit)) {
            config = narrowConfig(config, persistenceUnit)
            Map<String, Object> emConfig = EntityManagerFactoryHolder.instance.getEntityManager(persistenceUnit)
            app.event('JpaDisconnectStart', [config, persistenceUnit, emConfig.factory])
            resolveJpaProvider(app).withJpa(persistenceUnit) { pu, em -> bootstrap.destroy(pu, em) }
            destroyEntityManager(config, emConfig.factory)
            EntityManagerFactoryHolder.instance.disconnectEntityManager(persistenceUnit)
            app.event('JpaDisconnectEnd', [config, persistenceUnit])
        }
    }

    JpaProvider resolveJpaProvider(GriffonApplication app) {
        def jpaProvider = app.config.jpaProvider
        if (jpaProvider instanceof Class) {
            jpaProvider = jpaProvider.newInstance()
            app.config.jpaProvider = jpaProvider
        } else if (!jpaProvider) {
            jpaProvider = DefaultJpaProvider.instance
            app.config.jpaProvider = jpaProvider
        }
        jpaProvider
    }

    Map<String, Object> createEntityManager(ConfigObject config, String persistenceUnit = DEFAULT) {
        [
            factory: Persistence.createEntityManagerFactory(persistenceUnit, config.factory ?: [:]),
            entityManager: config.entityManager ?: [:]
        ]
    }

    void destroyEntityManager(ConfigObject config, EntityManagerFactory entityManagerFactory) {
        entityManagerFactory.close()
    }
}
