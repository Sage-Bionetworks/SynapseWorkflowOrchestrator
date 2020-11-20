package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class AnnotationsTranslatorTest {

	@Test
	public void testTranslateToAnnotationsV2NullInput() {
		org.sagebionetworks.repo.model.annotation.v2.Annotations newAnnotations = AnnotationsTranslator.translateToAnnotationsV2(null);
		assertNotNull(newAnnotations);
	}

	@Test
	public void testTranslateToAnnotationsV2EmptyAnnotations() throws Exception {
		Annotations oldAnnotations = new Annotations();
		// Call under test
		org.sagebionetworks.repo.model.annotation.v2.Annotations newAnnotations = AnnotationsTranslator.translateToAnnotationsV2(oldAnnotations);
		assertNotNull(newAnnotations);
		assertNotNull(newAnnotations.getAnnotations());
		assertEquals(0, newAnnotations.getAnnotations().size());
	}

	@Test
	public void testTranslateToAnnotationsV2OneAnnotationValue () throws Exception {
		Annotations oldAnnotations = new Annotations();
		Long expectedValue = 1L;
		String expectedKey = "key";

		LongAnnotation longAnnotation = new LongAnnotation();
		longAnnotation.setKey(expectedKey);
		longAnnotation.setValue(expectedValue);
		oldAnnotations.setLongAnnos(Collections.singletonList(longAnnotation));

		org.sagebionetworks.repo.model.annotation.v2.Annotations expected = new org.sagebionetworks.repo.model.annotation.v2.Annotations();
		Map<String, AnnotationsValue> map = new HashMap<>();
		AnnotationsValue value = new AnnotationsValue();
		value.setValue(Collections.singletonList(expectedValue.toString()));
		value.setType(AnnotationsValueType.LONG);
		map.put(expectedKey, value);
		expected.setAnnotations(map);

		// Call under test
		org.sagebionetworks.repo.model.annotation.v2.Annotations newAnnotations = AnnotationsTranslator.translateToAnnotationsV2(oldAnnotations);

		assertEquals(expected, newAnnotations);
	}

	@Test
	public void testTranslateToAnnotationsV2MultipleAnnotationValues() throws Exception {
		Annotations oldAnnotations = new Annotations();
		Double expectedDouble = 123.3D;
		String doubleKey = "doubleKey";
		String expectedString = "value";
		String stringKey = "stringKey";
		String id = "123";

		DoubleAnnotation doubleAnnotation = new DoubleAnnotation();
		doubleAnnotation.setKey(doubleKey);
		doubleAnnotation.setValue(expectedDouble);
		oldAnnotations.setDoubleAnnos(Collections.singletonList(doubleAnnotation));

		StringAnnotation stringAnnotation = new StringAnnotation();
		stringAnnotation.setKey(stringKey);
		stringAnnotation.setValue(expectedString);
		oldAnnotations.setStringAnnos(Collections.singletonList(stringAnnotation));
		oldAnnotations.setObjectId(id);

		Map<String, AnnotationsValue> expectedMap = new HashMap<>();
		AnnotationsValue doubleValues = new AnnotationsValue();
		doubleValues.setType(AnnotationsValueType.DOUBLE);
		doubleValues.setValue(Collections.singletonList(expectedDouble.toString()));
		expectedMap.put(doubleKey, doubleValues);
		AnnotationsValue stringValues = new AnnotationsValue();
		stringValues.setType(AnnotationsValueType.STRING);
		stringValues.setValue(Collections.singletonList(expectedString));
		expectedMap.put(stringKey, stringValues);

		// Call under test
		org.sagebionetworks.repo.model.annotation.v2.Annotations newAnnotations = AnnotationsTranslator.translateToAnnotationsV2(oldAnnotations);

		assertEquals(expectedMap, newAnnotations.getAnnotations());
		assertEquals(id, newAnnotations.getId());
	}

	@Test
	public void testLongAnnotationsTranslator() throws Exception {
		Annotations oldAnnotations = new Annotations();
		Long expectedLong = 1L;
		Long expectedLong2 = 2L;
		String key1 = "key1";
		String key2 = "key2";

		LongAnnotation longAnnotation = new LongAnnotation();
		longAnnotation.setKey(key1);
		longAnnotation.setValue(expectedLong);
		LongAnnotation longAnnotation2 = new LongAnnotation();
		longAnnotation2.setKey(key2);
		longAnnotation2.setValue(expectedLong2);
		List<LongAnnotation> list = new ArrayList<LongAnnotation>();
		list.add(longAnnotation);
		list.add(longAnnotation2);
		oldAnnotations.setLongAnnos(list);

		Map<String, AnnotationsValue> expectedMap = new HashMap<>();
		AnnotationsValue value = new AnnotationsValue();
		value.setValue(Collections.singletonList(expectedLong.toString()));
		value.setType(AnnotationsValueType.LONG);
		AnnotationsValue value2 = new AnnotationsValue();
		value2.setValue(Collections.singletonList(expectedLong2.toString()));
		value2.setType(AnnotationsValueType.LONG);
		expectedMap.put(key1, value);
		expectedMap.put(key2, value2);

		Map<String, AnnotationsValue> mapValues = new HashMap<>();
		// Call under test
		AnnotationsTranslator.translateLongAnnotations(oldAnnotations, mapValues);

		assertEquals(expectedMap, mapValues);
	}

	@Test
	public void testDoubleAnnotationsTranslator() throws Exception {
		Annotations oldAnnotations = new Annotations();

		Double expectedDouble = 1.0D;
		Double expectedDouble2 = 2.0D;
		String key1 = "key1";
		String key2 = "key2";

		DoubleAnnotation doubleAnnotation = new DoubleAnnotation();
		doubleAnnotation.setKey(key1);
		doubleAnnotation.setValue(expectedDouble);
		DoubleAnnotation doubleAnnotation2 = new DoubleAnnotation();
		doubleAnnotation2.setKey(key2);
		doubleAnnotation2.setValue(expectedDouble2);
		List<DoubleAnnotation> list = new ArrayList<DoubleAnnotation>();
		list.add(doubleAnnotation);
		list.add(doubleAnnotation2);
		oldAnnotations.setDoubleAnnos(list);

		Map<String, AnnotationsValue> expectedMap = new HashMap<>();
		AnnotationsValue value = new AnnotationsValue();
		value.setValue(Collections.singletonList(expectedDouble.toString()));
		value.setType(AnnotationsValueType.DOUBLE);
		AnnotationsValue value2 = new AnnotationsValue();
		value2.setValue(Collections.singletonList(expectedDouble2.toString()));
		value2.setType(AnnotationsValueType.DOUBLE);
		expectedMap.put(key1, value);
		expectedMap.put(key2, value2);

		Map<String, AnnotationsValue> mapValues = new HashMap<>();
		// Call under test
		AnnotationsTranslator.translateDoubleAnnotations(oldAnnotations, mapValues);

		assertEquals(expectedMap, mapValues);
	}

	@Test
	public void testStringAnnotationsTranslator() throws Exception {
		Annotations oldAnnotations = new Annotations();

		String expectedString = "one";
		String expectedString2 = "two";
		String key1 = "key1";
		String key2 = "key2";

		StringAnnotation stringAnnotation = new StringAnnotation();
		stringAnnotation.setKey(key1);
		stringAnnotation.setValue(expectedString);
		StringAnnotation stringAnnotation2 = new StringAnnotation();
		stringAnnotation2.setKey(key2);
		stringAnnotation2.setValue(expectedString2);

		List<StringAnnotation> list = new ArrayList<StringAnnotation>();
		list.add(stringAnnotation);
		list.add(stringAnnotation2);
		oldAnnotations.setStringAnnos(list);

		Map<String, AnnotationsValue> expectedMap = new HashMap<>();
		AnnotationsValue value = new AnnotationsValue();
		value.setValue(Collections.singletonList(expectedString));
		value.setType(AnnotationsValueType.STRING);
		AnnotationsValue value2 = new AnnotationsValue();
		value2.setValue(Collections.singletonList(expectedString2));
		value2.setType(AnnotationsValueType.STRING);
		expectedMap.put(key1, value);
		expectedMap.put(key2, value2);

		Map<String, AnnotationsValue> mapValues = new HashMap<>();
		// Call under test
		AnnotationsTranslator.translateStringAnnotations(oldAnnotations, mapValues);

		assertEquals(expectedMap, mapValues);
	}
}
