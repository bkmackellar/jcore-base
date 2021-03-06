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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.utility.JCoReAnnotationIndexMerger;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;

public class EntityEvaluatorConsumer extends JCasAnnotator_ImplBase {

	public enum OffsetMode {
		CharacterSpan, NonWsCharacters
	}

	public enum OffsetScope {
		Document, Sentence
	}

	// If you add a new built-in column, don't forget to add its name to the
	// "predefinedColumnNames" set!
	public static final String DOCUMENT_ID_COLUMN = "DocumentId";
	public static final String SENTENCE_ID_COLUMN = "SentenceId";
	public static final String OFFSETS_COLUMN = "Offsets";

	public static final String PARAM_OUTPUT_COLUMNS = "OutputColumns";
	public static final String PARAM_COLUMN_DEFINITIONS = "ColumnDefinitions";
	public static final String PARAM_TYPE_PREFIX = "TypePrefix";
	public final static String PARAM_ENTITY_TYPES = "EntityTypes";
	public static final String PARAM_FEATURE_FILTERS = "FeatureFilters";
	public final static String PARAM_OFFSET_MODE = "OffsetMode";
	public final static String PARAM_OFFSET_SCOPE = "OffsetScope";
	public final static String PARAM_OUTPUT_FILE = "OutputFile";

	private static final Logger log = LoggerFactory.getLogger(EntityEvaluatorConsumer.class);

	/**
	 * Returns a map where for each white space position, the number of
	 * preceding non-whitespace characters from the beginning of <tt>input</tt>
	 * is returned.<br/>
	 * Thus, for each character-based offset <tt>o</tt>, the non-whitespace
	 * offset may be retrieved using the floor entry for <tt>o</tt>, retrieving
	 * its value and subtracting it from <tt>o</tt>.
	 * 
	 * @param input
	 * @return
	 */
	public static NavigableMap<Integer, Integer> createNumWsMap(String input) {
		NavigableMap<Integer, Integer> map = new TreeMap<>();
		map.put(0, 0);
		int numWs = 0;
		boolean lastCharWasWs = false;
		for (int i = 0; i < input.length(); ++i) {
			if (lastCharWasWs)
				map.put(i, numWs);
			char c = input.charAt(i);
			if (Character.isWhitespace(c)) {
				++numWs;
				lastCharWasWs = true;
			} else {
				lastCharWasWs = false;
			}
		}
		return map;
	}

	public static Type findType(String typeName, String typePrefix, TypeSystem ts) {
		String effectiveName = typeName.contains(".") ? typeName : typePrefix + "." + typeName;
		Type type = ts.getType(effectiveName);
		if (type == null)
			type = ts.getType(typePrefix + "." + effectiveName);
		if (type == null)
			throw new IllegalArgumentException(
					"The annotation type " + effectiveName + " was not found in the type system. The prefixed name \""
							+ typePrefix + "." + effectiveName + "\" has also been tried without success.");
		return type;
	}

