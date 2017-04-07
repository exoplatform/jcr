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
