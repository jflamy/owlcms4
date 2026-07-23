/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Small Jackson 3 helpers used by display payload builders.
 */
public final class JsonUtils {

	private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

	private JsonUtils() {
	}

	public static ArrayNode array() {
		return NODE_FACTORY.arrayNode();
	}

	public static ObjectNode object() {
		return NODE_FACTORY.objectNode();
	}

	public static BaseJsonNode nullNode() {
		return NODE_FACTORY.nullNode();
	}

	public static void set(ArrayNode array, int index, Boolean value) {
		set(array, index, value == null ? nullNode() : NODE_FACTORY.booleanNode(value));
	}

	public static void set(ArrayNode array, int index, JsonNode value) {
		while (array.size() <= index) {
			array.addNull();
		}
		array.set(index, value == null ? nullNode() : value);
	}

	public static void set(ArrayNode array, int index, String value) {
		set(array, index, value == null ? nullNode() : NODE_FACTORY.stringNode(value));
	}
}