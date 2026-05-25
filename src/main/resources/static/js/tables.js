document.addEventListener('DOMContentLoaded', () => {
    const floorPlan = document.getElementById('floor-plan');
    const statusMessage = document.getElementById('status-message');
    const saveButton = document.getElementById('save-layout');
    const addTableBtn = document.getElementById('add-table');
    const addWallBtn = document.getElementById('add-wall');
    const resetGridBtn = document.getElementById('reset-grid');
    const CELL_SIZE = 50;
    const GRID_SIZE = 15;

    let elements = []; // combined tables and walls
    let draggedElement = null;
    let offset = { x: 0, y: 0 };

    function initGrid() {
        floorPlan.innerHTML = ''; // clear first
        for (let y = 0; y < GRID_SIZE; y++) {
            for (let x = 0; x < GRID_SIZE; x++) {
                const cell = document.createElement('div');
                cell.classList.add('grid-cell');
                cell.dataset.x = x;
                cell.dataset.y = y;
                floorPlan.appendChild(cell);
            }
        }
    }

    async function fetchElements() {
        try {
            const [tablesRes, wallsRes] = await Promise.all([
                authenticatedFetch('/api/v1/tables/map'),
                authenticatedFetch('/api/v1/walls')
            ]);
            
            if (tablesRes.ok && wallsRes.ok) {
                const tablesResult = await tablesRes.json();
                const wallsResult = await wallsRes.json();
                
                elements = [
                    ...tablesResult.data.map(t => ({...t, elementType: 'table'})),
                    ...wallsResult.data.map(w => ({...w, elementType: 'wall'}))
                ];
                renderElements();
            } else {
                showStatus('Failed to fetch elements', 'error');
            }
        } catch (error) {
            showStatus('Error connecting to server', 'error');
        }
    }

    function renderElements() {
        // Clear existing elements
        document.querySelectorAll('.restaurant-table, .restaurant-wall').forEach(el => el.remove());

        elements.forEach(element => {
            const domEl = document.createElement('div');
            domEl.dataset.id = element.id;
            domEl.dataset.type = element.elementType;

            if (element.elementType === 'table') {
                domEl.classList.add('restaurant-table');
                domEl.innerText = element.tableNumber;
                
                if (element.status === 'OCCUPIED') domEl.classList.add('occupied');
                if (element.status === 'READY') domEl.classList.add('ready');
            } else {
                domEl.classList.add('restaurant-wall');
            }

            const x = element.xPos || element.xpos || 0;
            const y = element.yPos || element.ypos || 0;
            
            // Ensure local state uses camelCase to match
            element.xPos = x;
            element.yPos = y;

            // Remove any inline styles left over
            domEl.style.left = '';
            domEl.style.top = '';
            domEl.style.gridColumn = '';
            domEl.style.gridRow = '';

            domEl.addEventListener('mousedown', startDragging);
            
            const targetCell = floorPlan.querySelector(`.grid-cell[data-x="${x}"][data-y="${y}"]`);
            if (targetCell) {
                targetCell.appendChild(domEl);
            } else {
                // Out of bounds fallback
                const firstCell = floorPlan.querySelector('.grid-cell');
                if (firstCell) firstCell.appendChild(domEl);
            }
        });
    }

    function startDragging(e) {
        if (e.button !== 0) return; // Only left click
        draggedElement = e.target;
        
        const rect = draggedElement.getBoundingClientRect();
        const floorRect = floorPlan.getBoundingClientRect();
        
        // Move element to floorPlan container directly so it can float over the grid
        floorPlan.appendChild(draggedElement);
        
        // Convert to absolute positioning for dragging (2px is the border width of floor-plan)
        draggedElement.style.left = (rect.left - floorRect.left - 2) + 'px';
        draggedElement.style.top = (rect.top - floorRect.top - 2) + 'px';
        
        // Remove grid properties just in case
        draggedElement.style.gridColumn = '';
        draggedElement.style.gridRow = '';
        
        draggedElement.classList.add('dragging');

        // Calculate offset from the top-left corner of the element
        offset.x = e.clientX - rect.left;
        offset.y = e.clientY - rect.top;

        document.addEventListener('mousemove', drag);
        document.addEventListener('mouseup', stopDragging);
    }

    function drag(e) {
        if (!draggedElement) return;
        e.preventDefault();

        const floorRect = floorPlan.getBoundingClientRect();
        
        let x = e.clientX - floorRect.left - 2 - offset.x;
        let y = e.clientY - floorRect.top - 2 - offset.y;

        // Boundary constraints
        const maxX = (GRID_SIZE - 1) * 51; // 50px + 1px gap
        const maxY = (GRID_SIZE - 1) * 51;
        
        x = Math.max(0, Math.min(x, maxX));
        y = Math.max(0, Math.min(y, maxY));

        draggedElement.style.left = x + 'px';
        draggedElement.style.top = y + 'px';
    }

    async function stopDragging(e) {
        if (!draggedElement) return;

        const floorRect = floorPlan.getBoundingClientRect();
        
        // Use the center of the dragged element to determine the target cell
        const draggedRect = draggedElement.getBoundingClientRect();
        let centerX = draggedRect.left + draggedRect.width / 2;
        let centerY = draggedRect.top + draggedRect.height / 2;

        let dropX = centerX - floorRect.left - 2;
        let dropY = centerY - floorRect.top - 2;

        let gridX = Math.floor(dropX / 51);
        let gridY = Math.floor(dropY / 51);
        
        gridX = Math.max(0, Math.min(gridX, GRID_SIZE - 1));
        gridY = Math.max(0, Math.min(gridY, GRID_SIZE - 1));

        const elementId = draggedElement.dataset.id;
        const elementType = draggedElement.dataset.type;
        
        // Check for overlap
        const isOverlap = elements.some(el => 
            !(el.id.toString() === elementId && el.elementType === elementType) && 
            el.xPos === gridX && 
            el.yPos === gridY
        );

        const element = elements.find(el => el.id.toString() === elementId && el.elementType === elementType);

        // Remove dragging states and clear inline positioning styles
        draggedElement.classList.remove('dragging');
        draggedElement.style.left = '';
        draggedElement.style.top = '';

        if (isOverlap) {
            showStatus('Cannot place here: Overlap detected!', 'error');
            // Revert to original position
            const originalCell = floorPlan.querySelector(`.grid-cell[data-x="${element.xPos}"][data-y="${element.yPos}"]`);
            if (originalCell) originalCell.appendChild(draggedElement);
        } else {
            const oldX = element.xPos;
            const oldY = element.yPos;
            
            element.xPos = gridX;
            element.yPos = gridY;

            // Update UI position by appending to new cell
            const targetCell = floorPlan.querySelector(`.grid-cell[data-x="${gridX}"][data-y="${gridY}"]`);
            if (targetCell) targetCell.appendChild(draggedElement);

            // Auto-save
            try {
                const success = await saveElementPosition(elementId, elementType, gridX, gridY);
                if (success) {
                    // Refetch elements to get updated grouping numbers
                    fetchElements();
                } else {
                    element.xPos = oldX;
                    element.yPos = oldY;
                    renderElements();
                }
            } catch (err) {
                element.xPos = oldX;
                element.yPos = oldY;
                renderElements();
            }
        }

        draggedElement = null;
        document.removeEventListener('mousemove', drag);
        document.removeEventListener('mouseup', stopDragging);
    }

    async function saveElementPosition(id, type, x, y) {
        const endpoint = type === 'table' ? `/api/v1/tables/${id}/position` : `/api/v1/walls/${id}/position`;
        try {
            const response = await authenticatedFetch(endpoint, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ xPos: x, yPos: y })
            });

            const result = await response.json();
            if (response.ok) {
                showStatus('Layout saved', 'success');
                return true;
            } else {
                showStatus('Error saving: ' + (result.message || 'Server error'), 'error');
                return false;
            }
        } catch (error) {
            showStatus('Connection error', 'error');
            return false;
        }
    }

    // Add Table Event
    addTableBtn.addEventListener('click', async () => {
        try {
            const response = await authenticatedFetch('/api/v1/tables', {
                method: 'POST'
            });
            const result = await response.json();
            if (response.ok) {
                showStatus('Table added', 'success');
                fetchElements(); // re-fetch to get updated state
            } else {
                showStatus('Error adding table', 'error');
            }
        } catch (error) {
            showStatus('Connection error', 'error');
        }
    });

    // Add Wall Event
    addWallBtn.addEventListener('click', async () => {
        try {
            const response = await authenticatedFetch('/api/v1/walls', {
                method: 'POST'
            });
            const result = await response.json();
            if (response.ok) {
                showStatus('Wall added', 'success');
                fetchElements(); // re-fetch
            } else {
                showStatus('Error adding wall', 'error');
            }
        } catch (error) {
            showStatus('Connection error', 'error');
        }
    });
    
    // Reset Grid Event
    resetGridBtn.addEventListener('click', async () => {
        if (!confirm('Are you sure you want to clear all tables and walls? This action cannot be undone.')) return;
        try {
            const response = await authenticatedFetch('/api/v1/layout/reset', {
                method: 'DELETE'
            });
            if (response.ok) {
                showStatus('Grid reset successfully', 'success');
                fetchElements(); // re-fetch to clear UI
            } else {
                showStatus('Error resetting grid', 'error');
            }
        } catch (error) {
            showStatus('Connection error', 'error');
        }
    });

    function showStatus(message, type) {
        statusMessage.innerText = message;
        statusMessage.className = type;
        setTimeout(() => {
            statusMessage.innerText = '';
            statusMessage.className = '';
        }, 3000);
    }

    initGrid();
    fetchElements();
});