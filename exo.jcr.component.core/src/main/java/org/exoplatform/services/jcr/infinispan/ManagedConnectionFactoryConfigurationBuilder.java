/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.infinispan;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationChildBuilder;
import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfigurationBuilder;

/**
 * ManagedConnectionFactoryConfigurationBuilder.
 *
 */
public class ManagedConnectionFactoryConfigurationBuilder<S extends AbstractJdbcStoreConfigurationBuilder<?, S>> extends AbstractJdbcStoreConfigurationChildBuilder<S>
        implements ConnectionFactoryConfigurationBuilder<ManagedConnectionFactoryConfiguration>
{

    public ManagedConnectionFactoryConfigurationBuilder(AbstractJdbcStoreConfigurationBuilder<?, S> builder) {
        super(builder);
    }

    private String jndiUrl;

    public void jndiUrl(String jndiUrl) {
        this.jndiUrl = jndiUrl;
    }

    @Override
    public void validate() {
        if (jndiUrl == null) {
            throw new CacheConfigurationException("The jndiUrl has not been specified");
        }
    }

    @Override
    public void validate(GlobalConfiguration globalConfig) {
    }

    @Override
    public ManagedConnectionFactoryConfiguration create() {
        return new ManagedConnectionFactoryConfiguration(jndiUrl);
    }

    @Override
    public ManagedConnectionFactoryConfigurationBuilder<S> read(ManagedConnectionFactoryConfiguration template) {
        this.jndiUrl = template.jndiUrl();
        return this;
    }
}
