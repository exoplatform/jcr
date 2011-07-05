package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.datamodel.ItemData;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created to avoid huge operations on PropertyImpl instance initialization, 
 * as they are not needed for trail audit.
 * 
 * @author <a href="mailto:dmi3.kuleshov@gmail.com">Dmitry Kuleshov</a>
 * @version $Id: $
 */
public class AuditPropertyImpl extends PropertyImpl
{

   AuditPropertyImpl(ItemData data, SessionImpl session) throws RepositoryException, ConstraintViolationException
   {
      super(data, session);
   }

   /**
    * The most expensive method. In parent class it fulfills useless operations
    * in context of trail audit.
    *
    * {@inheritDoc}
    */
   @Override
   void loadData(ItemData data) throws RepositoryException
   {
      this.qpath = data.getQPath();
      return;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemDefinitionData getItemDefinitionData()
   {
      throw new UnsupportedOperationException("getItemDefinitionData method is not supported by this class");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PropertyDefinition getDefinition()
   {
      throw new UnsupportedOperationException("getItemDefinitionData method is not supported by this class");
   }

}
