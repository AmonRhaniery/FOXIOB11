package org.aksw.fox.tools.ner.en;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.aksw.fox.data.Entity;
import org.aksw.fox.data.EntityClassMap;
import org.aksw.fox.tools.ner.AbstractNER;
import org.aksw.fox.utils.FoxCfg;
import org.aksw.fox.utils.FoxTextUtil;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

/**
 *
 * @author rspeck
 *
 */
public class StanfordENOldVersion extends AbstractNER {

  public static final String CFG_KEY_STANFORD_CLASSIFIER =
      StanfordENOldVersion.class.getName().concat(".classifier");

  protected CRFClassifier<CoreLabel> classifier =
      CRFClassifier.getClassifierNoExceptions(FoxCfg.get(CFG_KEY_STANFORD_CLASSIFIER));

  /*
   * Gets a list with (CoreLabel, likelihood) with max likelihood at each point in a document.
   */
  protected List<Pair<CoreLabel, Double>> getProbsDocumentToList(final List<CoreLabel> document) {
    final List<Pair<CoreLabel, Double>> list = new ArrayList<>();

    final Triple<int[][][], int[], double[][][]> triple =
        classifier.documentToDataAndLabels(document);
    final CRFCliqueTree<String> cliqueTree = classifier.getCliqueTree(triple);
    for (int i = 0; i < cliqueTree.length(); i++) {
      final CoreLabel cl = document.get(i);
      double maxprob = -1;
      for (final Iterator<String> iter = classifier.classIndex.iterator(); iter.hasNext();) {
        final String label = iter.next();
        final double prob = cliqueTree.prob(i, classifier.classIndex.indexOf(label));
        if (prob > maxprob) {
          maxprob = prob;
        }
      }
      list.add(new Pair<>(cl, maxprob));
    }
    return list;
  }

  @Override
  public List<Entity> retrieve(final String input) {

    LOG.info("retrieve ...");
    final List<Entity> list = new ArrayList<>();

    for (String sentence : FoxTextUtil.getSentences(input)) {
      // DEBUG
      if (LOG.isDebugEnabled()) {
        LOG.debug("sentence: " + sentence);
        // DEBUG
      }

      // token
      final String options = "americanize=false,asciiQuotes=true,ptb3Escaping=false";
      final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(
          new StringReader(sentence), new CoreLabelTokenFactory(), options);

      sentence = null;
      final StringBuilder sb = new StringBuilder();
      for (CoreLabel label; tokenizer.hasNext();) {
        label = tokenizer.next();
        sb.append(label);
        if (tokenizer.hasNext()) {
          sb.append(" ");
        }
      }
      sentence = sb.toString();

      // classify
      for (final List<CoreLabel> lcl : classifier.classify(sentence)) {
        // gets probs map
        final List<Pair<CoreLabel, Double>> probs = getProbsDocumentToList(lcl);
        // DEBUG
        if (LOG.isDebugEnabled()) {
          LOG.debug("getProbs: " + probs);
        }
        // DEBUG
        for (final Pair<CoreLabel, Double> prob : probs) {

          final String type = stanford(prob.first().getString(AnswerAnnotation.class));
          // String txt = p.first().word().replaceAll("\\s+",
          // " ").trim();
          String currentToken = prob.first().get(CoreAnnotations.TextAnnotation.class);
          currentToken = currentToken.replaceAll("\\s+", " ").trim();
          // check for multiword entities
          boolean contains = false;
          boolean equalTypes = false;
          Entity lastEntity = null;
          if (!list.isEmpty()) {
            lastEntity = list.get(list.size() - 1);
            contains = sentence.contains(lastEntity.getFullText() + " " + currentToken + " ");
            equalTypes = type.equals(lastEntity.getCategory());
          }
          if (contains && equalTypes) {
            lastEntity.addText(currentToken);
            // TODO: relevance update
          } else {
            if (type != EntityClassMap.getNullCategory()) {
              float p = Entity.DEFAULT_RELEVANCE;
              if ((FoxCfg.get("stanfordDefaultRelevance") != null)
                  && !Boolean.valueOf(FoxCfg.get("stanfordDefaultRelevance"))) {
                p = prob.second().floatValue();
              }
              list.add(getEntity(currentToken, type, p, getToolName()));
            }
          }
        }
      }
    }
    // TRACE
    if (LOG.isTraceEnabled()) {
      LOG.trace(list);
    } // TRACE
    return list;
  }

  public static String stanford(final String stanfordTag) {
    switch (stanfordTag) {
      case "ORGANIZATION":
        return (EntityClassMap.O);
      case "LOCATION":
        return (EntityClassMap.L);
      case "PERSON":
        return (EntityClassMap.P);
      case "PEOPLE":
        return (EntityClassMap.P);
      case "O":
        return (EntityClassMap.N);
    }
    return EntityClassMap.N;
  }

  public static void main(final String[] a) {
    // NERStanford.en(null);
  }

  public static void en(final String[] a) {

    /*
     * for (Entity e : new NERStanford().retrieve(FoxConst.EXAMPLE_1)) NERStanford.LOG.info(e);
     */

    final Properties props = new Properties();
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, relation");
    final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    final Annotation ann =
        new Annotation("Stanford University is located in California. It is a great university.");
    pipeline.annotate(ann);

    for (final CoreMap sentence : ann.get(SentencesAnnotation.class)) {
      for (final CoreLabel token : sentence.get(TokensAnnotation.class)) {
        System.out.println(token.get(NamedEntityTagAnnotation.class));
        System.out.println(token.get(CoreAnnotations.AnswerAnnotation.class));
        /*
         * Tree tree = sentence.get(TreeAnnotation.class); System.out.println(tree);
         * System.out.println(tree.score());
         */
      }
    }

  }
}
