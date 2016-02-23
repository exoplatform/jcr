/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.misc;

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;

import javax.jcr.RepositoryException;

/**
 * Pattern to match normalized {@link QPath}s.
 * A pattern matches either a constant path, a name of a path element, a selection of
 * either of two patterns or a sequence of two patterns. The matching process is greedy.
 * That is, whenever a match is not unique only the longest match is considered.
 * Matching consumes as many elements from the beginning of an input path as possible and
 * returns what's left as an instance of {@link MatchResult}.
 * Use the {@link java.util.regex.Matcher} class for matching a whole path or finding matches inside a path.
 */
public abstract class Pattern
{

   /**
    * Matches this pattern against the input.
    * @param input path to match with this pattern
    * @return result from the matching <code>pattern</code> against <code>input</code>
    * @throws IllegalArgumentException if <code>input</code> is not normalized
    */
   public MatchResult match(QPath input)
   {
      try
      {
         return match(new Context(input)).getMatchResult();
      }
      catch (RepositoryException e)
      {
         throw (IllegalArgumentException)new IllegalArgumentException("QPath not normalized").initCause(e);
      }
   }

   protected abstract Context match(Context input) throws RepositoryException;

   /**
    * Construct a new pattern which matches an exact path
    * @param path
    * @return A pattern which matches <code>path</code> and nothing else
    * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
    */
   public static Pattern path(QPath path)
   {
      if (path == null)
      {
         throw new IllegalArgumentException("path cannot be null");
      }
      return new PathPattern(path);
   }

   /**
    * Construct a new pattern which matches a path element of a given name
    * @param name
    * @return A pattern which matches a path element with name <code>name</code>
    * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
    */
   public static Pattern name(QPathEntry name)
   {
      if (name == null)
      {
         throw new IllegalArgumentException("name cannot be null");
      }
      return new NamePattern(name);
   }

   /**
    * Constructs a pattern which matches a path elements against regular expressions.
    * @param namespaceUri A regular expression used for matching the name space URI of
    *   a path element.
    * @param localName A regular expression used for matching the local name of a path
    *   element
    * @return  A pattern which matches a path element if namespaceUri matches the
    *   name space URI of the path element and localName matches the local name of the
    *   path element.
    * @throws IllegalArgumentException if either <code>namespaceUri</code> or
    *   <code>localName</code> is <code>null</code>
    *
    * @see java.util.regex.Pattern
    */
   public static Pattern name(String namespaceUri, String localName)
   {
      if (namespaceUri == null || localName == null)
      {
         throw new IllegalArgumentException("neither namespaceUri nor localName can be null");
      }
      return new RegexPattern(namespaceUri, localName);
   }

   private static final Pattern ALL_PATTERN = new Pattern()
   {
      protected Context match(Context input)
      {
         return input.matchToEnd();
      }

      public String toString()
      {
         return "[ALL]";
      }

   };

   /**
    * A pattern which matches all input.
    * @return
    */
   public static Pattern all()
   {
      return ALL_PATTERN;
   }

   private static final Pattern NOTHING_PATTERN = new Pattern()
   {
      protected Context match(Context input)
      {
         return input.match(0);
      }

      public String toString()
      {
         return "[NOTHING]";
      }
   };

   /**
    * A pattern which matches nothing.
    * @return
    */
   public static Pattern nothing()
   {
      return NOTHING_PATTERN;
   }

   /**
    * A pattern which matches <code>pattern1</code> followed by <code>pattern2</code> and
    * returns the longer of the two matches.
    * @param pattern1
    * @param pattern2
    * @return
    * @throws IllegalArgumentException if either argument is <code>null</code>
    */
   public static Pattern selection(Pattern pattern1, Pattern pattern2)
   {
      if (pattern1 == null || pattern2 == null)
      {
         throw new IllegalArgumentException("Neither pattern can be null");
      }
      return new SelectPattern(pattern1, pattern2);
   }

   /**
    * A pattern which matches <code>pattern1</code> followed by <code>pattern2</code>.
    * @param pattern1
    * @param pattern2
    * @return
    */
   public static Pattern sequence(Pattern pattern1, Pattern pattern2)
   {
      if (pattern1 == null || pattern2 == null)
      {
         throw new IllegalArgumentException("Neither pattern can be null");
      }
      return new SequencePattern(pattern1, pattern2);
   }

