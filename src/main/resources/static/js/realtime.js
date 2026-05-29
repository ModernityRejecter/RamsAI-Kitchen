// realtime.js — shared STOMP/SockJS connection helper for live kitchen + order updates.
// Requires SockJS and @stomp/stompjs (global `StompJs`) to be loaded before this file.

function getRealtimeToken() {
    return localStorage.getItem('token') || sessionStorage.getItem('token');
}

// Creates and activates a STOMP client over SockJS, authenticated with the stored JWT.
// callbacks: { onConnect(client), onError(frameOrEvent), onDisconnect() }
// Returns the client (call .deactivate() to close) or null if realtime is unavailable.
function createRealtimeClient(callbacks = {}) {
    if (typeof StompJs === 'undefined' || typeof SockJS === 'undefined') {
        console.warn('Realtime libraries not loaded; falling back to polling.');
        return null;
    }
    const token = getRealtimeToken();
    if (!token) return null;

    const client = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: () => {}
    });

    // Pick up a freshly-refreshed token (authenticatedFetch rotates it) on every (re)connect.
    client.beforeConnect = () => {
        client.connectHeaders = { Authorization: `Bearer ${getRealtimeToken()}` };
    };

    client.onConnect = () => { if (callbacks.onConnect) callbacks.onConnect(client); };
    client.onStompError = (frame) => { if (callbacks.onError) callbacks.onError(frame); };
    client.onWebSocketClose = () => { if (callbacks.onDisconnect) callbacks.onDisconnect(); };
    client.onWebSocketError = (evt) => { if (callbacks.onError) callbacks.onError(evt); };

    client.activate();
    return client;
}
