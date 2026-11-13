package org.exoplatform.services.jcr.infinispan;

import static org.exoplatform.services.jcr.infinispan.ManagedConnectionFactoryConfiguration.JNDI_URL;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationChildBuilder;
import org.infinispan.persistence.jdbc.common.configuration.ConnectionFactoryConfigurationBuilder;

/**
 * ManagedConnectionFactoryConfigurationBuilder.
 *
 */
public class ManagedConnectionFactoryConfigurationBuilder<S extends AbstractJdbcStoreConfigurationBuilder<?, S>> extends AbstractJdbcStoreConfigurationChildBuilder<S>
        implements ConnectionFactoryConfigurationBuilder<ManagedConnectionFactoryConfiguration>
{

    private final AttributeSet attributes;

    public ManagedConnectionFactoryConfigurationBuilder(AbstractJdbcStoreConfigurationBuilder<?, S> builder) {
        super(builder);
        this.attributes = ManagedConnectionFactoryConfiguration.attributeSet();
    }

    public void jndiUrl(String jndiUrl) {
        attributes.attribute(JNDI_URL).set(jndiUrl);
    }

    @Override
    public AttributeSet attributes() {
        return attributes;
    }

    @Override
    public void validate() {
        if (attributes.attribute(JNDI_URL).get() == null) {
            throw new CacheConfigurationException("The jndiUrl has not been specified");
        }
    }

    @Override
    public ManagedConnectionFactoryConfiguration create() {
        return new ManagedConnectionFactoryConfiguration(attributes.protect());
    }

    @Override
    public ManagedConnectionFactoryConfigurationBuilder<S> read(ManagedConnectionFactoryConfiguration template,
                                                               Combine combine) {
        attributes.read(template.attributes(), combine);
        return this;
    }
}
