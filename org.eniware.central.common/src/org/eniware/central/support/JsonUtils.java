/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eniware.domain.GeneralDatumMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for JSON data.
 * @version 1.2
 */
public final class JsonUtils {

	/** A type reference for a Map with string keys. */
	public static final TypeReference<LinkedHashMap<String, Object>> STRING_MAP_TYPE = new StringMapTypeReference();

	private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setSerializationInclusion(Include.NON_NULL)
			.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

	private static final class StringMapTypeReference
			extends TypeReference<LinkedHashMap<String, Object>> {

		public StringMapTypeReference() {
			super();
		}

	}

	// don't construct me
	private JsonUtils() {
		super();
	}

	/**
	 * Convert an object to a JSON string. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and null values will not
	 * be included in the output. All exceptions while serializing the object
	 * are caught and ignored.
	 * 
	 * @param o
	 *        the object to serialize to JSON
	 * @param defaultValue
	 *        a default value to use if {@code o} is <em>null</em> or if any
	 *        error occurs serializing the object to JSON
	 * @return the JSON string
	 */
	public static String getJSONString(final Object o, final String defaultValue) {
		String result = defaultValue;
		if ( o != null ) {
			try {
				return OBJECT_MAPPER.writeValueAsString(o);
			} catch ( Exception e ) {
				LOG.error("Exception marshalling {} to JSON", o, e);
			}
		}
		return result;
	}

	/**
	 * Convert a JSON string to an object. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and all floating point
	 * values will be converted to {@link BigDecimal} values to faithfully
	 * represent the data. All exceptions while deserializing the object are
	 * caught and ignored.
	 * 
	 * @param json
	 *        the JSON string to convert
	 * @param clazz
	 *        the type of Object to map the JSON into
	 * @return the object
	 * @since 1.1
	 */
	public static <T> T getObjectFromJSON(final String json, Class<T> clazz) {
		T result = null;
		if ( json != null ) {
			try {
				result = OBJECT_MAPPER.readValue(json, clazz);
			} catch ( Exception e ) {
				LOG.error("Exception deserialzing json {}", json, e);
			}
		}
		return result;
	}

	/**
	 * Convert a JSON tree object to a Map with string keys.
	 * 
	 * <p>
	 * This is designed for simple values. An internal {@link ObjectMapper} will
	 * be used, and all floating point values will be converted to
	 * {@link BigDecimal} values to faithfully represent the data. All
	 * exceptions while deserializing the object are caught and ignored.
	 * </p>
	 * 
	 * @param Edge
	 *        the JSON object to convert
	 * @return the map, or {@literal null} if {@code Edge} is not a JSON object,
	 *         is {@literal null}, or any exception occurs generating the JSON
	 * @since 1.2
	 */
	public static Map<String, Object> getStringMapFromTree(final JsonNode Edge) {
		if ( Edge == null || !Edge.isObject() ) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(Edge), STRING_MAP_TYPE);
		} catch ( Exception e ) {
			LOG.error("Exception deserialzing JSON Edge {} to Map<String, Object>", Edge, e);
		}
		return null;
	}

	/**
	 * Write metadata to a JSON generator.
	 * 
	 * @param generator
	 *        The generator to write to.
	 * @param meta
	 *        The metadata to write.
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	public static void writeMetadata(JsonGenerator generator, GeneralDatumMetadata meta)
			throws IOException {
		if ( meta == null ) {
			return;
		}
		Map<String, Object> m = meta.getM();
		if ( m != null ) {
			generator.writeObjectField("m", m);
		}

		Map<String, Map<String, Object>> pm = meta.getPm();
		if ( pm != null ) {
			generator.writeObjectField("pm", pm);
		}

		Set<String> t = meta.getT();
		if ( t != null ) {
			generator.writeArrayFieldStart("t");
			for ( String tag : t ) {
				generator.writeString(tag);
			}
			generator.writeEndArray();
		}
	}

}
