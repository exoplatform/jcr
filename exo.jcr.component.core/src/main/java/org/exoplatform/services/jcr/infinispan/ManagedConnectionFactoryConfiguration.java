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

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;

/**
 * ManagedConnectionFactoryConfiguration.
 */
@BuiltBy(ManagedConnectionFactoryConfigurationBuilder.class)
public class ManagedConnectionFactoryConfiguration  implements ConnectionFactoryConfiguration
{
    private final String jndiUrl;

    ManagedConnectionFactoryConfiguration(String jndiUrl) {
        this.jndiUrl = jndiUrl;
    }

    public String jndiUrl() {
        return jndiUrl;
    }

    @Override
    public Class<? extends ConnectionFactory> connectionFactoryClass() {
        return ManagedConnectionFactory.class;
    }

    @Override
    public String toString() {
        return "ManagedConnectionFactoryConfiguration [jndiUrl=" + jndiUrl + "]";
    }
}
