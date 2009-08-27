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
package org.exoplatform.jcr.backupconsole;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: ClientTransport.java 111 2008-11-11 11:11:11Z serg $
 */
public interface ClientTransport
{

   /**
    * Execute assigned sURL using current transport and return result as byte array.
    * 
    * @param sURL String form of URL to execute.
    * @param postData data for post request.
    * @return BackupAgentResponce result.
    * @throws IOException any transport exception.
    * @throws BackupExecuteException other internal exception.
    */
   BackupAgentResponse executePOST(String sURL, String postData) throws IOException, BackupExecuteException;

   /**
    * Execute assigned sURL using current transport and return result as byte array.
    * 
    * @param sURL String form of URL to execute, GET method.
    * @return BackupAgentResponce result.
    * @throws IOException any transport exception.
    * @throws BackupExecuteException other internal exception.
    */
   BackupAgentResponse executeGET(String sURL) throws IOException, BackupExecuteException;
}
