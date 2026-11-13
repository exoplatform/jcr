package org.exoplatform.services.jcr.infinispan;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.persistence.jdbc.common.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.common.connectionfactory.ConnectionFactory;

/**
 * ManagedConnectionFactoryConfiguration.
 */
@BuiltBy(ManagedConnectionFactoryConfigurationBuilder.class)
public class ManagedConnectionFactoryConfiguration  implements ConnectionFactoryConfiguration
{
    static final AttributeDefinition<String> JNDI_URL = AttributeDefinition.builder("jndiUrl", null, String.class)
                                                                         .immutable()
                                                                         .build();

    private final AttributeSet attributes;

    ManagedConnectionFactoryConfiguration(AttributeSet attributes) {
        this.attributes = attributes.checkProtection();
    }

    static AttributeSet attributeSet() {
        return new AttributeSet(ManagedConnectionFactoryConfiguration.class, JNDI_URL);
    }

    public String jndiUrl() {
        return attributes.attribute(JNDI_URL).get();
    }

    @Override
    public Class<? extends ConnectionFactory> connectionFactoryClass() {
        return ManagedConnectionFactory.class;
    }

    @Override
    public AttributeSet attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "ManagedConnectionFactoryConfiguration [jndiUrl=" + jndiUrl() + "]";
    }
}
