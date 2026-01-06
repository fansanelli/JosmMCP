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

import java.util.Collection;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class SelectedTool extends BaseTool {

	@Override
	public String getName() {
		return "get_selected_elements";
	}

	@Override
	public String getDescription() {
		return "Get the currently selected OSM elements in JOSM";
	}

	@Override
	public JsonSchema getInputSchema() {
		McpSchema.JsonSchema emptySchema = new McpSchema.JsonSchema("object", new java.util.HashMap<>(), null, null,
				null, null);
		return emptySchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) {
		Collection<OsmPrimitive> selection = MainApplication.getLayerManager().getEditDataSet().getAllSelected();
		StringBuilder sb = new StringBuilder("Elementi selezionati: " + selection.size());
		for (OsmPrimitive prim : selection) {
			sb.append("\n- ").append(prim.getDisplayName(new DefaultNameFormatter())).append(" (tags: ")
					.append(prim.getKeys()).append(")");
		}
		return sb.toString();
	}
}
