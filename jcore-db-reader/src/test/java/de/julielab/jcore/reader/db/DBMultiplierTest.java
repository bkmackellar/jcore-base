package de.julielab.jcore.reader.db;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DBMultiplierTest {
    private final static Logger log = LoggerFactory.getLogger(DBMultiplierTest.class);
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        TestDBSetupHelper.setupDatabase(postgres);
    }

    @Test
    public void testDBMultiplierReader() throws UIMAException, IOException, ConfigurationException {

        String costosysConfig = TestDBSetupHelper.createTestCostosysConfig("medline_2017", postgres);
        CollectionReader reader = CollectionReaderFactory.createReader(DBMultiplierReader.class,
                PARAM_BATCH_SIZE, 5,
                PARAM_TABLE, "testsubset",
                PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(TestMultiplier.class,
                TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
        int numReadDocs = 0;
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            RowBatch rowBatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            assertTrue(rowBatch.getIdentifiers().size() > 0);
            JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            while (jCasIterator.hasNext()) {
                ++numReadDocs;
                JCas mJCas = jCasIterator.next();
                assertNotNull(mJCas.getDocumentText());
                assertTrue(JCoReTools.getDocId(mJCas) != null);
                assertTrue(JCoReTools.getDocId(mJCas).length() > 0);
                log.debug(StringUtils.abbreviate(mJCas.getDocumentText(), 200));
                mJCas.release();
            }
            jCas.reset();
        }
    }

    public static class TestMultiplier extends DBMultiplier {
        private final static Logger log = LoggerFactory.getLogger(TestMultiplier.class);

        @Override
        public AbstractCas next() throws AnalysisEngineProcessException {
            JCas jCas = getEmptyJCas();
            boolean hasNext = documentDataIterator.hasNext();
            assertTrue(hasNext);
            if (hasNext) {
                log.trace("Reading next document from database");
                byte[][] artifactData = documentDataIterator.next();
                try {
                    XMLMapper xmlMapper = new XMLMapper(new FileInputStream(new File("src/test/resources/medline2016MappingFile.xml")));
                    xmlMapper.parse(artifactData[1], artifactData[0], jCas);
                } catch (IOException e) {
                    throw new AnalysisEngineProcessException(e);
                }
            }
            return jCas;
        }
    }
}