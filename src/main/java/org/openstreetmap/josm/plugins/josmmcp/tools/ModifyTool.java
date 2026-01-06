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
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class ModifyTool extends BaseTool {

	@Override
	public String getName() {
		return "modify_elements";
	}

	@Override
	public String getDescription() {
		return "Modifica elementi OSM: seleziona, aggiungi/rimuovi tag";
	}

	@Override
	public JsonSchema getInputSchema() {
		Map<String, Object> modifyProps = new java.util.HashMap<>();
		Map<String, Object> actionProp = new java.util.HashMap<>();
		actionProp.put("type", "string");
		actionProp.put("enum", new ArrayList<String>(Arrays.asList("select", "add_tag", "set_tags", "remove_tag")));
		modifyProps.put("action", actionProp);
		Map<String, Object> elementIdsProp = new java.util.HashMap<>();
		elementIdsProp.put("type", "array");
		elementIdsProp.put("items", java.util.Collections.singletonMap("type", "integer"));
		modifyProps.put("element_ids", elementIdsProp);
		Map<String, Object> tagKeyProp = new java.util.HashMap<>();
		tagKeyProp.put("type", "string");
		modifyProps.put("tag_key", tagKeyProp);
		Map<String, Object> tagValueProp = new java.util.HashMap<>();
		tagValueProp.put("type", "string");
		modifyProps.put("tag_value", tagValueProp);
		McpSchema.JsonSchema modifySchema = new McpSchema.JsonSchema("object", modifyProps, null, null, null, null);
		return modifySchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) {
		Map<String, Object> args = params.arguments();
		try {
			String action = (String) args.get("action");
			if (action == null || action.trim().isEmpty()) {
				return "Errore: parametro 'action' obbligatorio";
			}

			@SuppressWarnings("unchecked")
			List<Long> elementIds = (List<Long>) args.get("element_ids");
			if (elementIds == null || elementIds.isEmpty()) {
				return "Errore: parametro 'element_ids' obbligatorio";
			}

			DataSet ds = MainApplication.getLayerManager().getEditDataSet();
			if (ds == null) {
				return "Errore: nessun dataset attivo";
			}

			// Trova gli elementi
			List<OsmPrimitive> elements = new java.util.ArrayList<>();
			for (Long id : elementIds) {
				for (OsmPrimitive prim : ds.allPrimitives()) {
					if (prim.getId() == id.longValue()) {
						elements.add(prim);
						break;
					}
				}
			}

			if (elements.isEmpty()) {
				return "Errore: nessun elemento trovato con gli ID specificati";
			}

			if ("select".equals(action)) {
				ds.setSelected(elements);
				return "Selezionati " + elements.size() + " elementi";
			} else if ("add_tag".equals(action) || "set_tags".equals(action)) {
				String tagKey = (String) args.get("tag_key");
				String tagValue = (String) args.get("tag_value");
				if (tagKey == null || tagKey.trim().isEmpty()) {
					return "Errore: parametro 'tag_key' obbligatorio per " + action;
				}
				if (tagValue == null) {
					return "Errore: parametro 'tag_value' obbligatorio per " + action;
				}

				ChangePropertyCommand cmd = new ChangePropertyCommand(elements, tagKey, tagValue);
				// MainApplication.undoRedo.add(cmd);
				cmd.executeCommand();
				return "Tag '" + tagKey + "=" + tagValue + "' impostato su " + elements.size() + " elementi";
			} else if ("remove_tag".equals(action)) {
				String removeKey = (String) args.get("tag_key");
				if (removeKey == null || removeKey.trim().isEmpty()) {
					return "Errore: parametro 'tag_key' obbligatorio per remove_tag";
				}

				ChangePropertyCommand removeCmd = new ChangePropertyCommand(elements, removeKey, null);
				// MainApplication.undoRedo.add(removeCmd);
				removeCmd.executeCommand();
				return "Tag '" + removeKey + "' rimosso da " + elements.size() + " elementi";
			} else {
				return "Errore: azione '" + action
						+ "' non supportata. Azioni disponibili: select, add_tag, set_tags, remove_tag";
			}
		} catch (Exception e) {
			return "Errore durante la modifica: " + e.getMessage();
		}
	}
}
