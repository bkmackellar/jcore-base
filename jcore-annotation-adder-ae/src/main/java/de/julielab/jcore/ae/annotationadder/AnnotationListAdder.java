package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalAnnotation;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class AnnotationListAdder implements AnnotationAdder {
    private final static Logger log = LoggerFactory.getLogger(AnnotationListAdder.class);

    @Override
    public boolean addAnnotations(AnnotationData data, AnnotationAdderHelper helper, AnnotationAdderConfiguration configuration, JCas jCas) {
        AnnotationList annotationList;
        try {
            annotationList = (AnnotationList) data;
        } catch (ClassCastException e) {
            log.debug("AnnotationList adder rejected annotation data of class {}", data.getClass().getCanonicalName());
            return false;
        }
        for (ExternalAnnotation a : annotationList) {
            String uimaType = a.getUimaType() == null ? configuration.getDefaultUimaType() : a.getUimaType();
            if (uimaType == null)
                throw new IllegalArgumentException("Missing annotation type: Neither the annotation of document " + a.getDocumentId() + " with offsets " + a.getStart() + "-" + a.getEnd() + " provides a type nor is the default type set.");
            if (jCas.getTypeSystem().getType(uimaType) == null)
                throw new IllegalArgumentException("The entity annotation type " + a.getUimaType() + " does not exist in the type system.");
            try {
                final Annotation annotation = JCoReAnnotationTools.getAnnotationByClassName(jCas, uimaType);
                helper.setAnnotationOffsets(annotation, a, configuration);
                annotation.addToIndexes();
            } catch (ClassNotFoundException | NoSuchMethodException |InstantiationException | IllegalAccessException | InvocationTargetException | CASException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}