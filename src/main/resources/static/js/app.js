const API_BASE = '/api/v1';
let selectedFile = null;
let columnRules = {};
let allHeaders = [];
let currentJobId = null;

document.addEventListener('DOMContentLoaded', function () {
    const uploadZone = document.getElementById('upload-zone');
    const fileInput = document.getElementById('file-input');
    const form = document.getElementById('sanitization-form');

    uploadZone.addEventListener('click', () => fileInput.click());
    uploadZone.addEventListener('dragover', (e) => { e.preventDefault(); uploadZone.classList.add('dragover'); });
    uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('dragover'));
    uploadZone.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadZone.classList.remove('dragover');
        if (e.dataTransfer.files.length) handleFile(e.dataTransfer.files[0]);
    });

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length) handleFile(e.target.files[0]);
    });

    document.getElementById('remove-file')?.addEventListener('click', (e) => {
        e.stopPropagation();
        resetUpload();
    });

    form.addEventListener('submit', handleSubmit);
    checkHealth();

    // Check URL for diff parameter (from dashboard/history links)
    const urlParams = new URLSearchParams(window.location.search);
    const diffJobId = urlParams.get('diff');
    if (diffJobId) {
        // Hide initial upload state nicely
        document.getElementById('upload-zone').parentElement.reset();
        document.getElementById('upload-zone').classList.add('hidden');
        document.getElementById('columns-section').classList.add('hidden');

        showDiff(parseInt(diffJobId));
        // Clear the URL param
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

async function handleFile(file) {
    if (!file.name.toLowerCase().endsWith('.csv')) {
        alert('Please upload a CSV file');
        return;
    }
    selectedFile = file;
    document.getElementById('upload-content').classList.add('hidden');
    document.getElementById('file-info').classList.remove('hidden');
    document.getElementById('file-name').textContent = file.name;
    document.getElementById('file-size').textContent = formatBytes(file.size);

    // Get preview from server
    await loadPreview(file);
}

async function loadPreview(file) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('rows', '10');

    try {
        const res = await fetch(`${API_BASE}/preview`, {
            method: 'POST',
            body: formData
        });

        if (!res.ok) throw new Error('Failed to load preview');

        const preview = await res.json();
        allHeaders = preview.headers;
        showColumnConfig(preview.headers);
        showPreviewTable(preview);
    } catch (e) {
        console.error('Preview error:', e);
        // Fallback to local parsing
        parseCSVHeadersLocal(file);
    }
}

