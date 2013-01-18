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

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import static griffon.util.GriffonNameUtils.isBlank

import javax.persistence.EntityManagerFactory

/**
 * @author Andres Almiray
 */
class EntityManagerFactoryHolder {
    private static final String DEFAULT = 'default'
    private static final Object[] LOCK = new Object[0]
    private final Map<String, Map<String, Object>> factories = [:]

    private static final EntityManagerFactoryHolder INSTANCE

    static {
        INSTANCE = new EntityManagerFactoryHolder()
    }

    static EntityManagerFactoryHolder getInstance() {
        INSTANCE
    }

    String[] getPersistenceUnitNames() {
        List<String> persistenceUnits = new ArrayList().addAll(factories.keySet())
        persistenceUnits.toArray(new String[persistenceUnits.size()])
    }

    Map<String, Object> getEntityManager(String persistenceUnit = DEFAULT) {
        if(isBlank(persistenceUnit)) persistenceUnit = DEFAULT
        retrieveEntityManager(persistenceUnit)
    }

    void setEntityManager(String persistenceUnit = DEFAULT, Map<String, Object> entityManager) {
        if(isBlank(persistenceUnit)) persistenceUnit = DEFAULT
        storeEntityManager(persistenceUnit, entityManager)
    }

    boolean isEntityManagerConnected(String persistenceUnit) {
        if(isBlank(persistenceUnit)) persistenceUnit = DEFAULT
        retrieveEntityManager(persistenceUnit) != null
    }

    void disconnectEntityManager(String persistenceUnit) {
        if(isBlank(persistenceUnit)) persistenceUnit = DEFAULT
        storeEntityManager(persistenceUnit, null)
    }

    Map<String, Object> getEntityManagerConfiguration(String persistenceUnit) {
        if(isBlank(persistenceUnit)) persistenceUnit = DEFAULT
        Map<String, Object> emConfig = retrieveEntityManager(persistenceUnit)
        if(emConfig == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = JpaConnector.instance.createConfig(app)
            emConfig = JpaConnector.instance.connect(app, config, persistenceUnit)
        }

        if(emConfig == null) {
            throw new IllegalArgumentException("No such EntityManager configuration for name $persistenceUnit")
        }
        emConfig
    }

    private Map<String, Object> retrieveEntityManager(String persistenceUnit) {
        synchronized(LOCK) {
            factories[persistenceUnit]
        }
    }

    private void storeEntityManager(String persistenceUnit, Map<String, Object> entityManager) {
        synchronized(LOCK) {
            factories[persistenceUnit] = entityManager
        }
    }
}