package org.aksw.fox.tools.re.en;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.fox.data.Entity;
import org.aksw.fox.data.Relation;
import org.aksw.fox.tools.re.AbstractRE;
import org.aksw.ocelot.application.Application;
import org.aksw.ocelot.application.IOcelot;
import org.aksw.ocelot.common.config.CfgManager;
import org.aksw.ocelot.common.nlp.stanford.StanfordPipe;
import org.aksw.ocelot.core.nlp.StanfordPipeExtended;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.util.CoreMap;

public class OcelotEN extends AbstractRE {

  static String file = "data/ocelot/config";
  static {
    CfgManager.setFolder(file);
  }
  IOcelot ocelot = new Application(file);
  final StanfordPipe stanford = StanfordPipeExtended.getStanfordPipe();

  /**
   *
   */
  @Override
  public Set<Relation> extract() {
    LOG.info("extract");
    relations.clear();
    if ((entities != null) && !entities.isEmpty()) {
      return _extract(input, breakdownAndSortEntity(entities));
    } else {
      LOG.warn("Entities not given!");
    }
    return relations;
  }

  /**
   * Splits text the sentences and stores in an empty map , keys are the sentence order.
   *
   * @param text
   * @return LinkedHashMap containing sentences
   */
  public Map<Integer, String> getSentences(final String text) {
    final Map<Integer, String> sentenceIndex = new LinkedHashMap<>();
    final List<CoreMap> list = stanford.getSentences(text);
    for (final CoreMap sentence : list) {
      final List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
      String originalSentence = Sentence.listToOriginalTextString(labels);
      if (list.indexOf(sentence) != (list.size() - 1)) {
        originalSentence = originalSentence.replaceAll("\\s+$", "");
      }
      sentenceIndex.put(sentenceIndex.size(), originalSentence);
    }
    return sentenceIndex;
  }

  private int getIndex(final Entity e) {
    int index = -1;
    if (e.getIndices().size() > 1) {
      throw new UnsupportedOperationException("Break down entitiy indices to single index pair.");
    } else if (e.getIndices().size() > 0) {
      index = e.getIndices().iterator().next();
    }
    return index;
  }

  private Map<Integer, List<Entity>> getEntitiesMap(final Map<Integer, String> sentenceMap,
      final List<Entity> entities) {
    int offset = 0;
    final Map<Integer, List<Entity>> entitiesMap = new LinkedHashMap<>();
    for (final Entry<Integer, String> entry : sentenceMap.entrySet()) {
      final int id = entry.getKey();
      final String sentence = entry.getValue();
      entitiesMap.put(id, new ArrayList<>());

      final Iterator<Entity> iter = entities.iterator();
      while (iter.hasNext()) {
        final Entity e = iter.next();
        final int index = getIndex(e);

        if ((index + e.getFullText().length()) > offset) {
          if ((index + e.getFullText().length()) <= (sentence.length() + offset)) {
            entitiesMap.get(id).add(e);
          }
        }
      }
      offset += sentence.length();
    }
    return entitiesMap;
  }

  private Set<URI> getUris(final Set<String> uris) {
    final Set<URI> _uris = new HashSet<>();
    for (final String uri : uris) {
      try {
        _uris.add(new URI(uri));
      } catch (final URISyntaxException e) {
        LOG.error("URISyntaxException for: " + uri);
      }
    }
    return _uris;
  }

  private Set<Relation> _extract(final String text, final List<Entity> entities) {

    // splits text to sentences
    final Map<Integer, String> sentenceMap = getSentences(text);

    // find entities for sentence
    final Map<Integer, List<Entity>> entitiesMap = getEntitiesMap(sentenceMap, entities);
    LOG.info(entitiesMap);
    // each sentence
    int offset = 0;
    for (final Entry<Integer, String> entry : sentenceMap.entrySet()) {
      final int id = entry.getKey();
      final String sentence = entry.getValue();
      LOG.info(sentence);

      final List<Entity> sentenceEntities = entitiesMap.get(id);
      for (int i = 0; (i + 1) < sentenceEntities.size(); i++) {
        final Entity subject = sentenceEntities.get(i);
        final Entity object = sentenceEntities.get(i + 1);

        final String sType = subject.getCategory();
        final String oType = object.getCategory();

        final int si = subject.getIndices().iterator().next() - offset;
        final int oi = object.getIndices().iterator().next() - offset;

        final Set<String> uris = ocelot//
            .run(//
                sentence, sType, oType, //
                si, si + subject.getFullText().length(), //
                oi, oi + object.getFullText().length()//
        );

        if ((null != uris) && (uris.size() > 0)) {

          final String uri = "";
          final String reLabel = "";

          Relation relation = null;
          relation = new Relation(//
              subject, //
              reLabel, //
              uri, //
              object, //
              new ArrayList<>(getUris(uris)), //
              getToolName(), //
              Relation.DEFAULT_RELEVANCE//
          );
          relations.add(relation);
        }
      } // end for
      offset += sentence.length();
    }
    return relations;
  }
}
