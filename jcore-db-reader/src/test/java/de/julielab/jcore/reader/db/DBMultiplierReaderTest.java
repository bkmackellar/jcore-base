package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.xmlData.Constants;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.sql.SQLException;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;
import static org.junit.Assert.*;

public class DBMultiplierReaderTest {
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
        assertTrue(reader.hasNext());
        int batchCount = 0;
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            RowBatch rowbatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            assertNotNull(rowbatch);
            assertNotNull(rowbatch.getIdentifiers());
            assertEquals(5, rowbatch.getIdentifiers().size());
            assertNotNull(rowbatch.getTable());
            assertEquals(Constants.DEFAULT_DATA_TABLE_NAME, rowbatch.getTable());
            ++batchCount;
            jCas.reset();
        }
        assertEquals(4, batchCount);
    }

}
