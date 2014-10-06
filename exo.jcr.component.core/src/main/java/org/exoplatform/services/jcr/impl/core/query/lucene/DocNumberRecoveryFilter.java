/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

import javax.jcr.RepositoryException;

/**
 * Filter asks coordinator for the number of documents in it's index. Returns true
 * is it differs with local index.
 * 
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchul</a>
 * @version $Id: DocNumberRecoveryFilter.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 */
public class DocNumberRecoveryFilter extends AbstractRecoveryFilter
{

   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.index.DocNumberRecoveryFilter");

   protected RemoteCommand getDocsNumCommand;

   protected RPCService rpcService;

   /**
    * @param searchIndex
    */
   public DocNumberRecoveryFilter(SearchIndex searchIndex)
   {
      super(searchIndex);

      rpcService = context.getRPCService();

      getDocsNumCommand = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.lucene.DocNumberRecoveryFilter-getDocNum-"
               + DocNumberRecoveryFilter.this.searchIndex.getWsId() + "-"
               + (DocNumberRecoveryFilter.this.searchIndex.getContext().getParentHandler() == null);
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            if (log.isDebugEnabled())
               log.debug("Trying to get the total amount of documents from the master");
            MultiIndex index;
            while ((index = DocNumberRecoveryFilter.this.searchIndex.getIndex()) == null ||
                   index.isStopped())
            {
               if (log.isDebugEnabled())
                  log.debug("The index is not yet ready, it will retry later");
               Thread.sleep(1000);
            }
            return Integer.valueOf(index.numDocs());
         }
      });

   }

   /**
   * {@inheritDoc}
   */
   @Override
   public boolean accept() throws RepositoryException
   {
      try
      {
         if (!rpcService.isCoordinator())
         {
            Integer docsNumber = (Integer)rpcService.executeCommandOnCoordinator(getDocsNumCommand, true);
            if (log.isDebugEnabled())
               log.debug("Remote result received: {} and local is: {}", docsNumber, searchIndex.getIndex().numDocs());
            return docsNumber.intValue() != searchIndex.getIndex().numDocs();
         }
         // if current node is coordinator
         return false;
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (RPCException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   @Override
   public void close()
   {
      rpcService.unregisterCommand(getDocsNumCommand);
      super.close();
   }

}