	@ConfigurationParameter(name = PARAM_OUTPUT_COLUMNS, description = "A list of column names that are either defined with the parameter " + PARAM_COLUMN_DEFINITIONS + " or one of '"+DOCUMENT_ID_COLUMN+"', '"+SENTENCE_ID_COLUMN+"' or '"+OFFSETS_COLUMN+"'. This list determines the set and the order of columns that are written into the output file in a tab-separated manner.")
	private String[] outputColumnNamesArray;
	@ConfigurationParameter(name = PARAM_COLUMN_DEFINITIONS, description = "Custom definitions of output columns. Predefined columns are '"+DOCUMENT_ID_COLUMN+"', '"+SENTENCE_ID_COLUMN+"' and '"+OFFSETS_COLUMN+"'. The first two may be overwritten by a custom definition using their exact name. A column definition consists of the name of the column, the type of the annotation from which the values for this column should be derived, and a feature path pointing to the value. A single column definition may refer to multiple, different annotation types with their own feature path. Annotation types that should use the same feature path are separated by a comma. The sets of annotation types where each set shared one feature path are separated by a semicolon. Example: 'entityid:Chemical,Gene=/registryNumber;Disease=/specificType'. In this example, the column named 'entityid' will list the IDs of annotations of types 'Chemical', 'Gene' and 'Disease'. For the first two, the feature 'registryNumber' will be employed, for the latter the feature 'specificType'. The annotation type names will be resolved against the '" + PARAM_TYPE_PREFIX + "' parameter, if specified. The built-in feature path functions 'coveredText()' and 'typeName()' are available. For example, 'type:Gene=/:typeName()' (note the colon preceding the built-in function) will output the fully qualified name of the Gene type.")
	private String[] columnDefinitionDescriptions;
	@ConfigurationParameter(name = PARAM_ENTITY_TYPES, mandatory = false, description = "Optional. A list of entity types for which an output should be created. If all desired types are already mentioned in the '"+ PARAM_COLUMN_DEFINITIONS + "' parameter, this parameter can be left empty.")
	private String[] entityTypeStrings;
	@ConfigurationParameter(name = PARAM_OFFSET_MODE, mandatory = false, description = "Optional. Determines the kind of offset printed out by the component for each entity. Supported are CharacterSpan and NonWsCharacters. The first uses the common UIMA character span offsets. The second counts only the non-whitespace characters for the offsets. This last format is used, for example, by the BioCreative 2 Gene Mention task data. Default is CharacterSpan.")
	private OffsetMode offsetMode;
	@ConfigurationParameter(name = PARAM_OFFSET_SCOPE, mandatory = false, description = "Optional. 'Document' or 'Sentence'. Defaults to Document.")
	private OffsetScope offsetScope;

	@ConfigurationParameter(name = PARAM_TYPE_PREFIX, mandatory = false, description = "Optional. If an annotation type name given in one of the '" + PARAM_COLUMN_DEFINITIONS + "' or '"+ PARAM_ENTITY_TYPES + "' can not be found, it is searched with this prefix. Thus, for JCoRe the prefix 'de.julielab.jcore.types' will cover all annotation types and make the other parameter values briefer.")
	private String typePrefix;
	@ConfigurationParameter(name = PARAM_FEATURE_FILTERS, mandatory = false, description = "Optional. Only lets those entities contribute to the output file that fulfill the given feature value. The syntax is <type>:<feature path>=<value>")
	private String[] featureFilterDefinitions;
	@ConfigurationParameter(name = PARAM_OUTPUT_FILE, description = "Output file to which all entity information is written in the format\n"
			+ "docId EGID begin end confidence\n"
			+ "Where the fields are separated by tab stops. If the file name ends with .gz, the output file will automatically be gzipped.")
	private String outputFilePath;
	private Set<String> predefinedColumnNames = new HashSet<>();

	private LinkedHashSet<String> outputColumnNames;

	private LinkedHashMap<String, Column> columns;
	private LinkedHashSet<Object> entityTypes = new LinkedHashSet<>();

	private List<FeatureValueFilter> featureFilters;

	private File outputFile;

	private List<String[]> entityRecords = new ArrayList<>();

	private BufferedWriter bw;

	private void addOffsetsColumn(JCas aJCas) {
		NavigableMap<Integer, Integer> numWsMap = null;
		OffsetsColumn offsetColumn;
		if (offsetMode == OffsetMode.NonWsCharacters && offsetScope == OffsetScope.Document) {
			numWsMap = createNumWsMap(aJCas.getDocumentText());
			offsetColumn = new OffsetsColumn(numWsMap, offsetMode);
		} else if (offsetScope == OffsetScope.Document) {
			offsetColumn = new OffsetsColumn(offsetMode);
		} else if (offsetScope == OffsetScope.Sentence) {
			offsetColumn = new OffsetsColumn(((SentenceIdColumn) columns.get(SENTENCE_ID_COLUMN)).getSentenceIndex(),
					offsetMode);
		} else
			throw new IllegalArgumentException("Unsupported offset scope " + offsetScope);
		columns.put(OFFSETS_COLUMN, offsetColumn);
	}

