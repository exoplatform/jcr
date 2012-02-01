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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br>
 * Class provides CND grammar manipulation tool for reading node type
 * definitions and namespaces from stream.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class CNDStreamReader
{

   /**
    * RegistryImpl instance
    */
   private NamespaceRegistryImpl namespaceRegistry;

   /**
    * Constructs instance of CNDStreamReader. Instance of NamespaceRegistryImpl
    * is used to parse strings as InternalQnames.
    * 
    * @param namespaceRegistry
    */
   public CNDStreamReader(NamespaceRegistryImpl namespaceRegistry)
   {
      this.namespaceRegistry = namespaceRegistry;
   }

   /**
    * Method which reads input stream as compact node type definition string. If
    * any namespaces are placed in stream they are registered through namespace
    * registry.
    * 
    * @param is {@link InputStream} to read from.
    * @return List of parsed nodetypes
    * @throws RepositoryException Exception is thrown when wrong input is
    *           provided in stream or any {@link IOException} are repacked to
    *           {@link RepositoryException}
    */
   public List<NodeTypeData> read(InputStream is) throws RepositoryException
   {
      try
      {
         if (is != null)
         {
            /** Lexing input stream */
            CNDLexer lex = new CNDLexer(new ANTLRInputStream(is));
            CommonTokenStream tokens = new CommonTokenStream(lex);
            /** Parsing input stream */
            CNDParser parser = new CNDParser(tokens);
            CNDParser.cnd_return r;
            /** Throw exception if any lex errors found */
            if (lex.hasError())
            {
               throw new RepositoryException("Lexer errors found " + lex.getErrors().toString());
            }
            r = parser.cnd();
            /** Throw exception if any parse errors found */
            if (parser.hasError())
            {
               throw new RepositoryException("Parser errors found " + parser.getErrors().toString());
            }
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(r.getTree());
            CNDWalker walker = new CNDWalker(nodes);
            /**
             * Running tree walker to build nodetypes. Namespace registry is
             * provided to register namespaced mentioned in stream and
             * locationFactory is used to parse JCR names
             */
            walker.cnd(namespaceRegistry);
            return walker.getNodeTypes();
         }
         else
         {
            return new ArrayList<NodeTypeData>();
         }

      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (RecognitionException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

}
