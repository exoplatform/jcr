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
package org.exoplatform.services.jcr.ext.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.jibx.runtime.BindingDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads backup chain logs and the configurations of their backup set, as a
 * restore does.
 */
public class TestBackupChainLogParsing
{
   private static final String REFUSAL = "External DTD or entity not allowed";

   private Path dir;

   private String dtdUri;

   private RepositoryServiceConfiguration configuration;

   @Before
   public void setUp() throws Exception
   {
      dir = Files.createTempDirectory("backup-chain-log");
      Path dtd = dir.resolve("external.dtd");
      Files.write(dtd, "<!ENTITY m \"MARKER-EXO-92938\">".getBytes(StandardCharsets.UTF_8));
      dtdUri = dtd.toUri().toString();
      try (InputStream config = getClass().getResourceAsStream("/conf/standalone/test-jcr-ext-config.xml"))
      {
         configuration =
            (RepositoryServiceConfiguration)BindingDirectory.getFactory(RepositoryServiceConfiguration.class)
               .createUnmarshallingContext().unmarshalDocument(config, null);
      }
   }

   @After
   public void tearDown() throws Exception
   {
      try (Stream<Path> files = Files.walk(dir))
      {
         files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
   }

   @Test
   public void testWorkspaceBackupLogIsRead() throws Exception
   {
      BackupChainLog log = new BackupChainLog(new File(workspaceBackupLog().getLogFilePath()));

      assertEquals("ws1", log.getOriginalWorkspaceEntry().getName());
      assertEquals("db1", log.getBackupConfig().getRepository());
   }

   @Test
   public void testWorkspaceBackupLogWithExternalDtdIsRefused() throws Exception
   {
      File logFile = new File(workspaceBackupLog().getLogFilePath());
      addExternalDtd(logFile);

      assertRefused(() -> new BackupChainLog(logFile));
   }

   @Test
   public void testWorkspaceBackupConfigurationWithExternalDtdIsRefused() throws Exception
   {
      File logFile = new File(workspaceBackupLog().getLogFilePath());
      addExternalDtd(new File(dir.toFile(), referencedFile(logFile, "original-workspace-config")));

      assertRefused(() -> new BackupChainLog(logFile));
   }

   @Test
   public void testRepositoryBackupLogIsRead() throws Exception
   {
      RepositoryBackupChainLog log =
         new RepositoryBackupChainLog(new File(repositoryBackupLog().getLogFilePath()));

      assertEquals("db1", log.getOriginalRepositoryEntry().getName());
      assertEquals("ws", log.getSystemWorkspace());
   }

   @Test
   public void testRepositoryBackupLogWithExternalDtdIsRefused() throws Exception
   {
      File logFile = new File(repositoryBackupLog().getLogFilePath());
      addExternalDtd(logFile);

      assertRefused(() -> new RepositoryBackupChainLog(logFile));
   }

   @Test
   public void testRepositoryBackupConfigurationWithExternalDtdIsRefused() throws Exception
   {
      File logFile = new File(repositoryBackupLog().getLogFilePath());
      addExternalDtd(new File(dir.toFile(), referencedFile(logFile, "original-repository-config")));

      assertRefused(() -> new RepositoryBackupChainLog(logFile));
   }

   private BackupChainLog workspaceBackupLog() throws Exception
   {
      BackupConfig config = new BackupConfig();
      config.setRepository("db1");
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(dir.toFile());
      BackupChainLog log =
         new BackupChainLog(dir.toFile(), config, "full", "incremental", "ws-backup", configuration, dir.toFile());
      log.addJobEntry(new FinishedFullBackupJob(dir.toUri().toURL()));
      log.endLog();
      return log;
   }

   private RepositoryBackupChainLog repositoryBackupLog() throws Exception
   {
      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository("db1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(dir.toFile());
      RepositoryBackupChainLog log =
         new RepositoryBackupChainLog(dir.toFile(), config, "full", "incremental", "ws", new ArrayList<String>(),
            "repository-backup", Calendar.getInstance(), configuration.getRepositoryConfiguration("db1"),
            configuration);
      log.endLog();
      return log;
   }

   private void addExternalDtd(File xml) throws Exception
   {
      String content = new String(Files.readAllBytes(xml.toPath()), StandardCharsets.UTF_8);
      int prolog = content.indexOf("?>") + 2;
      content = content.substring(0, prolog) + "<!DOCTYPE root SYSTEM '" + dtdUri + "'>" + content.substring(prolog);
      Files.write(xml.toPath(), content.getBytes(StandardCharsets.UTF_8));
   }

   private String referencedFile(File logFile, String element) throws Exception
   {
      String content = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
      Matcher matcher = Pattern.compile("<" + element + ">([^<]+)</" + element + ">").matcher(content);
      assertTrue(element + " not found in the log", matcher.find());
      return matcher.group(1);
   }

   private void assertRefused(LogRead read)
   {
      try
      {
         read.run();
         fail("Backup log read");
      }
      catch (BackupOperationException e)
      {
         assertTrue(causeMessages(e), causeMessages(e).contains(REFUSAL));
      }
   }

   private static String causeMessages(Throwable e)
   {
      StringBuilder messages = new StringBuilder();
      for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause())
      {
         messages.append(t.getMessage()).append('\n');
         if (t instanceof org.jibx.runtime.JiBXException && ((org.jibx.runtime.JiBXException)t).getRootCause() != null)
         {
            messages.append(causeMessages(((org.jibx.runtime.JiBXException)t).getRootCause()));
         }
      }
      return messages.toString();
   }

   private interface LogRead
   {
      void run() throws BackupOperationException;
   }

   private static class FinishedFullBackupJob implements BackupJob
   {
      private final URL storageURL;

      FinishedFullBackupJob(URL storageURL)
      {
         this.storageURL = storageURL;
      }

      public int getType()
      {
         return FULL;
      }

      public int getState()
      {
         return FINISHED;
      }

      public int getId()
      {
         return 0;
      }

      public URL getStorageURL()
      {
         return storageURL;
      }

      public void stop()
      {
      }

      public void init(ManageableRepository repository, String workspaceName, BackupConfig config,
         Calendar timeStamp)
      {
      }

      public void addListener(BackupJobListener listener)
      {
      }

      public void removeListener(BackupJobListener listener)
      {
      }

      public void run()
      {
      }
   }
}