	private void addDocumentIdColumn(JCas aJCas) throws CASException {
		if (outputColumnNames.contains(DOCUMENT_ID_COLUMN)) {
			Column c = columns.get(DOCUMENT_ID_COLUMN);
			if (c == null)
                c = new Column(DOCUMENT_ID_COLUMN + ":" + Header.class.getCanonicalName() + "=/docId", null, aJCas.getTypeSystem());
            c = new DocumentIdColumn(c);
			columns.put(DOCUMENT_ID_COLUMN, c);
		}
	}

	private void addSentenceIdColumn(JCas aJCas) throws CASException {
		if (outputColumnNames.contains(SENTENCE_ID_COLUMN)) {
			Column c = columns.get(SENTENCE_ID_COLUMN);
			if (c == null)
                c = new Column(SENTENCE_ID_COLUMN + ":" + Sentence.class.getCanonicalName() + "=/id", null, aJCas.getTypeSystem());
			Column docIdColumn = columns.get(DOCUMENT_ID_COLUMN);
			String documentId = null;
			if (docIdColumn != null)
				documentId = docIdColumn.getValue(aJCas.getDocumentAnnotationFs());
			Type sentenceType = c.getSingleType();
			// put all sentences into an index with an
			// overlap-comparator - this way the index can be
			// queried for entities and the sentence overlapping the
			// entity will be returned
			JCoReTreeMapAnnotationIndex<Long, ? extends Annotation> sentenceIndex = new JCoReTreeMapAnnotationIndex<>(
					Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(),
					TermGenerators.longOffsetTermGenerator());
			sentenceIndex.index(aJCas, sentenceType);
			c = new SentenceIdColumn(documentId, c, sentenceIndex);
			columns.put(SENTENCE_ID_COLUMN, c);
		}
	}

