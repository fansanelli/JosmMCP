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

import org.openstreetmap.josm.tools.Logging;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import reactor.core.publisher.Mono;

public abstract class BaseTool implements org.openstreetmap.josm.plugins.josmmcp.tools.Tool {

	public McpStatelessServerFeatures.AsyncToolSpecification getSpec() {
		io.modelcontextprotocol.spec.McpSchema.Tool tool = Tool.builder().name(this.getName())
				.description(this.getDescription()).inputSchema(this.getInputSchema()).build();

		McpStatelessServerFeatures.AsyncToolSpecification spec = new McpStatelessServerFeatures.AsyncToolSpecification(
				tool, (exchange, params) -> {
					Logging.info(String.format("Tool '%s' called with params: %s", this.getName(), params.arguments()));
					try {
						String result = this.handle(exchange, params);
						Logging.info(String.format("Returning '%s' result: %s", this.getName(), result));
						return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(result)))
								.isError(false).build());
					} catch (Exception e) {
						Logging.error(String.format("Exception in '%s' - message: %s", this.getName(), e.getMessage()));
						return Mono.just(CallToolResult.builder().content(Arrays.asList(new TextContent(e.getMessage())))
								.isError(true).build());
					}
				});
		return spec;
	}
}
