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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.gui.MainApplication;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class SearchTool extends BaseTool {

	@Override
	public String getName() {
		return "search_elements";
	}

	@Override
	public String getDescription() {
		return "Cerca elementi OSM nei dati scaricati usando query JOSM";
	}

	@Override
	public JsonSchema getInputSchema() {
		Map<String, Object> searchProps = new java.util.HashMap<>();
		Map<String, Object> queryProp = new java.util.HashMap<>();
		queryProp.put("type", "string");
		searchProps.put("query", queryProp);
		Map<String, Object> maxResultsProp = new java.util.HashMap<>();
		maxResultsProp.put("type", "integer");
		searchProps.put("max_results", maxResultsProp);
		McpSchema.JsonSchema searchSchema = new McpSchema.JsonSchema("object", searchProps, Arrays.asList("query"),
				null, null, null);
		return searchSchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) throws Exception {
		Map<String, Object> args = params.arguments();

		String query = (String) args.get("query");
		Integer maxResults = (Integer) args.get("max_results");
		if (maxResults == null) {
			maxResults = 50;
		}

		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds == null) {
			throw new Exception("no active dataset found");
		}

		SearchCompiler.Match matcher = SearchCompiler.compile(query);
		Collection<OsmPrimitive> allPrimitives = ds.allPrimitives();
		List<OsmPrimitive> results = new java.util.ArrayList<>();

		for (OsmPrimitive prim : allPrimitives) {
			if (matcher.match(prim)) {
				results.add(prim);
			}
		}

		StringBuilder sb = new StringBuilder("Risultati ricerca per '").append(query).append("': ")
				.append(results.size());
		if (results.size() > maxResults) {
			sb.append(" (mostrati primi ").append(maxResults).append(")");
		}
		sb.append("\n");

		int count = 0;
		for (OsmPrimitive prim : results) {
			if (count >= maxResults)
				break;
			sb.append("- ").append(prim.getDisplayName(new DefaultNameFormatter())).append(" (ID: ")
					.append(prim.getId()).append(", tipo: ").append(prim.getType()).append(", tags: ")
					.append(prim.getKeys()).append(")\n");
			count++;
		}

		return sb.toString();
	}
}
