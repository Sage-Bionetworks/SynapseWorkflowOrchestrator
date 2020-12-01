package org.sagebionetworks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class AnnotationsTranslator {

	public static Annotations translateToAnnotationsV2(org.sagebionetworks.repo.model.annotation.Annotations oldAnnotations) {
		if (oldAnnotations == null) {
			return new Annotations();
		}

		Annotations newAnnotations = new Annotations();
		newAnnotations.setId(oldAnnotations.getObjectId());
		Map<String, AnnotationsValue> mapValues = new HashMap<String, AnnotationsValue>();

		translateLongAnnotations(oldAnnotations, mapValues);
		translateDoubleAnnotations(oldAnnotations, mapValues);
		translateStringAnnotations(oldAnnotations, mapValues);

		newAnnotations.setAnnotations(mapValues);
		return newAnnotations;
	}


	public static void translateLongAnnotations(org.sagebionetworks.repo.model.annotation.Annotations oldAnnotations, Map<String, AnnotationsValue> mapValues) {
		List<LongAnnotation> longList = oldAnnotations.getLongAnnos();
		if (longList != null) {
			for (LongAnnotation annot : longList) {
				AnnotationsValue annotationsValue = new AnnotationsValue();
				annotationsValue.setType(AnnotationsValueType.LONG);
				annotationsValue.setValue(Collections.singletonList(annot.getValue().toString()));
				mapValues.put(annot.getKey(), annotationsValue);
			}
		}
	}

	public static void translateDoubleAnnotations(org.sagebionetworks.repo.model.annotation.Annotations oldAnnotations, Map<String, AnnotationsValue> mapValues) {
		List<DoubleAnnotation> doubleList = oldAnnotations.getDoubleAnnos();
		if (doubleList != null) {
			for (DoubleAnnotation annot : doubleList) {
				AnnotationsValue annotationsValue = new AnnotationsValue();
				annotationsValue.setType(AnnotationsValueType.DOUBLE);
				annotationsValue.setValue(Collections.singletonList(annot.getValue().toString()));
				mapValues.put(annot.getKey(), annotationsValue);
			}
		}
	}

	public static void translateStringAnnotations(org.sagebionetworks.repo.model.annotation.Annotations oldAnnotations, Map<String, AnnotationsValue> mapValues) {
		List<StringAnnotation> stringList = oldAnnotations.getStringAnnos();
		if (stringList != null) {
			for (StringAnnotation annot : stringList) {
				AnnotationsValue annotationsValue = new AnnotationsValue();
				annotationsValue.setType(AnnotationsValueType.STRING);
				annotationsValue.setValue(Collections.singletonList(annot.getValue()));
				mapValues.put(annot.getKey(), annotationsValue);
			}
		}
	}

}