   /**
    * A pattern which matches <code>pattern</code> as many times as possible
    * @param pattern
    * @return
    */
   public static Pattern repeat(Pattern pattern)
   {
      if (pattern == null)
      {
         throw new IllegalArgumentException("Pattern can not be null");
      }
      return new RepeatPattern(pattern);
   }

   /**
    * A pattern which matches <code>pattern</code> as many times as possible
    * but at least <code>min</code> times and at most <code>max</code> times.
    * @param pattern
    * @param min
    * @param max
    * @return
    */
   public static Pattern repeat(Pattern pattern, int min, int max)
   {
      if (pattern == null)
      {
         throw new IllegalArgumentException("Pattern can not be null");
      }
      return new RepeatPattern(pattern, min, max);
   }

   // -----------------------------------------------------< Context >---

   private static class Context
   {
      private final QPath path;

      private final int length;

      private final int pos;

      private final boolean isMatch;

      public Context(QPath path)
      {
         super();
         this.path = path;
         length = path.getEntries().length;
         isMatch = false;
         pos = 0;
      }

      public Context(Context context, int pos, boolean matched)
      {
         path = context.path;
         length = context.length;
         this.pos = pos;
         this.isMatch = matched;
         if (pos > length)
         {
            throw new IllegalArgumentException("Cannot match beyond end of input");
         }
      }

      public Context matchToEnd()
      {
         return new Context(this, length, true);
      }

      public Context match(int count)
      {
         return new Context(this, pos + count, true);
      }

      public Context noMatch()
      {
         return new Context(this, this.pos, false);
      }

      public boolean isMatch()
      {
         return isMatch;
      }

      public QPath getRemainder() throws RepositoryException
      {
         if (pos >= length)
         {
            return null;
         }
         else
         {

            //throw new RepositoryException("not implemented");
            return subPath(path, pos, length);
         }
      }

      /**
       * @see Path#subPath(int, int)
       */
      public QPath subPath(QPath source, int from, int to) throws IllegalArgumentException, RepositoryException
      {
         QPathEntry[] elements = source.getEntries();
         if (from < 0 || to > elements.length || from >= to)
         {
            throw new IllegalArgumentException();
         }
         //         if (!isNormalized())
         //         {
         //            throw new RepositoryException("Cannot extract sub-Path from a non-normalized Path: " + this);
         //         }
         QPathEntry[] dest = new QPathEntry[to - from];
         System.arraycopy(elements, from, dest, 0, dest.length);
         //Builder pb = new Builder(dest);
         return new QPath(dest);
      }

      public boolean isExhausted()
      {
         return pos == length;
      }

      public MatchResult getMatchResult()
      {
         return new MatchResult(path, isMatch ? pos : 0);
      }

      public String toString()
      {
         return pos + " @ " + path;
      }

   }

   // -----------------------------------------------------< SelectPattern >---

   private static class SelectPattern extends Pattern
   {
      private final Pattern pattern1;

      private final Pattern pattern2;

      public SelectPattern(Pattern pattern1, Pattern pattern2)
      {
         super();
         this.pattern1 = pattern1;
         this.pattern2 = pattern2;
      }

      protected Context match(Context input) throws RepositoryException
      {
         Context remainder1 = pattern1.match(input);
         Context remainder2 = pattern2.match(input);
         return remainder1.pos > remainder2.pos ? remainder1 : remainder2;
      }

      public String toString()
      {
         return new StringBuilder().append("(").append(pattern1).append("|").append(pattern2).append(")").toString();
      }
   }

   // -----------------------------------------------------< SequencePattern >---

   private static class SequencePattern extends Pattern
   {
      private final Pattern pattern1;

      private final Pattern pattern2;

      public SequencePattern(Pattern pattern1, Pattern pattern2)
      {
         super();
         this.pattern1 = pattern1;
         this.pattern2 = pattern2;
      }

      protected Context match(Context input) throws RepositoryException
      {
         Context context1 = pattern1.match(input);
         if (context1.isMatch())
         {
            return pattern2.match(context1);
         }
         else
         {
            return input.noMatch();
         }
      }

      public String toString()
      {
         return new StringBuilder().append("(").append(pattern1).append(", ").append(pattern2).append(")").toString();
      }
   }

   // -----------------------------------------------------< RepeatPattern >---

