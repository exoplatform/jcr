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
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.index.TermPositionVector;

import java.io.IOException;

/**
 * <code>WeightedHTMLExcerpt</code> creates a HTML excerpt with the following
 * format:
 * <pre>
 *     <span><strong>Jackrabbit</strong> implements both the mandatory XPath and optional SQL
 *     <strong>query</strong> syntax.</span>
 *     <span>Before parsing the XPath <strong>query</strong> in <strong>Jackrabbit</strong>,
 *     the statement is surrounded</span>
 * </pre>
 * In contrast to {@link DefaultHTMLExcerpt} this implementation weights
 * fragments based on the proximity of highlighted terms. Highlighted terms that
 * are adjacent have a higher weight. In addition, the more highlighted terms,
 * the higher the weight.
 *
 * @see WeightedHighlighter
 */
public class WeightedHTMLExcerpt extends AbstractExcerpt {

    /**
     * {@inheritDoc}
     */
    protected String createExcerpt(TermPositionVector tpv,
                                   String text,
                                   int maxFragments,
                                   int maxFragmentSize) throws IOException {
        return WeightedHighlighter.highlight(tpv, getQueryTerms(), text,
                "<div>", "</div>", "<span>", "</span>", "<strong>", "</strong>",
                maxFragments, maxFragmentSize / 2);
    }
}
