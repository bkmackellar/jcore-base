/**
 * TokenAnnotatorTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 *
 * Current version: 2.0 Since version: 1.0
 *
 * Creation date: Nov 29, 2006
 *
 * This is a JUnit test for the TokenAnnotator.
 **/

package de.julielab.jcore.ae.jtbd;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;

import de.julielab.jcore.ae.jtbd.main.TokenAnnotator;
import de.julielab.jcore.types.Token;

public class TokenAnnotatorTest2 // extends TestCase {
{

	public static void main(String[] args) throws Exception {

		if (args.length == 2) {
			AnalysisEngine sentenceAE = AnalysisEngineFactory.createEngine(TokenAnnotator.class,
					TokenAnnotator.PARAM_MODEL,
					args[1],
					TokenAnnotator.USE_DOC_TEXT_PARAM, true);
			JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
			String abstractText = FileUtils.readFileToString(
					new File(args[0]),
					"UTF-8");
			cas.setDocumentText(abstractText);
			sentenceAE.process(cas);
			Collection<Token> tokens = JCasUtil.select(cas, Token.class);
			 for (Token token : tokens) {
			 System.out.println(token.getCoveredText());
			 }
			 System.out.println("token number: " + tokens.size());

			JFSIndexRepository indexes = cas.getJFSIndexRepository();
			Iterator<?> sentIter = indexes.getAnnotationIndex(Token.type).iterator();
			String offsets = getSentenceOffsets(sentIter);
			System.out.println(offsets);
		} else {
			System.out.println("usage: <path to text file> <path to model file>");
		}
		// assertEquals(19, sentences.size());
	}

	static private String getSentenceOffsets(Iterator<?> tokIter) {
		String predictedOffsets = "";
		while (tokIter.hasNext()) {
			Token t = (Token) tokIter.next();
			// System.out.println(t.getBegin() + " - " + t.getEnd() + " token");
			// LOGGER.debug("sentence: " + s.getCoveredText() + ": " +
			// s.getBegin() + " - " + s.getEnd());
			predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
			predictedOffsets += t.getBegin() + "-" + t.getEnd();
		}

		// if (LOGGER.isDebugEnabled()) {
		// LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
		// }
		// if (LOGGER.isDebugEnabled()) {
		// LOGGER.debug("testProcess() - wanted: " + TEST_TEXT_OFFSETS[i]);
		// }
		return predictedOffsets;
	}
	/**
	 * Logger for this class
	 */
	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(TokenAnnotatorTest.class);

