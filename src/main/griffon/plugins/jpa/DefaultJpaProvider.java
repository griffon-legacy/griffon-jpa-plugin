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

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;
import java.util.Map;

/**
 * @author Andres Almiray
 */
public class DefaultJpaProvider extends AbstractJpaProvider {
    private static final DefaultJpaProvider INSTANCE;

    static {
        INSTANCE = new DefaultJpaProvider();
    }

    public static DefaultJpaProvider getInstance() {
        return INSTANCE;
    }

    private DefaultJpaProvider() {}

    @Override
    protected EntityManager getEntityManager(String persistenceUnit) {
        final Map<String, Object> config = EntityManagerFactoryHolder.getInstance().getEntityManagerConfiguration(persistenceUnit);
        EntityManagerFactory factory = (EntityManagerFactory) config.get("factory");
        return factory.createEntityManager((Map) config.get("entityManager"));
    }
}
