package edu.stanford.nlp.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefClusterIdAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class DeterministicCorefAnnotatorITest extends TestCase {
  private static AnnotationPipeline pipeline;

  public void setUp() throws Exception {
    synchronized(DeterministicCorefAnnotatorITest.class) {
      pipeline = new AnnotationPipeline();
      pipeline.addAnnotator(new PTBTokenizerAnnotator(false));
      pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
      pipeline.addAnnotator(new POSTaggerAnnotator(false));
      pipeline.addAnnotator(new MorphaAnnotator(false));
      pipeline.addAnnotator(new NERCombinerAnnotator(false));
      pipeline.addAnnotator(new ParserAnnotator(false, -1));

      Properties corefProps = new Properties();
      corefProps.put(Constants.DEMONYM_PROP, DefaultPaths.DEFAULT_DCOREF_DEMONYM);
      corefProps.put(Constants.ANIMATE_PROP, DefaultPaths.DEFAULT_DCOREF_ANIMATE);
      corefProps.put(Constants.INANIMATE_PROP, DefaultPaths.DEFAULT_DCOREF_INANIMATE);
      corefProps.put(Constants.MALE_PROP, DefaultPaths.DEFAULT_DCOREF_MALE);
      corefProps.put(Constants.NEUTRAL_PROP, DefaultPaths.DEFAULT_DCOREF_NEUTRAL);
      corefProps.put(Constants.FEMALE_PROP, DefaultPaths.DEFAULT_DCOREF_FEMALE);
      corefProps.put(Constants.PLURAL_PROP, DefaultPaths.DEFAULT_DCOREF_PLURAL);
      corefProps.put(Constants.SINGULAR_PROP, DefaultPaths.DEFAULT_DCOREF_SINGULAR);
      pipeline.addAnnotator(new DeterministicCorefAnnotator(corefProps));
    }
  }


  public void testDeterministicCorefAnnotator() throws Exception {
    // create annotation with text
    String text = "Dan Ramage is working for\nMicrosoft. He's in Seattle!\nAt least, he used to be.  Ed is not in Seattle.";
    Annotation document = new Annotation(text);

    // annotate text with pipeline
    pipeline.annotate(document);

    // test CorefGraphAnnotation
    Map<Integer, CorefChain> corefChains = document.get(CorefChainAnnotation.class);
    Assert.assertNotNull(corefChains);

    // test chainID = m.corefClusterID
    for(int chainID : corefChains.keySet()) {
      CorefChain c = corefChains.get(chainID);
      for(CorefMention m : c.getMentionsInTextualOrder()) {
        Assert.assertEquals(m.corefClusterID, chainID);
      }
    }

    // test CorefClusterIdAnnotation
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    CoreLabel ramageToken = sentences.get(0).get(TokensAnnotation.class).get(1);
    CoreLabel heToken = sentences.get(1).get(TokensAnnotation.class).get(0);
    Integer ramageClusterId = ramageToken.get(CorefClusterIdAnnotation.class);
    Assert.assertNotNull(ramageClusterId);
    Assert.assertSame(ramageClusterId, heToken.get(CorefClusterIdAnnotation.class));
  }

  /**
   * Tests named entities with exact string matches (also tests some more pronouns).
   * @throws Exception
   */
  public void testSameString() throws Exception {
    // create annotation with text
    String text = "Your mom thinks she lives in Denver, but it's a big city.  She actually lives outside of Denver.";
    Annotation document = new Annotation(text);

    // annotate text with pipeline
    pipeline.annotate(document);

    // test CorefChainAnnotation
    Map<Integer, CorefChain> chains = document.get(CorefChainAnnotation.class);
    Assert.assertNotNull(chains);

    // test CorefGraphAnnotation
    //    List<Pair<IntTuple, IntTuple>> graph = document.get(CorefGraphAnnotation.class);
    //    Assert.assertNotNull(graph);

    //    for( Pair<IntTuple, IntTuple> pair : graph ) {
    //      System.out.println("pair " + pair);
    //    }

    // test chainID = m.corefClusterID
    for(int chainID : chains.keySet()) {
      CorefChain c = chains.get(chainID);
      for(CorefMention m : c.getMentionsInTextualOrder()) {
        Assert.assertEquals(m.corefClusterID, chainID);
      }
    }

    // test CorefClusterIdAnnotation
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    CoreLabel yourMomsToken = sentences.get(0).get(TokensAnnotation.class).get(1);
    CoreLabel sheToken1 = sentences.get(0).get(TokensAnnotation.class).get(3);
    CoreLabel sheToken2 = sentences.get(1).get(TokensAnnotation.class).get(0);
    CoreLabel denverToken1 = sentences.get(0).get(TokensAnnotation.class).get(6);
    CoreLabel denverToken2 = sentences.get(1).get(TokensAnnotation.class).get(5);

    Integer yourMomsClusterId = yourMomsToken.get(CorefClusterIdAnnotation.class);
    Integer she1ClusterId = sheToken1.get(CorefClusterIdAnnotation.class);
    Integer she2ClusterId = sheToken2.get(CorefClusterIdAnnotation.class);
    Integer denver1ClusterId = denverToken1.get(CorefClusterIdAnnotation.class);
    Integer denver2ClusterId = denverToken2.get(CorefClusterIdAnnotation.class);
    Assert.assertNotNull(yourMomsClusterId);
    Assert.assertNotNull(she1ClusterId);
    Assert.assertNotNull(she2ClusterId);
    Assert.assertNotNull(denver1ClusterId);
    Assert.assertNotNull(denver2ClusterId);
    Assert.assertSame(yourMomsClusterId, she1ClusterId);
    Assert.assertSame(yourMomsClusterId, she2ClusterId);
    Assert.assertSame(denver1ClusterId, denver2ClusterId);
    Assert.assertNotSame(yourMomsClusterId, denver1ClusterId);

    // test CorefClusterAnnotation
    //    Assert.assertEquals(yourMomsToken.get(CorefClusterAnnotation.class), sheToken1.get(CorefClusterAnnotation.class));
    //    Assert.assertEquals(yourMomsToken.get(CorefClusterAnnotation.class), sheToken2.get(CorefClusterAnnotation.class));
    //    Assert.assertEquals(denverToken1.get(CorefClusterAnnotation.class), denverToken2.get(CorefClusterAnnotation.class));
  }

  public static void main(String[] args) throws Exception {
    DeterministicCorefAnnotatorITest itest = new DeterministicCorefAnnotatorITest();
    itest.testDeterministicCorefAnnotator();
  }
}
