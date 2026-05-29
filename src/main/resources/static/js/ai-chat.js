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

    aiToggle.addEventListener('click', () => {
        aiWindow.classList.toggle('hidden');
        if (!aiWindow.classList.contains('hidden') && !currentSessionId) {
            startAiSession();
        }
    });

    closeChat.addEventListener('click', () => {
        aiWindow.classList.add('hidden');
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

    async function startAiSession() {
        showThinking(true);
        try {
            const res = await authenticatedFetch('/api/v1/ai-chat/sessions', { method: 'POST' });
            const json = await res.json();
            if (json.data) {
                currentSessionId = json.data.id;
                appendMessage('system', 'Agent 1 Analysis: Trends and feedback analyzed. Ready to brainstorm!');
            }
        } catch (err) {
            console.error('Failed to start AI session:', err);
            appendMessage('system', 'Failed to connect to AI Assistant.');
        } finally {
            showThinking(false);
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