function parseCSVHeadersLocal(file) {
    const reader = new FileReader();
    reader.onload = function (e) {
        const firstLine = e.target.result.split('\n')[0];
        const headers = firstLine.split(',').map(h => h.trim().replace(/"/g, ''));
        allHeaders = headers;
        showColumnConfig(headers);
    };
    reader.readAsText(file.slice(0, 10000));
}

function showPreviewTable(preview) {
    const section = document.getElementById('preview-section');
    const table = document.getElementById('preview-table');
    section.classList.remove('hidden');

    if (!preview.rows || preview.rows.length === 0) {
        table.innerHTML = '<tr><td class="p-4 text-center text-gray-400 italic">No data rows found in this file (only headers?)</td></tr>';
        document.getElementById('preview-info').textContent = '0 rows';
        return;
    }

    let html = `
        <thead class="bg-white/5">
            <tr>
                ${preview.headers.map(h => `<th class="px-4 py-3 text-left text-xs font-medium text-gray-400 uppercase">${h}</th>`).join('')}
            </tr>
        </thead>
        <tbody class="divide-y divide-white/10">
    `;

    preview.rows.forEach((row, idx) => {
        html += `<tr class="hover:bg-white/5">`;
        row.forEach(cell => {
            const val = cell || '';
            const truncated = val.length > 30 ? val.substring(0, 30) + '...' : val;
            html += `<td class="px-4 py-2 text-sm text-gray-300" title="${escapeHtml(val)}">${escapeHtml(truncated)}</td>`;
        });
        html += `</tr>`;
    });

    html += `</tbody>`;
    table.innerHTML = html;

    const info = document.getElementById('preview-info');
    info.textContent = `Showing ${preview.previewRows} of ${preview.totalRows > 0 ? preview.totalRows.toLocaleString() : 'many'} rows`;
}

function showColumnConfig(headers) {
    const grid = document.getElementById('columns-grid');
    const section = document.getElementById('columns-section');

    grid.innerHTML = headers.map((col, idx) => `
        <div class="glass-card rounded-xl p-4 hover:border-indigo-500/50 transition-all">
            <div class="flex items-center justify-between mb-3">
                <div class="flex items-center space-x-2">
                    <span class="text-xs text-gray-500 font-mono">#${idx + 1}</span>
                    <span class="font-medium text-sm truncate max-w-[120px]" title="${col}">${col}</span>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                    <input type="checkbox" class="col-checkbox sr-only peer" data-column="${col}">
                    <div class="w-9 h-5 bg-gray-700 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-600"></div>
                </label>
            </div>
            <select class="col-select w-full rounded-lg px-3 py-2 text-sm" data-column="${col}" disabled>
                <option value="MASK">🔒 MASK</option>
                <option value="HASH">🔗 HASH</option>
                <option value="NULLIFY">🚫 NULLIFY</option>
                <option value="RANDOMIZE">🎲 RANDOMIZE</option>
            </select>
        </div>
    `).join('');

    section.classList.remove('hidden');

    document.querySelectorAll('.col-checkbox').forEach(chk => {
        chk.addEventListener('change', function () {
            const col = this.dataset.column;
            const sel = document.querySelector(`.col-select[data-column="${col}"]`);
            sel.disabled = !this.checked;
            if (this.checked) {
                columnRules[col] = sel.value;
            } else {
                delete columnRules[col];
            }
            updateSelectAllState();
            updateSubmitButton();
        });
    });

    document.querySelectorAll('.col-select').forEach(sel => {
        sel.addEventListener('change', function () {
            const col = this.dataset.column;
            if (columnRules[col]) columnRules[col] = this.value;
        });
    });

    const selectAllCheckbox = document.getElementById('select-all-columns');
    const defaultOpSelect = document.getElementById('default-operation');

    selectAllCheckbox.addEventListener('change', function () {
        const defaultOp = defaultOpSelect.value;
        document.querySelectorAll('.col-checkbox').forEach(chk => {
            chk.checked = this.checked;
            const col = chk.dataset.column;
            const sel = document.querySelector(`.col-select[data-column="${col}"]`);
            sel.disabled = !this.checked;
            sel.value = defaultOp;
            if (this.checked) {
                columnRules[col] = defaultOp;
            } else {
                delete columnRules[col];
            }
        });
        updateSubmitButton();
    });

    updateSubmitButton();
}

function updateSelectAllState() {
    const allCheckboxes = document.querySelectorAll('.col-checkbox');
    const checkedCount = document.querySelectorAll('.col-checkbox:checked').length;
    const selectAllCheckbox = document.getElementById('select-all-columns');
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = checkedCount === allCheckboxes.length;
        selectAllCheckbox.indeterminate = checkedCount > 0 && checkedCount < allCheckboxes.length;
    }
}

function updateSubmitButton() {
    const btn = document.getElementById('submit-btn');
    btn.disabled = !selectedFile || Object.keys(columnRules).length === 0;
}

function resetUpload() {
    selectedFile = null;
    columnRules = {};
    allHeaders = [];
    currentJobId = null;
    document.getElementById('upload-content').classList.remove('hidden');
    document.getElementById('file-info').classList.add('hidden');
    document.getElementById('columns-section').classList.add('hidden');
    document.getElementById('preview-section').classList.add('hidden');
    document.getElementById('file-input').value = '';
    const selectAll = document.getElementById('select-all-columns');
    if (selectAll) selectAll.checked = false;
    document.getElementById('result-section').classList.add('hidden');
    document.getElementById('progress-section').classList.add('hidden');
    document.getElementById('diff-section').classList.add('hidden');
    updateSubmitButton();
}

async function handleSubmit(e) {
    e.preventDefault();
    if (!selectedFile || Object.keys(columnRules).length === 0) return;

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('config', JSON.stringify({ columns: columnRules }));

    showProgress();

    try {
        const response = await fetch(`${API_BASE}/sanitize`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) throw new Error('Job failed to start');

        const job = await response.json();
        currentJobId = job.jobExecutionId;
        pollJobStatus(job.jobExecutionId);
    } catch (error) {
        showError(error.message);
    }
}

function showProgress() {
    document.getElementById('submit-btn').disabled = true;
    document.getElementById('progress-section').classList.remove('hidden');
    document.getElementById('result-section').classList.add('hidden');
    document.getElementById('diff-section').classList.add('hidden');
}

async function pollJobStatus(jobId) {
    const poll = async () => {
        try {
            const res = await fetch(`${API_BASE}/jobs/${jobId}`);
            const job = await res.json();

            const status = job.status;
            document.getElementById('progress-status').textContent = `Status: ${status} | Rows: ${(job.rowsProcessed || 0).toLocaleString()}`;

            if (status === 'COMPLETED' || status === 'SUCCESS') {
                document.getElementById('progress-bar').style.width = '100%';
                document.getElementById('progress-percent').textContent = '100%';
                showResult(job);
            } else if (status === 'FAILED') {
                showError('Job failed: ' + (job.exitDescription || 'Unknown error'));
            } else {
                setTimeout(poll, 2000);
            }
        } catch (e) {
            showError('Failed to check status');
        }
    };
    poll();
}

function showResult(job) {
    document.getElementById('progress-section').classList.add('hidden');
    document.getElementById('result-section').classList.remove('hidden');

    const outputFileName = job.outputFile?.split(/[/\\]/).pop() || 'sanitized.csv';

    document.getElementById('result-details').innerHTML = `
        <p><strong>Job ID:</strong> ${job.jobExecutionId}</p>
        <p><strong>Rows Processed:</strong> ${(job.rowsProcessed || 0).toLocaleString()}</p>
        <p><strong>Rows Skipped:</strong> ${job.rowsSkipped || 0}</p>
        <p><strong>Output File:</strong> ${outputFileName}</p>
        <div class="mt-4 flex flex-wrap gap-3">
            <a href="${API_BASE}/jobs/${job.jobExecutionId}/download" 
               class="inline-flex items-center px-4 py-2 bg-gradient-to-r from-green-500 to-emerald-600 rounded-lg font-medium hover:from-green-400 hover:to-emerald-500 transition-all">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                </svg>
                Download CSV
            </a>
            <a href="diff.html?jobId=${job.jobExecutionId}" 
                class="inline-flex items-center px-4 py-2 bg-gradient-to-r from-purple-500 to-indigo-600 rounded-lg font-medium hover:from-purple-400 hover:to-indigo-500 transition-all">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
            </svg>
            View Changes
        </a>
        <button onclick="resetUpload()" 
                class="inline-flex items-center px-4 py-2 bg-white/10 border border-white/20 rounded-lg font-medium hover:bg-white/20 transition-all">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
            </svg>
            New Job
        </button>
    </div>
`;
}

function showDiff(jobId) {
    // Legacy support or fallback to redirection
    window.location.href = `diff.html?jobId=${jobId}`;
}



function renderDiff(diff) {
    const diffContent = document.getElementById('diff-content');

    if (!diff.rows || diff.rows.length === 0) {
        diffContent.innerHTML = '<div class="text-center py-8 text-gray-400">No changes to display</div>';
        return;
    }

    // Summary
    let html = `
        <div class="mb-6 p-4 rounded-xl bg-white/5">
            <h4 class="font-semibold mb-2">Change Summary</h4>
            <p class="text-sm text-gray-400">Total changes: <span class="text-white font-medium">${diff.totalChanges}</span></p>
            <div class="flex flex-wrap gap-2 mt-2">
                ${Object.entries(diff.changesByColumn || {}).map(([col, count]) =>
        `<span class="px-2 py-1 rounded-full text-xs bg-purple-500/20 text-purple-300">${col}: ${count}</span>`
    ).join('')}
            </div>
        </div>
    `;

    // Diff table
    html += `<div class="overflow-x-auto"><table class="w-full text-sm">`;
    html += `<thead class="bg-white/5"><tr><th class="px-3 py-2 text-left text-xs text-gray-400">Row</th>`;
    diff.headers.forEach(h => {
        html += `<th class="px-3 py-2 text-left text-xs text-gray-400">${h}</th>`;
    });
    html += `</tr></thead><tbody class="divide-y divide-white/10">`;

    diff.rows.forEach(row => {
        html += `<tr class="hover:bg-white/5"><td class="px-3 py-2 text-gray-500">#${row.rowNumber}</td>`;
        row.cells.forEach(cell => {
            if (cell.changed) {
                html += `
                    <td class="px-3 py-2">
                        <div class="text-red-400 line-through text-xs mb-1" title="${escapeHtml(cell.originalValue)}">${truncate(cell.originalValue, 20)}</div>
                        <div class="text-green-400 text-xs" title="${escapeHtml(cell.sanitizedValue)}">${truncate(cell.sanitizedValue, 20)}</div>
                    </td>`;
            } else {
                html += `<td class="px-3 py-2 text-gray-500 text-xs">${truncate(cell.originalValue, 20)}</td>`;
            }
        });
        html += `</tr>`;
    });

    html += `</tbody></table></div>`;
    diffContent.innerHTML = html;
}

function truncate(str, len) {
    if (!str) return '';
    return str.length > len ? str.substring(0, len) + '...' : str;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showError(msg) {
    document.getElementById('progress-section').classList.add('hidden');
    alert('Error: ' + msg);
    document.getElementById('submit-btn').disabled = false;
}

async function checkHealth() {
    try {
        const res = await fetch(`${API_BASE}/health`);
        if (res.ok) {
            document.getElementById('health-status').innerHTML = '<span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span><span>Online</span>';
        }
    } catch (e) {
        document.getElementById('health-status').innerHTML = '<span class="w-2 h-2 rounded-full bg-red-400"></span><span>Offline</span>';
    }
}

function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB';
}
