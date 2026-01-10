/*
 * JosmMCPPlugin - JOSM plugin to integrate JOSM with the Model Context Protocol
 * Copyright (C) 2025-2026 Pengunaria.dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.openstreetmap.josm.plugins.josmmcp.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class ModifyTags extends BaseTool {

	@Override
	public String getName() {
		return "modify_tags";
	}

	@Override
	public String getDescription() {
		return "Modify tags on an OSM element. To remove a tag use a null or empty value";
	}

	@Override
	public JsonSchema getInputSchema() {
		Map<String, Object> modifyProps = new java.util.HashMap<>();
		Map<String, Object> elTypeProp = new java.util.HashMap<>();
		elTypeProp.put("type", "string");
		elTypeProp.put("enum", new ArrayList<String>(Arrays.asList("node", "way", "relation")));
		modifyProps.put("element_type", elTypeProp);
		Map<String, Object> elementIdProp = new java.util.HashMap<>();
		elementIdProp.put("type", "number");
		modifyProps.put("element_id", elementIdProp);

		Map<String, Object> tagsProp = new java.util.HashMap<>();
		tagsProp.put("type", "object"); // Definisci 'tags' come oggetto
		tagsProp.put("additionalProperties", Map.of("type", "string", // Ogni valore deve essere una stringa
				"maxLength", 255 // Lunghezza massima per il valore
		));
		modifyProps.put("tags", tagsProp);
		McpSchema.JsonSchema modifySchema = new McpSchema.JsonSchema("object", modifyProps,
				Arrays.asList("element_type", "element_id", "tags"), null, null, null);
		return modifySchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) throws Exception {
		Map<String, Object> args = params.arguments();

		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds == null) {
			throw new Exception("no active dataset found");
		}

		String type = (String) args.get("element_type");
		int id = (int) args.get("element_id");
		OsmPrimitive el = ds.getPrimitiveById(new SimplePrimitiveId(id, OsmPrimitiveType.from(type)));

		Collection<Command> cmds = new LinkedList<>();
		@SuppressWarnings("unchecked")
		Map<String, String> tags = (java.util.HashMap<String, String>) args.get("tags");
		if (tags != null) {

			java.util.Iterator<Map.Entry<String, String>> iterator = tags.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, String> entry = iterator.next();
				cmds.add(new ChangePropertyCommand(el, entry.getKey(), entry.getValue()));
			}
		}

		Command c = new SequenceCommand("", cmds, false);
		UndoRedoHandler.getInstance().add(c);
		return "";
	}
}
