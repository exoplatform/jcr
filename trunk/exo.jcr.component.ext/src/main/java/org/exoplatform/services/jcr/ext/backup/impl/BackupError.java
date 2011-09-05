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
package org.exoplatform.services.jcr.ext.backup.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 15.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: BackupError.java 627 2008-01-15 14:18:58Z pnedonosko $
 */
public class BackupError extends BackupMessage
{
   final List<StackTraceElement[]> stackTraces = new ArrayList<StackTraceElement[]>();

   BackupError(Throwable e)
   {
      super(e.toString());
      readStackTrace(e);
   }

   BackupError(String description, Throwable e)
   {
      super(description);
      readStackTrace(e);
   }

   BackupError(String description, List<StackTraceElement[]> clone)
   {
      super(description);
      this.stackTraces.addAll(clone);
   }

   public boolean isError()
   {
      return true;
   }

   private void readStackTrace(Throwable e)
   {
      this.stackTraces.add(e.getStackTrace());
      Throwable cause;
      while ((cause = e.getCause()) != null)
      {
         this.stackTraces.add(cause.getStackTrace());
      }
   }

   public void printStackTrace()
   {
      printStackTrace(System.err);
   }

   public void printStackTrace(PrintStream s)
   {
      synchronized (s)
      {
         s.println("Backup error: " + message);
         if (stackTraces.size() > 0)
         {
            StackTraceElement[] ourTrace = stackTraces.get(0);
            for (int i = 0; i < ourTrace.length; i++)
               s.println("\tat " + ourTrace[i]);

            for (int i = 1; i < stackTraces.size(); i++)
            {
               printStackTraceAsCause(s, ourTrace, stackTraces.get(i));
            }
         }
      }
   }

   private void printStackTraceAsCause(PrintStream s, StackTraceElement[] ourTrace, StackTraceElement[] causedTrace)
   {
      // Compute number of frames in common between this and caused
      int m = ourTrace.length - 1, n = causedTrace.length - 1;
      while (m >= 0 && n >= 0 && ourTrace[m].equals(causedTrace[n]))
      {
         m--;
         n--;
      }

      int framesInCommon = ourTrace.length - 1 - m;

      s.println("Caused by: " + this);
      for (int i = 0; i <= m; i++)
         s.println("\tat " + ourTrace[i]);
      if (framesInCommon != 0)
         s.println("\t... " + framesInCommon + " more");
   }
}
