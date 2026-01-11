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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class CreateWay extends BaseTool {

	@Override
	public String getName() {
		return "create_way";
	}

	@Override
	public String getDescription() {
		return "Create a new way in current Dataset and returns the Id";
	}

	@Override
	public JsonSchema getInputSchema() {
		Map<String, Object> createProps = new java.util.HashMap<>();
		Map<String, Object> nodesProp = new java.util.HashMap<>();
		nodesProp.put("type", "array");
		nodesProp.put("items", Map.of("type", "number"));
		createProps.put("node_ids", nodesProp);
		McpSchema.JsonSchema createSchema = new McpSchema.JsonSchema("object", createProps, Arrays.asList("node_ids"),
				null, null, null);
		return createSchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) throws Exception {
		Map<String, Object> args = params.arguments();

		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds == null) {
			throw new Exception("no active dataset found");
		}

		@SuppressWarnings("unchecked")
		List<Long> nodes = ((List<Object>) args.get("node_ids")).stream().map(obj -> Long.parseLong(obj.toString()))
				.collect(Collectors.toList());
		Way w = new Way();
		for (long id : nodes) {
			Node nd = (Node) ds.getPrimitiveById(new SimplePrimitiveId(id, OsmPrimitiveType.NODE));
			w.addNode(nd);
		}

		AddCommand c = new AddCommand(ds, w);

		UndoRedoHandler.getInstance().add(c);
		return Long.toString(w.getUniqueId());
	}
}
