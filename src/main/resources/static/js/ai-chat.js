// AI Chat Assistant for Chef
let currentSessionId = null;

document.addEventListener('DOMContentLoaded', () => {
    const aiToggle = document.getElementById('aiChatToggle');
    const aiWindow = document.getElementById('aiChatWindow');
    const closeChat = document.getElementById('closeChat');
    const aiForm = document.getElementById('aiChatForm');
    const aiInput = document.getElementById('aiInput');
    const aiMessages = document.getElementById('aiChatMessages');
    const aiThinking = document.getElementById('aiThinking');
    
    // New UI Elements
    const chatView = document.getElementById('aiChatView');
    const historyView = document.getElementById('aiHistoryView');
    const newChatBtn = document.getElementById('newChatBtn');
    const historyBtn = document.getElementById('historyBtn');

    aiToggle.addEventListener('click', () => {
        aiWindow.classList.toggle('hidden');
        if (!aiWindow.classList.contains('hidden') && !currentSessionId) {
            initAiChat();
        }
    });

    closeChat.addEventListener('click', () => {
        aiWindow.classList.add('hidden');
    });

    newChatBtn.addEventListener('click', () => {
        startAiSession();
    });

    historyBtn.addEventListener('click', () => {
        if (historyView.classList.contains('hidden')) {
            loadSessions();
            toggleView('history');
        } else {
            toggleView('chat');
        }
    });

    aiForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = aiInput.value.trim();
        if (!content || !currentSessionId) return;

        appendMessage('user', content);
        aiInput.value = '';
        
        showThinking(true);
        try {
            const res = await authenticatedFetch(`/api/v1/ai-chat/sessions/${currentSessionId}/messages`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content })
            });
            const json = await res.json();
            if (json.data) {
                appendMessage('ai', json.data.content);
            }
        } catch (err) {
            console.error('AI Chat Error:', err);
            appendMessage('system', 'Sorry, something went wrong. Check connection.');
        } finally {
            showThinking(false);
        }
    });

    async function initAiChat() {
        showThinking(true);
        try {
            // Check for existing sessions first
            const res = await authenticatedFetch('/api/v1/ai-chat/sessions');
            const json = await res.json();
            if (json.data && json.data.length > 0) {
                // Resume most recent session
                loadSession(json.data[0].id);
            } else {
                // Start a new one if none exist
                startAiSession();
            }
        } catch (err) {
            console.error('Failed to init AI chat:', err);
            startAiSession(); // Fallback to starting new
        } finally {
            showThinking(false);
        }
    }

    async function startAiSession() {
        toggleView('chat');
        aiMessages.innerHTML = '';
        appendMessage('system', 'Starting a new analysis session...');
        showThinking(true);
        try {
            const res = await authenticatedFetch('/api/v1/ai-chat/sessions', { method: 'POST' });
            const json = await res.json();
            if (json.data) {
                currentSessionId = json.data.id;
                aiMessages.innerHTML = '';
                appendMessage('system', 'Agent 1 Analysis: Trends and feedback analyzed. Ready to brainstorm!');
            }
        } catch (err) {
            console.error('Failed to start AI session:', err);
            appendMessage('system', 'Failed to connect to AI Assistant.');
        } finally {
            showThinking(false);
        }
    }

    async function loadSessions() {
        historyView.innerHTML = '<div class="ai-message system">Loading history...</div>';
        try {
            const res = await authenticatedFetch('/api/v1/ai-chat/sessions');
            const json = await res.json();
            if (json.data) {
                renderSessions(json.data);
            }
        } catch (err) {
            console.error('Failed to load sessions:', err);
            historyView.innerHTML = '<div class="ai-message system">Failed to load history.</div>';
        }
    }

    function renderSessions(sessions) {
        if (sessions.length === 0) {
            historyView.innerHTML = '<div class="ai-message system">No past sessions found.</div>';
            return;
        }

        historyView.innerHTML = '';
        sessions.forEach(session => {
            const date = new Date(session.startedAt).toLocaleString();
            const summary = session.summary || 'New Conversation';
            
            const div = document.createElement('div');
            div.className = 'session-item';
            div.innerHTML = `
                <div class="session-info">
                    <span class="session-summary">${summary}</span>
                    <span class="session-meta">${date}</span>
                </div>
                <button class="session-delete" data-id="${session.id}" title="Delete Session">
                    <i class="fas fa-trash"></i>
                </button>
            `;
            
            div.addEventListener('click', (e) => {
                if (!e.target.closest('.session-delete')) {
                    loadSession(session.id);
                }
            });

            const delBtn = div.querySelector('.session-delete');
            delBtn.addEventListener('click', async (e) => {
                e.stopPropagation();
                if (confirm('Are you sure you want to delete this session?')) {
                    await deleteSession(session.id);
                    loadSessions();
                }
            });

            historyView.appendChild(div);
        });
    }

    async function loadSession(sessionId) {
        showThinking(true);
        toggleView('chat');
        aiMessages.innerHTML = '';
        try {
            const res = await authenticatedFetch(`/api/v1/ai-chat/sessions/${sessionId}`);
            const json = await res.json();
            if (json.data) {
                currentSessionId = json.data.id;
                if (json.data.messages && json.data.messages.length > 0) {
                    json.data.messages.forEach(msg => {
                        appendMessage(msg.senderType.toLowerCase(), msg.content);
                    });
                } else {
                    appendMessage('system', 'Agent 1 Analysis: Trends and feedback analyzed. Ready to brainstorm!');
                }
            }
        } catch (err) {
            console.error('Failed to load session:', err);
            appendMessage('system', 'Failed to load conversation history.');
        } finally {
            showThinking(false);
        }
    }

    async function deleteSession(sessionId) {
        try {
            await authenticatedFetch(`/api/v1/ai-chat/sessions/${sessionId}`, { method: 'DELETE' });
            if (currentSessionId === sessionId) {
                currentSessionId = null;
                aiMessages.innerHTML = '';
                appendMessage('system', 'Current session deleted.');
            }
        } catch (err) {
            console.error('Failed to delete session:', err);
        }
    }

    function toggleView(view) {
        if (view === 'chat') {
            chatView.classList.remove('hidden');
            historyView.classList.add('hidden');
            historyBtn.innerHTML = '<i class="fas fa-history"></i>';
        } else {
            chatView.classList.add('hidden');
            historyView.classList.remove('hidden');
            historyBtn.innerHTML = '<i class="fas fa-comment"></i>';
        }
    }

    function appendMessage(role, text) {
        const div = document.createElement('div');
        div.className = `ai-message ${role}`;
        
        // Handle line breaks and bolding from AI
        const formattedText = text
            .replace(/\n/g, '<br>')
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        
        div.innerHTML = role === 'system' ? `<i class="fas fa-info-circle"></i> ${formattedText}` : formattedText;
        aiMessages.appendChild(div);
        aiMessages.scrollTop = aiMessages.scrollHeight;
    }

    function showThinking(show) {
        if (show) aiThinking.classList.remove('hidden');
        else aiThinking.classList.add('hidden');
        aiMessages.scrollTop = aiMessages.scrollHeight;
    }
});