	// private static final String DESCRIPTOR =
	// "src/test/resources/de/julielab/jcore/ae/jtbd/desc/TokenAnnotatorTest.xml";
	//
	//// private static final String TEST_TERM = "alpha protein(s)";
	// // private static final String TEST_TERM = "Broadly speaking, TUFs can be
	// // classified into three categories: 1.";
	// // private static final String TEST_SENTENCES = "X-inactivation, T-cells
	// and
	// // CD44 are XYZ! CD44-related " +
	// // "stuff is\t(not).";
	// //private static final String TEST_SENTENCES = "X-inactivation, T-cells
	// and CD44 are XYZ! CD44-related "
	//// + "stuff is\t(not).";
	//
	//// private static final String TEST_SENTENCES_OFFSETS =
	// "0-1;1-2;2-14;14-15;16-23;24-27;28-32;33-36;37-40;40-41;"
	//// + "42-46;46-47;47-54;55-60;61-63;64-65;65-68;68-69;69-70";
	//// private static final String TEST_TERM_OFFSETS =
	// "0-5;6-13;13-14;14-15;15-16";
	//
	// //private static final String TEST_SENTENCES_TOKEN_NUMBERS =
	// "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19";
	// //private static final String TEST_TERM_TOKEN_NUMBERS = "1;2;3;4;5";
	//
	// private static String getPredictedOffsets(final Iterator<?> tokIter) {
	// String predictedOffsets = "";
	// while (tokIter.hasNext()) {
	// final Token t = (Token) tokIter.next();
	// System.out.println(t.getBegin() + " - " + t.getEnd() + " token " +
	// t.getCoveredText());
	//// //LOGGER.debug(
	//// "getPredictedOffsets() - token: " + t.getCoveredText() + " " +
	// t.getBegin() + " - " + t.getEnd());
	// predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
	// predictedOffsets += t.getBegin() + "-" + t.getEnd();
	// }
	// return predictedOffsets;
	// }
	//
	//// private String getTokenNumbers(final Iterator<?> tokIter) {
	//// String tokenNumbers = "";
	//// while (tokIter.hasNext()) {
	//// final Token t = (Token) tokIter.next();
	//// LOGGER.debug("getTokenNumbers() - token: " + t.getCoveredText() + " " +
	// t.getId());
	//// System.out.println("getTokenNumbers() - token: " + t.getCoveredText() +
	// " " + t.getId());
	//// tokenNumbers += (tokenNumbers.length() > 0) ? ";" : "";
	//// tokenNumbers += t.getId();
	//// }
	//// return tokenNumbers;
	//// }
	//
	// /**
	// * initialize a CAS which is then used for the test. 2 sentences are added
	// * @throws IOException
	// */
	// public static void initSentenceCas(final JCas jcas) throws IOException {
	// String abstractText = FileUtils.readFileToString(new
	// File("src/test/resources/fortok.txt"), "UTF-8");
	// jcas.reset();
	// jcas.setDocumentText(abstractText);
	//
	// final Sentence s1 = new Sentence(jcas);
	// s1.setBegin(0);
	// s1.setEnd(41);
	// s1.addToIndexes();
	//
	// final Sentence s2 = new Sentence(jcas);
	// s2.setBegin(42);
	// s2.setEnd(70);
	// s2.addToIndexes();
	// }
	//
	// /**
	// * initialize a CAS which is then used for the test, the CAS holds no
	// token
	// * annotations
	// */
	//// public void initTermCas(final JCas jcas) {
	//// jcas.reset();
	//// jcas.setDocumentText(TEST_TERM);
	//// }
	//
	//// @Ignore
	//// @Test
	//// public void testProcessOnTestFile() throws Exception {
	//// AnalysisEngine tokenAE =
	// AnalysisEngineFactory.createEngine(TokenAnnotator.class,
	// TokenAnnotator.PARAM_MODEL,
	//// "de/julielab/jcore/ae/jtbd/model/test-model.gz");
	//// JCas cas =
	// JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
	//// String text = FileUtils.readFileToString(new
	// File("src/test/resources/fortok.txt"), "UTF-8");
	//// cas.setDocumentText(text);
	//// tokenAE.process(cas);
	////
	//// Collection<Token> tokens = JCasUtil.select(cas, Token.class);
	//// for (Token token : tokens) {
	//// System.out.println(token.getCoveredText());
	//// }
	//// System.out.println("tokens number: " + tokens.size());
	//// }
	//
	// /**
	// * Test CAS with sentence annotations.
	// * @throws IOException
	// */
	//
	// /**
	// * Test CAS without sentence annotations.
	// * @throws IOException
	// */
	//// @Test
	// public static void main(String[] args) throws IOException {
	//
	// String abstractText = FileUtils.readFileToString(new
	// File("src/test/resources/fortok.txt"), "UTF-8");
	//
	// XMLInputSource tokenXML = null;
	// ResourceSpecifier tokenSpec = null;
	// AnalysisEngine tokenAnnotator = null;
	//
	// try {
	// tokenXML = new XMLInputSource(DESCRIPTOR);
	// tokenSpec =
	// UIMAFramework.getXMLParser().parseResourceSpecifier(tokenXML);
	// tokenAnnotator = UIMAFramework.produceAnalysisEngine(tokenSpec);
	// tokenAnnotator.setConfigParameterValue("UseDocText", true);
	// tokenAnnotator.reconfigure();
	// } catch (IOException | InvalidXMLException |
	// ResourceInitializationException | ResourceConfigurationException ioe) {
	// System.out.println("exception!!!");
	// //LOGGER.error("testProcess()", e);
	// }
	//
	// JCas jcas = null;
	//
	//
	//
	// try {
	//// initSentenceCas(jcas);
	// jcas = tokenAnnotator.newJCas();
	// jcas.reset();
	// jcas.setDocumentText(abstractText);
	// } catch (final ResourceInitializationException e) {
	// //LOGGER.error("testProcess()", e);
	// }
	//
	// // ------------- testing TEST_SENTENCES as input ----------------
	//
	// try {
	// tokenAnnotator.process(jcas);
	// } catch (final Exception e) {
	// e.printStackTrace();
	// }
	//
	// // get the offsets of the sentences
	// JFSIndexRepository indexes = jcas.getJFSIndexRepository();
	// Iterator<?> tokIter = indexes.getAnnotationIndex(Token.type).iterator();
	// String predictedOffsets = getPredictedOffsets(tokIter);
	// // compare offsets
	// System.out.println("predicted: " + predictedOffsets);
	// //LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
	//// LOGGER.debug("testProcess() - wanted: " + TEST_SENTENCES_OFFSETS);
	// //assertEquals(TEST_SENTENCES_OFFSETS, predictedOffsets);
	//
	// // get the token numbers of the sentences
	// tokIter = indexes.getAnnotationIndex(Token.type).iterator();
	// //String tokenNumbers = getTokenNumbers(tokIter);
	// // compare token numbers
	// //LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
	// //LOGGER.debug("testProcess() - wanted: " +
	// TEST_SENTENCES_TOKEN_NUMBERS);
	// //assertEquals(TEST_SENTENCES_TOKEN_NUMBERS, tokenNumbers);
	//
	// // ------------- testing TEST_TERM as input ----------------
	//// initTermCas(jcas);
	//// try {
	//// tokenAnnotator.process(jcas, null);
	//// } catch (final Exception e) {
	//// e.printStackTrace();
	//// }
	////
	//// // get the offsets of the term
	//// indexes = jcas.getJFSIndexRepository();
	//// tokIter = indexes.getAnnotationIndex(Token.type).iterator();
	//// predictedOffsets = getPredictedOffsets(tokIter);
	// // compare offsets
	// //System.out.println("testProcess() - predicted: " + predictedOffsets);
	//// System.out.println("testProcess() - wanted: " + TEST_TERM_OFFSETS);
	// //assertEquals(TEST_TERM_OFFSETS, predictedOffsets);
	//
	// // get the token numbers of the sentences
	// //tokIter = indexes.getAnnotationIndex(Token.type).iterator();
	// //tokenNumbers = getTokenNumbers(tokIter);
	// // compare token numbers
	// //LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
	// //.debug("testProcess() - wanted: " + TEST_TERM_TOKEN_NUMBERS);
	// //assertEquals(TEST_TERM_TOKEN_NUMBERS, tokenNumbers);
	//
	// }

}
