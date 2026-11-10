/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.repository.creation;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.exoplatform.services.ispn.ClusterJavaSerializationMarshaller;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.impl.AbstractRPCService.MessageBody;

/**
 * One request and one reply of each command the repository creation service registers, built as its callers and its
 * execution build them, through the marshaller of the cluster; then the failure its execution throws.
 */
public class TestRepositoryCreationCommandPayloads extends TestCase
{
   private static final String PREFIX = "org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl";

   private static final String ENTRY = "{\"name\":\"repository\",\"defaultWorkspace\":\"collaboration\"}";

   private final ClusterJavaSerializationMarshaller marshaller = new ClusterJavaSerializationMarshaller();

   public void testEachCommandMakesTheRoundTrip() throws Exception
   {
      DBCreationProperties creation = new DBCreationProperties("jdbc:mysql://localhost:3306/", Map.of("driverClassName",
         "com.mysql.cj.jdbc.Driver"), "conf/jcr/db-script.sql", "exo", "secret");
      List<Object[]> commands =
         List.of(command(PREFIX + "-reserveRepositoryName", "repository-token", "repository"),
                 command(PREFIX + "-createRepository", null, "backup-id", ENTRY, "repository-token", creation),
                 // The creation properties are optional
                 command(PREFIX + "-createRepository", null, "backup-id", ENTRY, "repository-token", null),
                 command(PREFIX + "-startRepository", null, ENTRY, creation),
                 command(PREFIX + "-removeRepository", null, "repository", Boolean.FALSE),
                 command(PREFIX + "-checkRepositoryInUse", Boolean.FALSE, "repository"));

      for (Object[] command : commands)
      {
         String id = (String)command[0];
         Serializable[] args = (Serializable[])command[2];
         MessageBody request = (MessageBody)marshaller.objectFromByteBuffer(marshaller.objectToByteBuffer(
            new MessageBody(null, id, args), null, () -> id));
         assertEquals(id, request.getCommandId());
         assertEquals(id, args.length, request.getArgs().length);
         for (int i = 0; i < args.length; i++)
         {
            if (args[i] instanceof DBCreationProperties)
            {
               DBCreationProperties received = (DBCreationProperties)request.getArgs()[i];
               assertEquals(id, creation.getServerUrl(), received.getServerUrl());
               assertEquals(id, creation.getConnProps(), received.getConnProps());
            }
            else
            {
               assertEquals(id, args[i], request.getArgs()[i]);
            }
         }
         assertEquals(id, command[1], marshaller.objectFromByteBuffer(marshaller.objectToByteBuffer(command[1])));
      }
   }

   public void testTheFailureOfACommandReachesTheCallerWithItsType() throws Exception
   {
      RPCException failure = new RPCException("Cannot execute the command " + PREFIX + "-reserveRepositoryName",
         new RepositoryCreationException("Repository name repository already reserved."));

      Object received = marshaller.objectFromByteBuffer(marshaller.objectToByteBuffer(failure));

      assertTrue(String.valueOf(received), received instanceof RPCException);
      assertTrue(((Throwable)received).getCause() instanceof RepositoryCreationException);
      assertEquals("Repository name repository already reserved.", ((Throwable)received).getCause().getMessage());
   }

   private static Object[] command(String id, Serializable reply, Serializable... args)
   {
      return new Object[] {id, reply, args};
   }
}
