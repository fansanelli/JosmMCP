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

import java.util.Arrays;
import java.util.Map;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class CreateNode extends BaseTool {

	@Override
	public String getName() {
		return "create_node";
	}

	@Override
	public String getDescription() {
		return "Create a new node in current Dataset and returns the Id";
	}

	@Override
	public JsonSchema getInputSchema() {
		Map<String, Object> createProps = new java.util.HashMap<>();
		Map<String, Object> latProp = new java.util.HashMap<>();
		latProp.put("type", "number");
		createProps.put("latitude", latProp);
		Map<String, Object> lonProp = new java.util.HashMap<>();
		lonProp.put("type", "number");
		createProps.put("longitude", lonProp);
		McpSchema.JsonSchema createSchema = new McpSchema.JsonSchema("object", createProps,
				Arrays.asList("latitude", "longitude"), null, null, null);
		return createSchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) throws Exception {
		Map<String, Object> args = params.arguments();

		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds == null) {
			throw new Exception("no active dataset found");
		}

		double latitude = (double) args.get("latitude");
		double longitude = (double) args.get("longitude");
		LatLon ll = new LatLon(latitude, longitude);
		Node nd = new Node(ll);

		AddCommand c = new AddCommand(ds, nd);

		UndoRedoHandler.getInstance().add(c);
		return Long.toString(nd.getUniqueId());
	}
}
