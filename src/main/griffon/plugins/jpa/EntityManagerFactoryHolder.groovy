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

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import griffon.util.CallableWithArgs
import static griffon.util.GriffonNameUtils.isBlank

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.EntityManagerFactory
import javax.persistence.EntityManager

/**
 * @author Andres Almiray
 */
@Singleton
class EntityManagerFactoryHolder implements JpaProvider {
    private static final Logger LOG = LoggerFactory.getLogger(EntityManagerFactoryHolder)
    private static final Object[] LOCK = new Object[0]
    private final Map<String, Map<String, Object>> factories = [:]
  
    String[] getPersistenceUnitNames() {
        List<String> persistenceUnits = new ArrayList().addAll(factories.keySet())
        persistenceUnits.toArray(new String[persistenceUnits.size()])
    }

    Map<String, Object> getEntityManager(String persistenceUnit = 'default') {
        if(isBlank(persistenceUnit)) persistenceUnit = 'default'
        retrieveEntityManager(persistenceUnit)
    }

    void setEntityManager(String persistenceUnit = 'default', Map<String, Object> entityManager) {
        if(isBlank(persistenceUnit)) persistenceUnit = 'default'
        storeEntityManager(persistenceUnit, entityManager)       
    }

    Object withJpa(String persistenceUnit = 'default', Closure closure) {
        Map<String, Object> config = fetchEntityManager(persistenceUnit)
        if(LOG.debugEnabled) LOG.debug("Executing statement on persistenceUnit '$persistenceUnit'")
        EntityManager em = openEntityManager(config)
        try {
            return closure(persistenceUnit, em)
        } finally {
            em.close()
        }
    }

    public <T> T withJpa(String persistenceUnit = 'default', CallableWithArgs<T> callable) {
        Map<String, Object> config = fetchEntityManager(persistenceUnit)
        if(LOG.debugEnabled) LOG.debug("Executing statement on persistenceUnit '$persistenceUnit'")
        EntityManager em = openEntityManager(config)
        try {
            callable.args = [persistenceUnit, em] as Object[]
            return callable.call()
        } finally {
            em.close()
        }
    }
    
    boolean isEntityManagerConnected(String persistenceUnit) {
        if(isBlank(persistenceUnit)) persistenceUnit = 'default'
        retrieveEntityManager(persistenceUnit) != null
    }
    
    void disconnectEntityManager(String persistenceUnit) {
        if(isBlank(persistenceUnit)) persistenceUnit = 'default'
        storeEntityManager(persistenceUnit, null)        
    }

    private EntityManager openEntityManager(Map<String, Object> config) {
        config.factory.createEntityManager(config.entityManager)
    }

    private Map<String, Object> fetchEntityManager(String persistenceUnit) {
        if(isBlank(persistenceUnit)) persistenceUnit = 'default'
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
