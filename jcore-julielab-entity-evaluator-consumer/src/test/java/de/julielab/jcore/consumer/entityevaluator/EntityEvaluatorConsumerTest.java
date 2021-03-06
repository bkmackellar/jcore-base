/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.consumer.entityevaluator;

import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.DOCUMENT_ID_COLUMN;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.OFFSETS_COLUMN;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_COLUMN_DEFINITIONS;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_ENTITY_TYPES;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_FEATURE_FILTERS;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_OFFSET_MODE;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_OUTPUT_COLUMNS;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_OUTPUT_FILE;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_TYPE_PREFIX;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.SENTENCE_ID_COLUMN;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.TreeMap;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.Test;

import com.google.common.io.Files;

import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.Disease;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;

public class EntityEvaluatorConsumerTest {

	public static final File DOC_DIR_FILE = new File("src/test/resources/documents");
	public static final File DOC_FILE_ENTITIES_WO_ID = new File("src/test/resources/entitiesWithoutId/7520377.xmi");
	public static final File OUTPUT_FILE = new File("src/test/resources/entityRecord.tsv");

	@Test
	public void testEntityEvaluatorConsumerSingleEntity() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-mention-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-meta-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				PARAM_COLUMN_DEFINITIONS,
				new String[] { DOCUMENT_ID_COLUMN + ": Header = /docId",
						"geneid:Gene=/resourceEntryList[0]/entryId", "name:/:coveredText()" },
				// We here use the default SentenceId column, we did not provide a definition!
				PARAM_OUTPUT_COLUMNS, new String[] { DOCUMENT_ID_COLUMN, SENTENCE_ID_COLUMN, "geneid", "name" },
				PARAM_TYPE_PREFIX, "de.julielab.jcore.types", PARAM_OUTPUT_FILE, "src/test/resources/outfile-test.tsv");

		jcas.setDocumentText("One gene one sentence.");
		Header h = new Header(jcas);
		h.setDocId("document1");
		h.addToIndexes();
		Sentence s = new Sentence(jcas, 0, jcas.getDocumentText().length());
		s.setId("sentence1");
		s.addToIndexes();
		Gene g = new Gene(jcas, 4, 8);
		GeneResourceEntry re = new GeneResourceEntry(jcas);
		re.setEntryId("23");
		FSArray array = new FSArray(jcas, 1);
		array.set(0, re);
		g.setResourceEntryList(array);
		g.addToIndexes();

		consumer.process(jcas.getCas());
		consumer.collectionProcessComplete();

		List<String> lines = Files.readLines(new File("src/test/resources/outfile-test.tsv"), Charset.forName("UTF-8"));
		assertEquals(1, lines.size());
		assertEquals("document1	document1:0	23	gene", lines.get(0));
	}

	@Test
	public void testEntityEvaluatorConsumerMultipleEntities() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-mention-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-meta-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				PARAM_COLUMN_DEFINITIONS,
				new String[] {  SENTENCE_ID_COLUMN + ": Sentence=/id",
						"entityid:Chemical=/registryNumber;Disease=/specificType", "name:/:coveredText()" },
				PARAM_OUTPUT_COLUMNS,
                // In this test, we employ the default DocumentId column, we did not define it.
				new String[] { DOCUMENT_ID_COLUMN, SENTENCE_ID_COLUMN, OFFSETS_COLUMN, "entityid", "name" },
				PARAM_TYPE_PREFIX, "de.julielab.jcore.types", PARAM_OUTPUT_FILE, "src/test/resources/outfile-test.tsv",
				EntityEvaluatorConsumer.PARAM_OFFSET_SCOPE, "Document");

		jcas.setDocumentText("Aspirin is an acid. It is good against headache.");
		Header h = new Header(jcas);
		h.setDocId("document1");
		h.addToIndexes();
		new Sentence(jcas, 0, 19).addToIndexes();
		new Sentence(jcas, 20, jcas.getDocumentText().length()).addToIndexes();
		Chemical g = new Chemical(jcas, 0, 7);
		g.setRegistryNumber("registry 42");
		g.addToIndexes();
		Disease d = new Disease(jcas, 39, 47);
		d.setSpecificType("headstuff");
		d.addToIndexes();

		consumer.process(jcas.getCas());
		consumer.collectionProcessComplete();

		List<String> lines = Files.readLines(new File("src/test/resources/outfile-test.tsv"), Charset.forName("UTF-8"));
		assertEquals(2, lines.size());
		assertEquals("document1	document1:0	0	7	registry 42	Aspirin", lines.get(0));
		assertEquals("document1	document1:1	39	47	headstuff	headache", lines.get(1));
	}

	@Test
	public void testEntityEvaluatorConsumerSingleEntityNoWSOffsets() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-mention-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-meta-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				PARAM_COLUMN_DEFINITIONS,
				new String[] { DOCUMENT_ID_COLUMN + ": Header = /docId", SENTENCE_ID_COLUMN + ": Sentence=/id",
						"geneid:/specificType", "name:/:coveredText()" },
				PARAM_OUTPUT_COLUMNS,
				new String[] { DOCUMENT_ID_COLUMN, SENTENCE_ID_COLUMN, "geneid", "name", OFFSETS_COLUMN },
				PARAM_TYPE_PREFIX, "de.julielab.jcore.types", PARAM_OUTPUT_FILE, "src/test/resources/outfile-test.tsv",
				PARAM_OFFSET_MODE, "NonWsCharacters", EntityEvaluatorConsumer.PARAM_ENTITY_TYPES,
				new String[] { "Gene" });

		jcas.setDocumentText("One gene one sentence.");
		Header h = new Header(jcas);
		h.setDocId("document1");
		h.addToIndexes();
		Sentence s = new Sentence(jcas, 0, jcas.getDocumentText().length());
		s.setId("sentence1");
		s.addToIndexes();
		Gene g = new Gene(jcas, 4, 8);
		g.setSpecificType("Growth Hormon Producer");
		g.addToIndexes();

		consumer.process(jcas.getCas());
		consumer.collectionProcessComplete();

		List<String> lines = Files.readLines(new File("src/test/resources/outfile-test.tsv"), Charset.forName("UTF-8"));
		assertEquals(1, lines.size());
		System.out.println(lines);
		assertEquals("document1	document1:0	Growth Hormon Producer	gene	3	6", lines.get(0));
	}

	@Test
	public void testEntityEvaluatorConsumerSuperType() throws Exception {
		// When the column definitions include types where one subsumes the
		// other, e.g. EntityMention and Gene, then we don't want to traverse
		// the subsumed types on their own. They are contained in the annotation
		// index of their super type.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-mention-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-meta-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				PARAM_COLUMN_DEFINITIONS,
				new String[] { DOCUMENT_ID_COLUMN + ": Header = /docId", SENTENCE_ID_COLUMN + ": Sentence=/id",
						"geneid:Gene=/resourceEntryList[0]/entryId", "name:EntityMention=/:coveredText()" },
				PARAM_OUTPUT_COLUMNS, new String[] { DOCUMENT_ID_COLUMN, SENTENCE_ID_COLUMN, "geneid", "name" },
				PARAM_TYPE_PREFIX, "de.julielab.jcore.types", PARAM_OUTPUT_FILE, "src/test/resources/outfile-test.tsv",
				PARAM_ENTITY_TYPES, new String[] { "Gene", "EntityMention" });

		jcas.setDocumentText("One gene one sentence.");
		Header h = new Header(jcas);
		h.setDocId("document1");
		h.addToIndexes();
		Sentence s = new Sentence(jcas, 0, jcas.getDocumentText().length());
		s.setId("sentence1");
		s.addToIndexes();
		Gene g = new Gene(jcas, 4, 8);
		GeneResourceEntry re = new GeneResourceEntry(jcas);
		re.setEntryId("23");
		FSArray array = new FSArray(jcas, 1);
		array.set(0, re);
		g.setResourceEntryList(array);
		g.addToIndexes();

		consumer.process(jcas.getCas());
		consumer.collectionProcessComplete();

		List<String> lines = Files.readLines(new File("src/test/resources/outfile-test.tsv"), Charset.forName("UTF-8"));
		assertEquals(1, lines.size());
		assertEquals("document1	document1:0	23	gene", lines.get(0));
	}

	@Test
	public void testCreateNonWsOffsetMap() throws Exception {
		Method method = EntityEvaluatorConsumer.class.getDeclaredMethod("createNumWsMap", String.class);
		method.setAccessible(true);
		@SuppressWarnings("unchecked")
		TreeMap<Integer, Integer> numWsMap = (TreeMap<Integer, Integer>) method.invoke(null, "one two three");
		// first check the actual map entries (after each white space position
		// there should be an entry)
		assertEquals(new Integer(0), numWsMap.get(0));
		assertEquals(new Integer(1), numWsMap.get(4));
		assertEquals(new Integer(2), numWsMap.get(8));

		// now check the intended use; using the floor element, we should be
		// able to the correct value even for those positions we don't have an
		// explicit mapping for
		assertEquals(new Integer(0), numWsMap.floorEntry(2).getValue());
		assertEquals(new Integer(1), numWsMap.floorEntry(5).getValue());
		assertEquals(new Integer(2), numWsMap.floorEntry(11).getValue());
	}

	@Test
	public void testEntityEvaluatorConsumerFeatureFilter() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-mention-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-meta-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				PARAM_COLUMN_DEFINITIONS,
				new String[] { DOCUMENT_ID_COLUMN + ": Header = /docId", SENTENCE_ID_COLUMN + ": Sentence=/id",
						"geneid:Gene=/resourceEntryList[0]/entryId", "name:/:coveredText()" },
				PARAM_OUTPUT_COLUMNS, new String[] { DOCUMENT_ID_COLUMN, SENTENCE_ID_COLUMN, "geneid", "name" },
				PARAM_TYPE_PREFIX, "de.julielab.jcore.types", PARAM_OUTPUT_FILE, "src/test/resources/outfile-test.tsv",
				PARAM_FEATURE_FILTERS, new String[] { "Gene:/resourceEntryList[0]/entryId=42" });

		jcas.setDocumentText("One gene one sentence.");
		Header h = new Header(jcas);
		h.setDocId("document1");
		h.addToIndexes();
		Sentence s = new Sentence(jcas, 0, jcas.getDocumentText().length());
		s.setId("sentence1");
		s.addToIndexes();
		{
			Gene g = new Gene(jcas, 4, 8);
			GeneResourceEntry re = new GeneResourceEntry(jcas);
			re.setEntryId("23");
			FSArray array = new FSArray(jcas, 1);
			array.set(0, re);
			g.setResourceEntryList(array);
			g.addToIndexes();
		}
		{
			Gene g = new Gene(jcas, 0, 3);
			GeneResourceEntry re = new GeneResourceEntry(jcas);
			re.setEntryId("42");
			FSArray array = new FSArray(jcas, 1);
			array.set(0, re);
			g.setResourceEntryList(array);
			g.addToIndexes();
		}

		consumer.process(jcas.getCas());
		consumer.collectionProcessComplete();

		List<String> lines = Files.readLines(new File("src/test/resources/outfile-test.tsv"), Charset.forName("UTF-8"));
		assertEquals(1, lines.size());
		assertEquals("document1	document1:0	42	One", lines.get(0));
	}
}
