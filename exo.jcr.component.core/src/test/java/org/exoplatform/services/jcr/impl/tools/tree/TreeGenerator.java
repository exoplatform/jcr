/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.tools.tree;

import org.exoplatform.services.jcr.impl.tools.tree.generator.NodeGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TreeGenerator.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TreeGenerator
{
   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.TreeGenerator");

   private final Node root;

   private final NodeGenerator nodegenerator;

   public TreeGenerator(Node root, NodeGenerator nodegenerator)
   {
      this.root = root;
      this.nodegenerator = nodegenerator;
   }

   public void genereteTree() throws RepositoryException
   {
      long startTime = System.currentTimeMillis();
      nodegenerator.genereteTree(root);
      root.save();
      log.info("Tree generete by " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
   }

}
