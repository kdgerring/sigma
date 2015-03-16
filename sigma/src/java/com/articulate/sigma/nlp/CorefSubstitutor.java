package com.articulate.sigma.nlp;

/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
*/

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.google.common.collect.Lists;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Map;

public class CorefSubstitutor {

    final Annotation document;

    public CorefSubstitutor(Annotation document) {
        this.document = document;
    }

    public CorefSubstitutor(String input) {
        Pipeline pipeline = new Pipeline();
        this.document = pipeline.annotate(input);
    }

    private boolean needSpaceBefore(CoreLabel label) {
        String text = label.get(CoreAnnotations.OriginalTextAnnotation.class);
        boolean skipSpace = text.length() == 1 && (
                ",".equals(text)
                        || ".".equals(text)
                        || ")".equals(text)
                        || "!".equals(text)
                        || "?".equals(text)
        );
        return !skipSpace;
    }

    private String replaceCoref(final CoreLabel label, Map<Integer, CorefChain> corefs) {
        String text = label.get(CoreAnnotations.OriginalTextAnnotation.class);
        if(isPronoun(label)) {
            Integer corefClusterId = label.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
            if (corefClusterId != null) {
                if (corefs.get(corefClusterId).getMentionsInTextualOrder().size() > 1) {
                    Integer index = label.get(CoreAnnotations.IndexAnnotation.class);
                    Integer sentence = 1 + label.get(CoreAnnotations.SentenceIndexAnnotation.class);
                    CorefMention mention = corefs.get(corefClusterId).getMentionsInTextualOrder().get(0);
                    if (sentence != mention.sentNum || index < mention.startIndex || index >= mention.endIndex) {
                        text = extractTextWithSameNER(mention);
                    }
                }
            }
        }

        return text;
    }

    private boolean isPronoun(CoreLabel label) {
        String pos = label.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        return "PRP".equals(pos);
    }

    private String extractTextWithSameNER(CorefMention mention) {
        List<String> out = Lists.newArrayListWithCapacity(mention.endIndex - mention.startIndex);
        List<CoreLabel> tokens = getSentenceTokens(mention.sentNum - 1);
        final String ner = tokens.get(0).ner();
        for(int i = mention.startIndex; i < mention.endIndex; i++) {
            CoreLabel coreLabel = tokens.get(i - 1);
            if(ner.equals(coreLabel.get(CoreAnnotations.NamedEntityTagAnnotation.class))) {
                out.add(coreLabel.get(CoreAnnotations.OriginalTextAnnotation.class));
            }
        }
        return String.join(" ", out);
    }

    private List<CoreLabel> getSentenceTokens(int sentenceNumber) {
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        CoreMap sentence = sentences.get(sentenceNumber);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        return tokens;
    }

    public static String substitute(String input) {
        CorefSubstitutor substitutor = new CorefSubstitutor(input);
        return substitutor.substitute();
    }

    public String substitute() {
        Map<Integer, CorefChain> corefs = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        StringBuilder builder = new StringBuilder();
        for(CoreLabel label : document.get(CoreAnnotations.TokensAnnotation.class)) {
            if(builder.length() != 0 && needSpaceBefore(label)) {
                builder.append(" ");
            }
            builder.append(replaceCoref(label, corefs));
        }

        return builder.toString();
    }
}
