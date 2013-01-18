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

package griffon.plugins.jpa;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractJpaProvider implements JpaProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJpaProvider.class);
    private static final String DEFAULT = "default";

    public <R> R withJpa(Closure<R> closure) {
        return withJpa(DEFAULT, closure);
    }

    public <R> R withJpa(String persistenceUnit, Closure<R> closure) {
        if (isBlank(persistenceUnit)) persistenceUnit = DEFAULT;
        if (closure != null) {
            EntityManager em = getEntityManager(persistenceUnit);
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Executing statement on persistenceUnit '" + persistenceUnit + "'");
                }
                return closure.call(persistenceUnit, em);
            } finally {
                em.close();
            }
        }
        return null;
    }

    public <R> R withJpa(CallableWithArgs<R> callable) {
        return withJpa(DEFAULT, callable);
    }

    public <R> R withJpa(String persistenceUnit, CallableWithArgs<R> callable) {
        if (isBlank(persistenceUnit)) persistenceUnit = DEFAULT;
        if (callable != null) {
            EntityManager em = getEntityManager(persistenceUnit);
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Executing statement on persistenceUnit '" + persistenceUnit + "'");
                }
                callable.setArgs(new Object[]{persistenceUnit, em});
                return callable.call();
            } finally {
                em.close();
            }
        }
        return null;
    }

    protected abstract EntityManager getEntityManager(String persistenceUnit);
}