   private static class RepeatPattern extends Pattern
   {
      private final Pattern pattern;

      private final int min;

      private final int max;

      private boolean hasBounds;

      public RepeatPattern(Pattern pattern)
      {
         this(pattern, 0, 0);
         this.hasBounds = false;
      }

      public RepeatPattern(Pattern pattern, int min, int max)
      {
         super();
         this.pattern = pattern;
         this.min = min;
         this.max = max;
         this.hasBounds = true;
      }

      protected Context match(Context input) throws RepositoryException
      {
         Context nextInput;
         Context output = input.match(0);
         int matchCount = -1;
         do
         {
            nextInput = output;
            output = pattern.match(nextInput);
            matchCount++;
         }
         while (output.isMatch() && (output.pos > nextInput.pos));

         if (!hasBounds() || (min <= matchCount && matchCount <= max))
         {
            return nextInput;
         }
         else
         {
            return input.noMatch();
         }
      }

      private boolean hasBounds()
      {
         return hasBounds;
      }

      public String toString()
      {
         return new StringBuilder().append("(").append(pattern).append(")*").toString();
      }

   }

   // -----------------------------------------------------< PathPattern >---

   private static class PathPattern extends Pattern
   {
      private final QPath path;

      private final QPathEntry[] patternElements;

      public PathPattern(QPath path)
      {
         super();
         this.path = path;
         patternElements = path.getEntries();
      }

      protected Context match(Context input) throws RepositoryException
      {
         if (input.isExhausted())
         {
            return input;
         }

         QPath inputPath = input.getRemainder();
         //            if (!inputPath.isNormalized()) {
         //                throw new IllegalArgumentException("Not normalized");
         //            }

         QPathEntry[] inputElements = inputPath.getEntries();
         int inputLength = inputElements.length;
         int patternLength = patternElements.length;
         if (patternLength > inputLength)
         {
            return input.noMatch();
         }

         for (int k = 0; k < patternLength; k++)
         {
            if (!patternElements[k].equals(inputElements[k]))
            {
               return input.noMatch();
            }
         }

         return input.match(patternLength);
      }

      public String toString()
      {
         return new StringBuilder().append("\"").append(path).append("\"").toString();
      }
   }

   // -----------------------------------------------------< AbstractNamePattern >---

   private static abstract class AbstractNamePattern extends Pattern
   {
      protected abstract boolean matches(QPathEntry element);

      protected Context match(Context input) throws RepositoryException
      {
         if (input.isExhausted())
         {
            return input.noMatch();
         }

         QPath inputPath = input.getRemainder();
         //            if (!inputPath.isNormalized()) {
         //                throw new IllegalArgumentException("Not normalized");
         //            }

         QPathEntry[] inputElements = inputPath.getEntries();
         if (inputElements.length < 1 || !matches(inputElements[0]))
         {
            return input.noMatch();
         }

         return input.match(1);
      }

   }

   // -----------------------------------------------------< NameNamePattern >---

   private static class NamePattern extends AbstractNamePattern
   {
      private final InternalQName name;

      public NamePattern(InternalQName name)
      {
         super();
         this.name = name;
      }

      protected boolean matches(QPathEntry element)
      {
         return name.equals(element);
      }

      public String toString()
      {
         return new StringBuilder().append("\"").append(name).append("\"").toString();
      }
   }

   // -----------------------------------------------------< StringNamePattern >---

   private static class RegexPattern extends AbstractNamePattern
   {
      private final java.util.regex.Pattern namespaceUri;

      private final java.util.regex.Pattern localName;

      private final String localNameStr;

      private final String namespaceUriStr;

      public RegexPattern(String namespaceUri, String localName)
      {
         super();

         this.namespaceUri = java.util.regex.Pattern.compile(namespaceUri);
         this.localName = java.util.regex.Pattern.compile(localName);
         this.namespaceUriStr = namespaceUri;
         this.localNameStr = localName;
      }

      protected boolean matches(QPathEntry element)
      {
         InternalQName name = element;
         boolean nsMatches = namespaceUri.matcher(name.getNamespace()).matches();
         boolean localMatches = localName.matcher(name.getName()).matches();
         return nsMatches && localMatches;
      }

      public String toString()
      {
         return new StringBuilder().append("\"{").append(namespaceUriStr).append("}").append(localNameStr).append("\"")
            .toString();
      }
   }

}
