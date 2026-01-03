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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
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
			McpSchema.JsonSchema schema = new McpSchema.JsonSchema("object", Map.of(), null, null, null, null);
			Tool getSelectedTool = Tool.builder().name("get_selected_elements")
					.description("Get the currently selected OSM elements in JOSM").inputSchema(schema).build();

			this.jettyServer = new Server(3000);
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/");
			jettyServer.setHandler(context);
			HttpServletStatelessServerTransport servlet = HttpServletStatelessServerTransport.builder().build();
			McpStatelessAsyncServer server = McpServer.async(servlet).serverInfo("JOSM MCP Server", "1.0.0").tools(
					new McpStatelessServerFeatures.AsyncToolSpecification(getSelectedTool, (exchange, params) -> {
						Logging.info("Tool get_selected_elements called");
						// Logica per ottenere selezione corrente
						Collection<OsmPrimitive> selection = MainApplication.getLayerManager().getEditDataSet()
								.getAllSelected();
						String result = formatSelectedElements(selection);
						Logging.info("Returning selection: " + result);
						return Mono.just(CallToolResult.builder().content(List.of(new TextContent(result)))
								.isError(false).build());
					})).build();
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
}
