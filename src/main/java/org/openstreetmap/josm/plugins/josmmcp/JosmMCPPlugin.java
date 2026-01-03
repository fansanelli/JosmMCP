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
package org.openstreetmap.josm.plugins.josmmcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Logging;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import reactor.core.publisher.Mono;

public class JosmMCPPlugin extends Plugin {
	private Server jettyServer;

	public JosmMCPPlugin(PluginInformation info) {
		super(info);

		Logging.info("JosmMCPPlugin initialization");
		try {
			// Schema per tool senza parametri
			McpSchema.JsonSchema emptySchema = new McpSchema.JsonSchema("object", new java.util.HashMap<>(), null, null,
					null, null);

			// Schema per tool di ricerca
			Map<String, Object> searchProps = new java.util.HashMap<>();
			Map<String, Object> queryProp = new java.util.HashMap<>();
			queryProp.put("type", "string");
			searchProps.put("query", queryProp);
			Map<String, Object> maxResultsProp = new java.util.HashMap<>();
			maxResultsProp.put("type", "integer");
			searchProps.put("max_results", maxResultsProp);
			McpSchema.JsonSchema searchSchema = new McpSchema.JsonSchema("object", searchProps, null, null, null, null);

			// Schema per tool di modifica
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

			Tool getSelectedTool = Tool.builder().name("get_selected_elements")
					.description("Get the currently selected OSM elements in JOSM").inputSchema(emptySchema).build();
			Tool getStateTool = Tool.builder().name("get_josm_state")
					.description("Ottieni lo stato corrente di JOSM: versione, layer scaricati e informazioni sui dati")
					.inputSchema(emptySchema).build();
			Tool searchTool = Tool.builder().name("search_elements")
					.description("Cerca elementi OSM nei dati scaricati usando query JOSM").inputSchema(searchSchema)
					.build();
			Tool modifyTool = Tool.builder().name("modify_elements")
					.description("Modifica elementi OSM: seleziona, aggiungi/rimuovi tag").inputSchema(modifySchema)
					.build();

			this.jettyServer = new Server(3000);
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/");
			jettyServer.setHandler(context);
			HttpServletStatelessServerTransport servlet = HttpServletStatelessServerTransport.builder().build();
			McpStatelessServerFeatures.AsyncToolSpecification searchSpec = new McpStatelessServerFeatures.AsyncToolSpecification(
					searchTool, (exchange, params) -> {
						Logging.info("Tool search_elements called with params: " + params.arguments());
						Map<String, Object> args = params.arguments();
						if (args == null) {
							String result = "No arguments provided";
							Logging.info("Returning search results: " + result);
							return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(result)))
									.isError(false).build());
						}
						String result = searchElements(args);
						Logging.info("Returning search results: " + result);
						return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(result)))
								.isError(false).build());
					});
			McpStatelessServerFeatures.AsyncToolSpecification modifySpec = new McpStatelessServerFeatures.AsyncToolSpecification(
					modifyTool, (exchange, params) -> {
						Logging.info("Tool modify_elements called");
						String result = modifyElements((Map<String, Object>) params.arguments());
						Logging.info("Returning modify result: " + result);
						return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(result)))
								.isError(false).build());
					});
			McpStatelessServerFeatures.AsyncToolSpecification selectedSpec = new McpStatelessServerFeatures.AsyncToolSpecification(
					getSelectedTool, (exchange, params) -> {
						Logging.info("Tool get_selected_elements called");
						// Logica per ottenere selezione corrente
						Collection<OsmPrimitive> selection = MainApplication.getLayerManager().getEditDataSet()
								.getAllSelected();
						String result = formatSelectedElements(selection);
						Logging.info("Returning selection: " + result);
						return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(result)))
								.isError(false).build());
					});
			McpStatelessServerFeatures.AsyncToolSpecification stateSpec = new McpStatelessServerFeatures.AsyncToolSpecification(
					getStateTool, (exchange, params) -> {
						Logging.info("Tool get_josm_state called");
						String result = getJosmState();
						Logging.info("Returning state: " + result);
						return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(result)))
								.isError(false).build());
					});
			McpStatelessAsyncServer server = McpServer.async(servlet).serverInfo("JOSM MCP Server", "1.0.0")
					.tools(searchSpec, modifySpec, selectedSpec, stateSpec).build();
			Logging.info("JOSM MCP Server created");
			context.addServlet(new ServletHolder(servlet), "/mcp");
			jettyServer.start();
			Logging.info("MCP HTTP server started on port 3000");
		} catch (Exception e) {
			Logging.error("Failed to start MCP server: " + e.getMessage(), e);
		}
	}

	/**
	 * Formatta una collezione di elementi OSM selezionati in una stringa leggibile.
	 */
	private static String formatSelectedElements(Collection<OsmPrimitive> selection) {
		StringBuilder sb = new StringBuilder("Elementi selezionati: " + selection.size());
		for (OsmPrimitive prim : selection) {
			sb.append("\n- ").append(prim.getDisplayName(new DefaultNameFormatter())).append(" (tags: ")
					.append(prim.getKeys()).append(")");
		}
		return sb.toString();
	}

	/**
	 * Ottiene lo stato corrente di JOSM: versione, layer e informazioni sui dati.
	 */
	private static String getJosmState() {
		StringBuilder sb = new StringBuilder();

		// Versione JOSM
		String version = Version.getInstance().getVersionString();
		sb.append("Versione JOSM: ").append(version).append("\n");

		// Layer scaricati
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		sb.append("Layer scaricati: ").append(layers.size()).append("\n");
		for (Layer layer : layers) {
			sb.append("- ").append(layer.getName()).append(" (").append(layer.getClass().getSimpleName()).append(")\n");
		}

		// Informazioni sui dati
		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds != null) {
			sb.append("Informazioni sui dati:\n");
			sb.append("- Nodi: ").append(ds.getNodes().size()).append("\n");
			sb.append("- Ways: ").append(ds.getWays().size()).append("\n");
			sb.append("- Relazioni: ").append(ds.getRelations().size()).append("\n");
			List<Bounds> boundsList = ds.getDataSourceBounds();
			if (!boundsList.isEmpty()) {
				sb.append("- Bounds: ");
				for (Bounds b : boundsList) {
					sb.append(b.toString()).append("; ");
				}
				sb.append("\n");
			}
		} else {
			sb.append("Nessun dataset di modifica attivo.\n");
		}

		return sb.toString();
	}

	/**
	 * Cerca elementi OSM usando una query JOSM.
	 */
	private static String searchElements(Map<String, Object> params) {
		try {
			String query = (String) params.get("query");
			if (query == null || query.trim().isEmpty()) {
				query = "highway=*"; // Default query to search for highways
			}

			Integer maxResults = (Integer) params.get("max_results");
			if (maxResults == null) {
				maxResults = 50;
			}

			DataSet ds = MainApplication.getLayerManager().getEditDataSet();
			if (ds == null) {
				return "Errore: nessun dataset attivo";
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
		} catch (SearchParseError e) {
			return "Errore nella query di ricerca: " + e.getMessage();
		} catch (Exception e) {
			return "Errore durante la ricerca: " + e.getMessage();
		}
	}

	/**
	 * Modifica elementi OSM.
	 */
	private static String modifyElements(Map<String, Object> params) {
		try {
			String action = (String) params.get("action");
			if (action == null || action.trim().isEmpty()) {
				return "Errore: parametro 'action' obbligatorio";
			}

			@SuppressWarnings("unchecked")
			List<Long> elementIds = (List<Long>) params.get("element_ids");
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
				String tagKey = (String) params.get("tag_key");
				String tagValue = (String) params.get("tag_value");
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
				String removeKey = (String) params.get("tag_key");
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
