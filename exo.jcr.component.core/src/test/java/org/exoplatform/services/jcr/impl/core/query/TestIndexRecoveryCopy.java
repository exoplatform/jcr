/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.services.jcr.impl.core.query;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.core.query.lucene.MultiIndex;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;

/**
 * The copy of an index another node asks for is made of the index's files: the
 * changes still in the volatile index are written to them first.
 */
public class TestIndexRecoveryCopy extends TestCase
{
   private File indexDirectory;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      indexDirectory = Files.createTempDirectory("index-to-copy").toFile();
      Files.writeString(new File(indexDirectory, "indexes").toPath(), "segments");
   }

   @Override
   protected void tearDown() throws Exception
   {
      for (File file : indexDirectory.listFiles())
      {
         Files.delete(file.toPath());
      }
      Files.delete(indexDirectory.toPath());
      super.tearDown();
   }

   public void testTheCopyIsPreparedAfterTheVolatileIndexIsFlushed() throws Throwable
   {
      MultiIndex index = mock(MultiIndex.class);
      RemoteCommand prepare = prepareIndexesForCopy(searchManager(index, IndexerIoMode.READ_WRITE));

      Serializable zipName = prepare.execute(new Serializable[0]);

      verify(index).flush();
      File zip = new File(System.getProperty("java.io.tmpdir"), (String)zipName);
      assertTrue(zip.exists());
      Files.delete(zip.toPath());
   }

   public void testAReadOnlyIndexIsCopiedWithoutAFlush() throws Throwable
   {
      MultiIndex index = mock(MultiIndex.class);
      RemoteCommand prepare = prepareIndexesForCopy(searchManager(index, IndexerIoMode.READ_ONLY));

      Serializable zipName = prepare.execute(new Serializable[0]);

      verify(index, never()).flush();
      Files.delete(new File(System.getProperty("java.io.tmpdir"), (String)zipName).toPath());
   }

   public void testTheCopyIsRefusedWhenTheVolatileIndexCannotBeFlushed() throws Throwable
   {
      MultiIndex index = mock(MultiIndex.class);
      doThrow(new IOException("disk full")).when(index).flush();
      RemoteCommand prepare = prepareIndexesForCopy(searchManager(index, IndexerIoMode.READ_WRITE));

      try
      {
         prepare.execute(new Serializable[0]);
         fail("A copy that lacks the volatile index must be refused");
      }
      catch (IOException e)
      {
         assertEquals("disk full", e.getMessage());
      }
   }

   private SearchManager searchManager(MultiIndex index, IndexerIoMode mode) throws Exception
   {
      SearchIndex handler = mock(SearchIndex.class);
      when(handler.getIndex()).thenReturn(index);
      when(handler.getIndexerIoModeHandler()).thenReturn(new IndexerIoModeHandler(mode));
      SearchManager searchManager = mock(SearchManager.class);
      when(searchManager.getWsId()).thenReturn("ws");
      when(searchManager.getIndexDirectory()).thenReturn(indexDirectory);
      when(searchManager.getHandler()).thenReturn(handler);
      return searchManager;
   }

   private static RemoteCommand prepareIndexesForCopy(SearchManager searchManager) throws Exception
   {
      List<RemoteCommand> commands = new ArrayList<>();
      RPCService rpcService = mock(RPCService.class);
      when(rpcService.registerCommand(any())).thenAnswer(invocation -> {
         RemoteCommand command = invocation.getArgument(0);
         commands.add(command);
         return command;
      });
      new IndexRecoveryImpl(rpcService, searchManager);
      return commands.stream()
                     .filter(command -> command.getId().contains("prepareIndexesForCopy"))
                     .findFirst()
                     .orElseThrow();
   }
}
