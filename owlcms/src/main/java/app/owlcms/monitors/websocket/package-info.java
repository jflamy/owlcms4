/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
/**
 * WebSocket communication utilities for event forwarding to remote scoreboards and displays.
 * 
 * <p>This package contains extracted classes from WebSocketEventForwarder to improve
 * code organization and maintainability:
 * 
 * <ul>
 *   <li>{@link app.owlcms.monitors.websocket.AttemptStatus} - Enum for attempt display status (good, bad, current, next, request, empty)</li>
 *   <li>{@link app.owlcms.monitors.websocket.AthleteExporter} - Static utilities for exporting athlete data in V2 format</li>
 *   <li>{@link app.owlcms.monitors.websocket.ForwarderPayloadBuilder} - Creates payload maps for update, timer, and decision messages</li>
 *   <li>{@link app.owlcms.monitors.websocket.WebSocketSender} - Handles WebSocket/HTTP communication with debouncing</li>
 *   <li>{@link app.owlcms.monitors.websocket.WebSocketEventSender} - Low-level WebSocket connection manager with pooling and reconnection</li>
 * </ul>
 * 
 * <p>The main orchestrator remains {@link app.owlcms.monitors.WebSocketEventForwarder} which
 * subscribes to UI events and delegates to these helper classes.
 * 
 * @since 64.0
 */
package app.owlcms.monitors.websocket;
