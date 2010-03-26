/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.replication.recovery;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: WaitConfirmation.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class WaitConfirmation extends Thread
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.WaitConfirmation");

   /**
    * The wait timeout.
    */
   private long timeOut;

   /**
    * The RecoveryManager will be saved the ChangesLog.
    */
   private RecoveryManager recoveryManager;

   /**
    * The identifier string to ChangesLog.
    */
   private String identifier;

   /**
    * WaitConfirmation constructor.
    * 
    * @param timeOut
    *          the wait timeout
    * @param recoveryManager
    *          the RecoveryManager
    * @param identifier
    *          the identifier to ChangesLog
    */
   WaitConfirmation(long timeOut, RecoveryManager recoveryManager, String identifier)
   {
      super();
      this.timeOut = timeOut;
      this.recoveryManager = recoveryManager;
      this.identifier = identifier;

      if (log.isDebugEnabled())
         log.debug("init : " + identifier);
   }

   /**
    * {@inheritDoc}
    */
   public void run()
   {
      try
      {
         if (log.isDebugEnabled())
            log.debug("Before : getParticipantsClusterList().size():"
               + recoveryManager.getPendingConfirmationChengesLogById(identifier).getConfirmationList().size());

         Thread.sleep(timeOut);

         PendingConfirmationChengesLog confirmationChengesLog =
            recoveryManager.getPendingConfirmationChengesLogById(identifier);

         List<String> notConfirmationList = new ArrayList<String>(recoveryManager.getParticipantsClusterList());
         notConfirmationList.removeAll(confirmationChengesLog.getConfirmationList());

         if (notConfirmationList.size() > 0)
         {
            confirmationChengesLog.setNotConfirmationList(notConfirmationList);

            String fileName = recoveryManager.save(identifier);

            if (log.isDebugEnabled())
               log.debug("save : " + identifier);

            for (String ownerName : confirmationChengesLog.getConfirmationList())
               recoveryManager.removeChangesLog(identifier, ownerName);
         }
         else if (notConfirmationList.size() == 0 && confirmationChengesLog.getDataFilePath() != null)
         {
            recoveryManager.removeDataFile(new File(confirmationChengesLog.getDataFilePath()));
         }

         if (log.isDebugEnabled())
            log.debug("After : getParticipantsClusterList().size():"
               + confirmationChengesLog.getConfirmationList().size());

         recoveryManager.remove(identifier);

         if (log.isDebugEnabled())
            log.debug("remove : " + identifier);
      }
      catch (InterruptedException e)
      {
         log.error("Can't save ChangesLog", e);
      }
      catch (FileNotFoundException e)
      {
         log.error("Can't save ChangesLog", e);
      }
      catch (IOException e)
      {
         log.error("Can't save ChangesLog", e);
      }
      catch (Exception e)
      {
         log.error("Can't save ChangesLog", e);
      }
   }
}