	protected void appendEntityRecordsToFile() {
		for (String[] entityRecord : entityRecords) {
			try {
				bw.write(Stream.of(entityRecord).collect(Collectors.joining("\t")) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		entityRecords.clear();
	}

	private void assertColumnDefined(String columnName) {
		Column c = columns.get(columnName);
		if (c == null)
			throw new IllegalArgumentException(
					"The column \"" + columnName + "\" was set for output but was not defined.");
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		super.batchProcessComplete();
		log.debug("Batch completed. Writing {} entity records to file {}.", entityRecords.size(), outputFile.getName());
		appendEntityRecordsToFile();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		log.info("Collection completed. Writing {} entity records to file {}.", entityRecords.size(),
				outputFile.getName());
		appendEntityRecordsToFile();
		try {
			bw.close();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		outputColumnNamesArray = (String[]) aContext.getConfigParameterValue(PARAM_OUTPUT_COLUMNS);
		columnDefinitionDescriptions = (String[]) aContext.getConfigParameterValue(PARAM_COLUMN_DEFINITIONS);
		typePrefix = (String) aContext.getConfigParameterValue(PARAM_TYPE_PREFIX);

		featureFilterDefinitions = (String[]) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_FEATURE_FILTERS)).orElse(new String[0]);
		outputFilePath = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_FILE);
		entityTypeStrings = (String[]) aContext.getConfigParameterValue(PARAM_ENTITY_TYPES);
		String offsetModeStr = (String) aContext.getConfigParameterValue(PARAM_OFFSET_MODE);
		String offsetScopeStr = (String) aContext.getConfigParameterValue(PARAM_OFFSET_SCOPE);

		outputColumnNames = new LinkedHashSet<>(Stream.of(outputColumnNamesArray).collect(Collectors.toList()));

		offsetMode = null == offsetModeStr ? OffsetMode.CharacterSpan : OffsetMode.valueOf(offsetModeStr);
		if (null == offsetScopeStr) {
			offsetScope = outputColumnNames.contains(SENTENCE_ID_COLUMN) ? OffsetScope.Sentence : OffsetScope.Document;
		} else {
			offsetScope = OffsetScope.valueOf(offsetScopeStr);
		}

		outputFile = new File(outputFilePath);
		if (outputFile.exists()) {
			log.warn("File \"{}\" is overridden.", outputFile.getAbsolutePath());
			outputFile.delete();
		}
		try {
		    if (outputFile != null && outputFile.getParentFile() != null && !outputFile.getParentFile().exists())
                outputFile.getParentFile().mkdirs();
			bw = FileUtilities.getWriterToFile(outputFile);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

		predefinedColumnNames.add(DOCUMENT_ID_COLUMN);
		predefinedColumnNames.add(SENTENCE_ID_COLUMN);
		predefinedColumnNames.add(OFFSETS_COLUMN);

		log.info("{}: {}", PARAM_OUTPUT_COLUMNS, outputColumnNames);
		log.info("{}: {}", PARAM_COLUMN_DEFINITIONS, columnDefinitionDescriptions);
		log.info("{}: {}", PARAM_FEATURE_FILTERS, featureFilterDefinitions);
		log.info("{}: {}", PARAM_ENTITY_TYPES, entityTypeStrings);
		log.info("{}: {}", PARAM_TYPE_PREFIX, typePrefix);
		log.info("{}: {}", PARAM_OUTPUT_FILE, outputFilePath);
		log.info("{}: {}", PARAM_OFFSET_MODE, offsetMode);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			TypeSystem ts = aJCas.getTypeSystem();
			// Initialization of the columns, entity types and filters
			if (columns == null) {
				columns = new LinkedHashMap<>();
				for (int i = 0; i < columnDefinitionDescriptions.length; i++) {
					String definition = columnDefinitionDescriptions[i];
					Column c = new Column(definition, typePrefix, ts);
					columns.put(c.getName(), c);
				}
				// collect all entity types from the column definitions and, one
				// step below, the explicitly listed
				entityTypes = new LinkedHashSet<>(
						columns.values().stream().filter(c -> !predefinedColumnNames.contains(c.getName()))
								.flatMap(c -> c.getTypes().stream()).collect(Collectors.toList()));

				if (entityTypeStrings != null)
					Stream.of(entityTypeStrings).map(name -> findType(name, typePrefix, ts)).forEach(entityTypes::add);
				removeSubsumedTypes(entityTypes, ts);

				featureFilters = Stream.of(featureFilterDefinitions).map(d -> new FeatureValueFilter(d, typePrefix, ts))
						.collect(Collectors.toList());

				addDocumentIdColumn(aJCas);
			}
			// the sentence column must be created new for each document because
			// it is using a document-specific sentence index
			addSentenceIdColumn(aJCas);
			// we just always add the offsets column, if it is used of not
			addOffsetsColumn(aJCas);

			JCoReAnnotationIndexMerger indexMerger = new JCoReAnnotationIndexMerger(entityTypes, true, null, aJCas);
			while (indexMerger.incrementAnnotation()) {
				TOP a = indexMerger.getAnnotation();
				int contradictions = 0;
				for (FeatureValueFilter filter : featureFilters) {
					if (filter.contradictsFeatureFilter(a)) {
						++contradictions;
					}
				}
				if (!featureFilters.isEmpty() && contradictions == featureFilters.size())
					continue;
				int colIndex = 0;
				String[] record = new String[outputColumnNames.size()];
				for (String outputColumnName : outputColumnNames) {
					assertColumnDefined(outputColumnName);
					Column c = columns.get(outputColumnName);
					record[colIndex++] = removeLineBreak(c.getValue(a));
				}
				entityRecords.add(record);
			}
		} catch (CASException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		// Some columns store document specific information that must be cleared
		// before the next document is processed
		for (Column c : columns.values())
			c.reset();
	}

	/**
	 * Primitive removal of line breaks within entity text by replacing newlines
	 * by white spaces. May go wrong if the line break is after a dash, for
	 * example.
	 * 
	 * @param text
	 * @return
	 */
	private String removeLineBreak(String text) {
		if (text == null)
			return null;
		String ret = text.replaceAll("\n", " ");
		return ret;
	}

	private void removeSubsumedTypes(LinkedHashSet<Object> entityTypes, TypeSystem ts) {
		Set<Type> copy = entityTypes.stream().map(Type.class::cast).collect(Collectors.toSet());
		for (Type refType : copy) {
			for (Iterator<Object> typeIt = entityTypes.iterator(); typeIt.hasNext();) {
				Type type = (Type) typeIt.next();
				if (!refType.equals(type) && ts.subsumes(refType, type))
					typeIt.remove();
			}
		}
	}
}
