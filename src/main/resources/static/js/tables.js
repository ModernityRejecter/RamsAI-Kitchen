document.addEventListener('DOMContentLoaded', () => {
    const floorPlan = document.getElementById('floor-plan');
    const statusMessage = document.getElementById('status-message');
    const addTableBtn = document.getElementById('add-table');
    const addWallBtn = document.getElementById('add-wall');
    const resetGridBtn = document.getElementById('reset-grid');
    const applyGridSizeBtn = document.getElementById('applyGridSize');
    const gridColsInput = document.getElementById('gridColsInput');
    const gridRowsInput = document.getElementById('gridRowsInput');
    const mapSettingsSection = document.getElementById('mapSettingsSection');
    const CELL_SIZE = 50;
    const GAP = 0;
    const STRIDE = CELL_SIZE + GAP;

    let gridCols = parseInt(localStorage.getItem('floorPlanCols')) || 15;
    let gridRows = parseInt(localStorage.getItem('floorPlanRows')) || 15;

    let elements = []; // combined tables and walls
    let tableGroups = {}; // Maps tableNumber to group of squares
    let selectedGroupNumber = null;
    let draggedElement = null;
    let offset = { x: 0, y: 0 };
    let dragStartX = 0, dragStartY = 0;
    let didDrag = false;
    let currentUserId = null;

    const userRole = localStorage.getItem('role') || sessionStorage.getItem('role');
    const isManagerOrWaiter = userRole === 'MANAGER' || userRole === 'WAITER';
    const isManager = userRole === 'MANAGER';

    if (!isManagerOrWaiter) {
        document.querySelector('.controls').style.display = 'none';
    }

    if (isManager) {
        mapSettingsSection.style.display = 'block';
        gridColsInput.value = gridCols;
        gridRowsInput.value = gridRows;
    }

    function initGrid() {
        floorPlan.innerHTML = '';
        floorPlan.style.gridTemplateColumns = `repeat(${gridCols}, ${CELL_SIZE}px)`;
        floorPlan.style.gridTemplateRows = `repeat(${gridRows}, ${CELL_SIZE}px)`;
        for (let y = 0; y < gridRows; y++) {
            for (let x = 0; x < gridCols; x++) {
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
                processGroups();
                computeWallEdges();
                renderElements();
                populateSidePanelList();
            } else {
                showStatus('Failed to fetch elements', 'error');
            }
        } catch (error) {
            showStatus('Error connecting to server', 'error');
        }
    }

    function processGroups() {
        tableGroups = {};
        const tables = elements.filter(e => e.elementType === 'table');

        tables.forEach(t => {
            if (!tableGroups[t.tableNumber]) {
                tableGroups[t.tableNumber] = {
                    squares: [],
                    capacity: 0,
                    status: t.status,
                    id: t.id,
                    occupiedByUserId: t.occupiedByUserId
                };
            }
            tableGroups[t.tableNumber].squares.push(t);
            // Keep the smallest id for stability
            if (t.id < tableGroups[t.tableNumber].id) {
                tableGroups[t.tableNumber].id = t.id;
            }
        });

        // Calculate edges (which sides are exposed = no same-group neighbor)
        Object.values(tableGroups).forEach(group => {
            let capacity = 0;
            group.squares.forEach(sq => {
                sq.edges = { top: true, right: true, bottom: true, left: true };

                if (group.squares.some(other => other.xPos === sq.xPos && other.yPos === sq.yPos - 1)) sq.edges.top = false;
                if (group.squares.some(other => other.xPos === sq.xPos && other.yPos === sq.yPos + 1)) sq.edges.bottom = false;
                if (group.squares.some(other => other.xPos === sq.xPos - 1 && other.yPos === sq.yPos)) sq.edges.left = false;
                if (group.squares.some(other => other.xPos === sq.xPos + 1 && other.yPos === sq.yPos)) sq.edges.right = false;

                capacity += Object.values(sq.edges).filter(Boolean).length;
            });
            group.capacity = capacity;
        });
    }

    function computeWallEdges() {
        const walls = elements.filter(e => e.elementType === 'wall');
        walls.forEach(w => {
            const x = w.xPos ?? w.xpos ?? 0;
            const y = w.yPos ?? w.ypos ?? 0;
            w.xPos = x; w.yPos = y;
            w.edges = {
                top:    !walls.some(o => o !== w && (o.xPos ?? o.xpos) === x && (o.yPos ?? o.ypos) === y - 1),
                bottom: !walls.some(o => o !== w && (o.xPos ?? o.xpos) === x && (o.yPos ?? o.ypos) === y + 1),
                left:   !walls.some(o => o !== w && (o.xPos ?? o.xpos) === x - 1 && (o.yPos ?? o.ypos) === y),
                right:  !walls.some(o => o !== w && (o.xPos ?? o.xpos) === x + 1 && (o.yPos ?? o.ypos) === y),
            };
        });
    }

    function applyCornerClasses(domEl, edges) {
        // A corner is rounded only if both adjacent edges are exposed.
        if (!(edges.top && edges.left))     domEl.classList.add('no-tl');
        if (!(edges.top && edges.right))    domEl.classList.add('no-tr');
        if (!(edges.bottom && edges.left))  domEl.classList.add('no-bl');
        if (!(edges.bottom && edges.right)) domEl.classList.add('no-br');
    }

    function renderElements() {
        document.querySelectorAll('.restaurant-table, .restaurant-wall').forEach(el => el.remove());

        elements.forEach(element => {
            const domEl = document.createElement('div');
            domEl.dataset.id = element.id;
            domEl.dataset.type = element.elementType;

            if (element.elementType === 'table') {
                domEl.classList.add('restaurant-table');
                domEl.dataset.tableNumber = element.tableNumber;
                domEl.innerText = element.tableNumber;

                const isSelfOccupied = element.status === 'OCCUPIED' &&
                    currentUserId != null &&
                    element.occupiedByUserId != null &&
                    Number(element.occupiedByUserId) === Number(currentUserId);

                if (isSelfOccupied) {
                    domEl.classList.add('self-occupied');
                } else if (element.status === 'OCCUPIED') {
                    domEl.classList.add('occupied');
                } else if (element.status === 'FREE') {
                    domEl.classList.add('free');
                }

                if (selectedGroupNumber === element.tableNumber) {
                    domEl.classList.add('selected-group');
                }

                if (element.edges) {
                    applyCornerClasses(domEl, element.edges);
                    if (element.edges.top)    addSeatDot(domEl, 'seat-top');
                    if (element.edges.right)  addSeatDot(domEl, 'seat-right');
                    if (element.edges.bottom) addSeatDot(domEl, 'seat-bottom');
                    if (element.edges.left)   addSeatDot(domEl, 'seat-left');
                }

                domEl.title = `Table ${element.tableNumber}`;
            } else {
                domEl.classList.add('restaurant-wall');
                if (element.edges) applyCornerClasses(domEl, element.edges);
            }

            const x = element.xPos ?? element.xpos ?? 0;
            const y = element.yPos ?? element.ypos ?? 0;
            element.xPos = x;
            element.yPos = y;

            domEl.style.left = '';
            domEl.style.top = '';

            if (isManagerOrWaiter) {
                domEl.addEventListener('mousedown', startDragging);
            } else {
                domEl.style.cursor = 'pointer';
            }

            domEl.addEventListener('click', (e) => handleTableClick(e, element));

            const targetCell = floorPlan.querySelector(`.grid-cell[data-x="${x}"][data-y="${y}"]`);
            if (targetCell) targetCell.appendChild(domEl);
        });
    }

    function addSeatDot(parent, positionClass) {
        const dot = document.createElement('div');
        dot.className = `seat-dot ${positionClass}`;
        parent.appendChild(dot);
    }

    function populateSidePanelList() {
        const listContainer = document.getElementById('tableList');
        listContainer.innerHTML = '';

        const entries = Object.entries(tableGroups)
            .sort((a, b) => Number(a[0]) - Number(b[0]));

        if (entries.length === 0) {
            listContainer.innerHTML = '<div style="color:#999;font-size:0.85rem;">No tables yet.</div>';
            return;
        }

        entries.forEach(([tableNum, group]) => {
            const item = document.createElement('div');
            item.className = 'table-list-item';
            if (selectedGroupNumber == tableNum) item.classList.add('active');

            const isSelfOccupied = group.status === 'OCCUPIED' &&
                currentUserId != null &&
                group.occupiedByUserId != null &&
                Number(group.occupiedByUserId) === Number(currentUserId);

            const statusLabel = isSelfOccupied ? 'YOURS' : group.status;
            const statusClass = isSelfOccupied ? 'SELF' : group.status;

            item.innerHTML = `
                <span>Table ${tableNum} (${group.capacity} seats)</span>
                <span class="status-badge ${statusClass}">${statusLabel}</span>
            `;
            item.onclick = () => selectGroup(tableNum);
            listContainer.appendChild(item);
        });
    }

    async function handleTableClick(e, element) {
        if (element.elementType !== 'table') return;
        if (didDrag) return;
        selectGroup(element.tableNumber);
    }

    function selectGroup(tableNumber) {
        selectedGroupNumber = Number(tableNumber);

        document.querySelectorAll('.restaurant-table').forEach(t => {
            if (parseInt(t.dataset.tableNumber) === selectedGroupNumber) t.classList.add('selected-group');
            else t.classList.remove('selected-group');
        });

        populateSidePanelList();

        const group = tableGroups[selectedGroupNumber];
        if (!group) return;

        const isSelfOccupied = group.status === 'OCCUPIED' &&
            currentUserId != null &&
            group.occupiedByUserId != null &&
            Number(group.occupiedByUserId) === Number(currentUserId);

        document.getElementById('selectionDetails').style.display = 'block';
        document.getElementById('detailTableNumber').textContent = selectedGroupNumber;
        document.getElementById('detailStatus').textContent = isSelfOccupied ? 'Occupied by you' : group.status;
        document.getElementById('detailCapacity').textContent = group.capacity;

        const occupyBtn = document.getElementById('occupyTableBtn');
        const freeBtn = document.getElementById('freeTableBtn');
        const selectBtn = document.getElementById('selectTableBtn');

        occupyBtn.style.display = 'none';
        freeBtn.style.display = 'none';
        selectBtn.style.display = 'none';

        if (group.status === 'FREE') {
            occupyBtn.style.display = 'block';
            selectBtn.style.display = 'block';
        } else if (isSelfOccupied) {
            // User can free their own table
            freeBtn.style.display = 'block';
            freeBtn.textContent = 'Leave Table';
            selectBtn.style.display = 'block';
        } else if (isManagerOrWaiter) {
            // Staff can free others' tables
            freeBtn.style.display = 'block';
            freeBtn.textContent = 'Free Table';
        }

        occupyBtn.onclick = () => occupyGroupAction(group.id, selectedGroupNumber);
        freeBtn.onclick = () => freeGroupAction(group.id, selectedGroupNumber);
        selectBtn.onclick = () => selectForOrderAction(group.id, selectedGroupNumber);
    }

    async function occupyGroupAction(firstSquareId, tableNumber) {
        try {
            const response = await authenticatedFetch(`/api/v1/tables/${firstSquareId}/occupy`, { method: 'PUT' });
            if (response.ok) {
                showStatus(`Table ${tableNumber} occupied.`, 'success');
                sessionStorage.setItem('selectedTableId', firstSquareId);
                sessionStorage.setItem('selectedTableNumber', tableNumber);
                await fetchElements();
                selectGroup(tableNumber);
            } else {
                const result = await response.json().catch(() => ({}));
                showStatus(result.message || 'Could not occupy table', 'error');
            }
        } catch (err) {
            showStatus('Error occupying table', 'error');
        }
    }

    async function freeGroupAction(firstSquareId, tableNumber) {
        const group = tableGroups[selectedGroupNumber];
        const isSelfOccupied = group && group.status === 'OCCUPIED' &&
            currentUserId != null &&
            group.occupiedByUserId != null &&
            Number(group.occupiedByUserId) === Number(currentUserId);

        if (!isSelfOccupied && !confirm(`Free Table ${tableNumber}?`)) return;
        try {
            const response = await authenticatedFetch(`/api/v1/tables/${firstSquareId}/free`, { method: 'PUT' });
            if (response.ok) {
                showStatus(`Table ${tableNumber} freed.`, 'success');
                await fetchElements();
                selectGroup(tableNumber);
            } else {
                showStatus('Could not free table', 'error');
            }
        } catch (err) {
            showStatus('Error freeing table', 'error');
        }
    }

    function selectForOrderAction(firstSquareId, tableNumber) {
        sessionStorage.setItem('selectedTableId', firstSquareId);
        sessionStorage.setItem('selectedTableNumber', tableNumber);
        showStatus(`Table ${tableNumber} selected!`, 'success');

        const btn = document.getElementById('selectTableBtn');
        btn.textContent = 'Selected ✓';
        setTimeout(() => window.location.href = 'menu.html', 800);
    }

    function startDragging(e) {
        if (e.button !== 0) return;
        draggedElement = e.currentTarget;
        didDrag = false;
        dragStartX = e.clientX;
        dragStartY = e.clientY;

        const rect = draggedElement.getBoundingClientRect();
        const floorRect = floorPlan.getBoundingClientRect();

        floorPlan.appendChild(draggedElement);

        draggedElement.style.left = (rect.left - floorRect.left - 2) + 'px';
        draggedElement.style.top = (rect.top - floorRect.top - 2) + 'px';

        draggedElement.style.gridColumn = '';
        draggedElement.style.gridRow = '';
        draggedElement.classList.add('dragging');

        offset.x = e.clientX - rect.left;
        offset.y = e.clientY - rect.top;

        document.addEventListener('mousemove', drag);
        document.addEventListener('mouseup', stopDragging);
    }

    function drag(e) {
        if (!draggedElement) return;
        e.preventDefault();
        const dx = e.clientX - dragStartX;
        const dy = e.clientY - dragStartY;
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) didDrag = true;

        const floorRect = floorPlan.getBoundingClientRect();

        let x = e.clientX - floorRect.left - 2 - offset.x;
        let y = e.clientY - floorRect.top - 2 - offset.y;

        const maxX = (gridCols - 1) * STRIDE;
        const maxY = (gridRows - 1) * STRIDE;

        x = Math.max(0, Math.min(x, maxX));
        y = Math.max(0, Math.min(y, maxY));

        draggedElement.style.left = x + 'px';
        draggedElement.style.top = y + 'px';
    }

    async function stopDragging(e) {
        if (!draggedElement) return;

        const floorRect = floorPlan.getBoundingClientRect();
        const draggedRect = draggedElement.getBoundingClientRect();
        const centerX = draggedRect.left + draggedRect.width / 2;
        const centerY = draggedRect.top + draggedRect.height / 2;

        const dropX = centerX - floorRect.left - 2;
        const dropY = centerY - floorRect.top - 2;

        let gridX = Math.floor(dropX / STRIDE);
        let gridY = Math.floor(dropY / STRIDE);

        gridX = Math.max(0, Math.min(gridX, gridCols - 1));
        gridY = Math.max(0, Math.min(gridY, gridRows - 1));

        const elementId = draggedElement.dataset.id;
        const elementType = draggedElement.dataset.type;

        const isOverlap = elements.some(el =>
            !(el.id.toString() === elementId && el.elementType === elementType) &&
            el.xPos === gridX &&
            el.yPos === gridY
        );

        const element = elements.find(el => el.id.toString() === elementId && el.elementType === elementType);

        document.removeEventListener('mousemove', drag);
        document.removeEventListener('mouseup', stopDragging);

        // Fix drop glitch: move element into target cell FIRST (while still position:absolute
        // from the dragging class), then strip the class. Removing the class before reparenting
        // briefly lets the element flow into floorPlan's grid at the wrong spot.
        if (isOverlap) {
            showStatus('Cannot place here: Overlap detected!', 'error');
            const originalCell = floorPlan.querySelector(`.grid-cell[data-x="${element.xPos}"][data-y="${element.yPos}"]`);
            if (originalCell) originalCell.appendChild(draggedElement);
            draggedElement.classList.remove('dragging');
            draggedElement.style.left = '';
            draggedElement.style.top = '';
            draggedElement = null;
        } else {
            const oldX = element.xPos;
            const oldY = element.yPos;
            element.xPos = gridX;
            element.yPos = gridY;

            const targetCell = floorPlan.querySelector(`.grid-cell[data-x="${gridX}"][data-y="${gridY}"]`);
            if (targetCell) targetCell.appendChild(draggedElement);
            draggedElement.classList.remove('dragging');
            draggedElement.style.left = '';
            draggedElement.style.top = '';
            draggedElement = null;

            try {
                const success = await saveElementPosition(elementId, elementType, gridX, gridY);
                if (success) {
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

        setTimeout(() => { didDrag = false; }, 10);
    }

    async function saveElementPosition(id, type, x, y) {
        const endpoint = type === 'table' ? `/api/v1/tables/${id}/position` : `/api/v1/walls/${id}/position`;
        try {
            const response = await authenticatedFetch(endpoint, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
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

    addTableBtn.addEventListener('click', async () => {
        try {
            const response = await authenticatedFetch('/api/v1/tables', { method: 'POST' });
            if (response.ok) {
                showStatus('Table added', 'success');
                fetchElements();
            } else {
                showStatus('Error adding table', 'error');
            }
        } catch (error) {
            showStatus('Connection error', 'error');
        }
    });

    addWallBtn.addEventListener('click', async () => {
        try {
            const response = await authenticatedFetch('/api/v1/walls', { method: 'POST' });
            if (response.ok) {
                showStatus('Wall added', 'success');
                fetchElements();
            } else {
                showStatus('Error adding wall', 'error');
            }
        } catch (error) {
            showStatus('Connection error', 'error');
        }
    });

    resetGridBtn.addEventListener('click', async () => {
        if (!confirm('Are you sure you want to clear all tables and walls? This action cannot be undone.')) return;
        try {
            const response = await authenticatedFetch('/api/v1/layout/reset', { method: 'DELETE' });
            if (response.ok) {
                showStatus('Grid reset successfully', 'success');
                fetchElements();
            } else {
                showStatus('Error resetting grid', 'error');
            }
        } catch (error) {
            showStatus('Connection error', 'error');
        }
    });

    if (applyGridSizeBtn) {
        applyGridSizeBtn.addEventListener('click', () => {
            const newCols = Math.max(5, Math.min(20, parseInt(gridColsInput.value) || 15));
            const newRows = Math.max(5, Math.min(20, parseInt(gridRowsInput.value) || 15));

            const maxX = elements.reduce((m, el) => Math.max(m, el.xPos ?? el.xpos ?? 0), 0);
            const maxY = elements.reduce((m, el) => Math.max(m, el.yPos ?? el.ypos ?? 0), 0);
            if (newCols <= maxX) {
                showStatus(`A table/wall sits at column ${maxX}. Move it before shrinking below ${maxX + 1} columns.`, 'error');
                return;
            }
            if (newRows <= maxY) {
                showStatus(`A table/wall sits at row ${maxY}. Move it before shrinking below ${maxY + 1} rows.`, 'error');
                return;
            }

            gridCols = newCols;
            gridRows = newRows;
            localStorage.setItem('floorPlanCols', gridCols);
            localStorage.setItem('floorPlanRows', gridRows);
            gridColsInput.value = gridCols;
            gridRowsInput.value = gridRows;

            initGrid();
            renderElements();
            showStatus(`Grid resized to ${gridCols}x${gridRows}`, 'success');
        });
    }

    function showStatus(message, type) {
        statusMessage.innerText = message;
        statusMessage.className = type;
        setTimeout(() => {
            statusMessage.innerText = '';
            statusMessage.className = '';
        }, 3000);
    }

    async function loadCurrentUserId() {
        try {
            const res = await authenticatedFetch('/api/v1/user/me');
            if (res.ok) {
                const json = await res.json();
                if (json.data && json.data.id != null) {
                    currentUserId = json.data.id;
                }
            }
        } catch (e) { /* ignore */ }
    }

    (async () => {
        initGrid();
        await loadCurrentUserId();
        fetchElements();
    })();
});
