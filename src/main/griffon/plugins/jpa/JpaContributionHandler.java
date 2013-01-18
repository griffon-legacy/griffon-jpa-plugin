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

/**
 * @author Andres Almiray
 */
public interface JpaContributionHandler {
    void setJpaProvider(JpaProvider provider);

    JpaProvider getJpaProvider();

    <R> R withJpa(Closure<R> closure);

    <R> R withJpa(String persistenceUnit, Closure<R> closure);

    <R> R withJpa(CallableWithArgs<R> callable);

    <R> R withJpa(String persistenceUnit, CallableWithArgs<R> callable);
}