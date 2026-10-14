const $ = id => document.getElementById(id);

const CLF_NAME = { KNN: 'KNN', SVM: 'SVM', NB: 'Naive Bayes' };
const COL = { base: '#3b82f6', ga: '#10b981', pso: '#8b5cf6' };
const TXT = '#475569', GRID = '#e2e8f0';
const METRIC = {
  gmean:     { b: 'gmean',     ga: 'gaGMean',     pso: 'psoGMean' },
  f1:        { b: 'f1',        ga: 'gaF1',        pso: 'psoF1' },
  precision: { b: 'precision', ga: 'gaPrecision', pso: 'psoPrecision' },
  recall:    { b: 'recall',    ga: 'gaRecall',    pso: 'psoRecall' }
};

let allGroups = [], selectedGroups = {}, baselineRows = [], resultRows = [];
let totalFeatures = 0, currentTab = 'GA', source = null;
let perfChart = null, convChart = null;

const runBtn = $('runBtn'), browseBtn = $('browseBtn');
const checkboxes = document.querySelectorAll('.classifier-chip input[type=checkbox]');
const groupingToggle = $('groupingToggle');

/* ---------------- Helpers ---------------- */
const sign = (v, d = 4) => (v >= 0 ? '+' : '') + v.toFixed(d);
const pctFewer = n => totalFeatures ? Math.round((totalFeatures - n) / totalFeatures * 100) : 0;

function appendLog(msg, cls = '') {
  const el = $('console-output'), line = document.createElement('div');
  line.className = 'log-line ' + cls;
  line.textContent = msg;
  el.appendChild(line);
  el.scrollTop = el.scrollHeight;
}
function setStatus(text, cls) { const s = $('log-status'); s.textContent = text; s.className = 'badge log-badge ' + (cls || ''); }
function setProgress(p) { $('progress-fill').style.width = p + '%'; $('progress-percentage').textContent = p + '%'; }

function getBest(rows) {
  let best = null;
  rows.forEach(r => [
    { method: 'GA',  gmean: r.gaGMean,  f1: r.gaF1,  features: r.gaFeatures,  time: r.gaDurationMs,  classifier: r.classifier },
    { method: 'PSO', gmean: r.psoGMean, f1: r.psoF1, features: r.psoFeatures, time: r.psoDurationMs, classifier: r.classifier }
  ].forEach(o => { if (!best || o.gmean > best.gmean) best = o; }));
  return best;
}

function groupFrequency() {
  const freq = {}; let runs = 0;
  Object.values(selectedGroups).forEach(s => {
    runs += 2;
    (s.gaGroups || []).forEach(g => freq[g] = (freq[g] || 0) + 1);
    (s.psoGroups || []).forEach(g => freq[g] = (freq[g] || 0) + 1);
  });
  return { freq, runs };
}

/* ---------------- File picker ---------------- */
browseBtn.addEventListener('click', async () => {
  browseBtn.disabled = true;
  try {
    const data = await (await fetch('/pick-file')).json();
    if (data.path) {
      $('datasetPath').value = data.path;
      $('datasetName').textContent = data.path;
      $('dataset-badge').textContent = 'Custom dataset';
    }
  } catch (err) {
    alert('Could not open file picker: ' + err.message);
  } finally { browseBtn.disabled = false; }
});

/* ---------------- Run ---------------- */
runBtn.addEventListener('click', runPipeline);

function resetUI() {
  allGroups = []; selectedGroups = {}; baselineRows = []; resultRows = []; totalFeatures = 0;
  $('console-output').innerHTML = '';
  $('groups-container').innerHTML = '<div class="placeholder-text">Groups appear here once the run starts.</div>';
  $('group-count-badge').textContent = '0 groups';
  $('selected-features-tags').innerHTML = '<span class="placeholder-text">Run feature selection to see the chosen features.</span>';
  $('selected-count').textContent = '0'; $('selected-ratio').textContent = '0%';
  $('kpi-total-features').textContent = '--'; $('kpi-num-groups').textContent = '--';
  $('kpi-selected').textContent = '--'; $('kpi-f1-lift').textContent = '--';
  $('kpi-selected-sub').textContent = 'Best result';
  $('summarySection').hidden = true; $('resultsSection').hidden = true; $('convergenceCard').hidden = true;
  $('resultsBody').innerHTML = ''; $('exportBtn').disabled = true;
  updateChart(); setProgress(0);
}

function runPipeline() {
  const selected = Array.from(checkboxes).filter(c => c.checked).map(c => c.value);
  if (!selected.length) return alert('Select at least one classifier.');
  const path = $('datasetPath').value.trim();
  if (!path) return alert('Choose a dataset first.');

  const useGrouping = groupingToggle.checked;

  resetUI();
  appendLog(`[iSelect] Starting run on ${path} with ${selected.join(', ')} (Pearson grouping: ${useGrouping ? 'ON' : 'OFF'})`, 'system');
  setStatus('Running...', 'running');
  runBtn.disabled = true; browseBtn.disabled = true;

  let finished = false;
  source = new EventSource('/run?dataset=' + encodeURIComponent(path)
          + '&classifiers=' + encodeURIComponent(selected.join(','))
          + '&grouping=' + useGrouping);

  source.addEventListener('log', e => appendLog(e.data));
  source.addEventListener('progress', e => setProgress(e.data));

  source.addEventListener('groups', e => {
    allGroups = JSON.parse(e.data);
    totalFeatures = allGroups.reduce((s, g) => s + g.features.length, 0);
    $('kpi-total-features').textContent = totalFeatures;
    $('kpi-num-groups').textContent = allGroups.length;
    $('group-count-badge').textContent = allGroups.length + ' groups';
    renderGroups();
  });

  source.addEventListener('selectedGroups', e => {
    const d = JSON.parse(e.data);
    const ga = d.gaGroups || d.ga || [], pso = d.psoGroups || d.pso || [];
    selectedGroups[d.classifier] = { gaGroups: ga, psoGroups: pso };
    appendLog(`[iSelect] ${d.classifier}: GA picked ${ga.length} groups, PSO picked ${pso.length}`, 'system');
    syncClassifierSelect();
    renderGroups(); renderSelected();
    $('exportBtn').disabled = false;
  });

  source.addEventListener('baseline', e => { baselineRows = JSON.parse(e.data); refreshResults(); });

  source.addEventListener('convergence', e => renderConvergence(JSON.parse(e.data)));

  source.addEventListener('results', e => { resultRows = JSON.parse(e.data); renderSelected(); refreshResults(); });

  source.addEventListener('done', () => {
    finished = true; source.close();
    runBtn.disabled = false; browseBtn.disabled = false;
    setProgress(100); setStatus('Completed', 'success');
    appendLog('[iSelect] Run complete.', 'system');
  });

  source.onerror = () => {
    source.close();
    runBtn.disabled = false; browseBtn.disabled = false;
    if (!finished) { setStatus('Error', 'error'); appendLog('[iSelect] Connection to the pipeline was lost.', 'system'); }
  };
}

/* ---------------- Feature groups ---------------- */
function renderGroups() {
  const box = $('groups-container'), { freq, runs } = groupFrequency();
  if (!allGroups.length) return;
  box.innerHTML = '';
  allGroups.forEach(g => {
    const n = freq[g.id] || 0, pct = runs ? Math.round(n / runs * 100) : 0;
    const cls = pct >= 66 ? '' : pct >= 33 ? 'mid' : 'low';
    const freqHtml = runs
      ? `<div class="freq-line"><span>Selected in ${n} of ${runs} runs</span><span class="freq-pct">${pct}%</span>
           <div class="freq-bar"><div class="freq-fill ${cls}" style="width:${pct}%"></div></div></div>` : '';
    box.insertAdjacentHTML('beforeend', `
      <div class="group-item ${n ? 'picked' : ''}">
        <div class="group-header">
          <span class="group-title">Group ${g.id}</span>
          <span class="group-size">${g.features.length} features</span>
        </div>
        <div class="group-tags">${g.features.map(f => `<span class="feature-tag">${f}</span>`).join('')}</div>
        ${freqHtml}
      </div>`);
  });
}

/* ---------------- Selected features ---------------- */
document.querySelectorAll('.tab-btn').forEach(btn => btn.addEventListener('click', () => {
  currentTab = btn.dataset.algo;
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b === btn));
  renderSelected();
}));
$('classifier-select').addEventListener('change', renderSelected);

function syncClassifierSelect() {
  const keys = Object.keys(selectedGroups), sel = $('classifier-select');
  if (!keys.length) return;
  const current = keys.includes(sel.value) ? sel.value : keys[0];
  sel.innerHTML = keys.map(k => `<option value="${k}">${CLF_NAME[k] || k}</option>`).join('');
  sel.value = current;
}

const findGroup = id => allGroups.find(x => String(x.id) === String(id));

function renderSelected() {
  const clf = $('classifier-select').value, box = $('selected-features-tags'), s = selectedGroups[clf];
  if (!s) {
    box.innerHTML = '<span class="placeholder-text">No selection data for this classifier yet.</span>';
    $('selected-count').textContent = '0'; $('selected-ratio').textContent = '0%';
    return;
  }
  const ids = currentTab === 'GA' ? s.gaGroups : s.psoGroups;
  const row = resultRows.find(r => r.classifier === clf);
  const count = row ? (currentTab === 'GA' ? row.gaFeatures : row.psoFeatures) : ids.length;
  $('selected-count').textContent = count;
  $('selected-ratio').textContent = pctFewer(count) + '%';
  box.innerHTML = ids.map(id => {
    const g = findGroup(id);
    if (!g) return `<div class="sel-group"><b>Group ${id}</b></div>`;
    return `<div class="sel-group"><b>Group ${id}</b><span class="group-size">${g.features.length} features in group</span>
      <div class="group-tags">${g.features.map(f => `<span class="feature-tag active">${f}</span>`).join('')}</div></div>`;
  }).join('') || '<span class="placeholder-text">No groups selected.</span>';
}

$('exportBtn').addEventListener('click', () => {
  const rows = ['optimizer,classifier,group,features'];
  Object.entries(selectedGroups).forEach(([clf, s]) => {
    [['GA', s.gaGroups], ['PSO', s.psoGroups]].forEach(([opt, ids]) => ids.forEach(id => {
      const g = findGroup(id);
      if (g) rows.push(`${opt},${clf},${id},"${g.features.join(' ')}"`);
    }));
  });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([rows.join('\n')], { type: 'text/csv' }));
  a.download = 'iselect_selected_features.csv';
  a.click();
});

/* ---------------- Results ---------------- */
function refreshResults() {
  if (!resultRows.length) return;
  const best = getBest(resultRows);
  const base = baselineRows.find(b => b.classifier === best.classifier);

  $('kpi-selected').textContent = `${best.features} / ${totalFeatures || '?'}`;
  $('kpi-selected-sub').textContent = `${pctFewer(best.features)}% fewer features (${best.method} + ${CLF_NAME[best.classifier] || best.classifier})`;
  const lift = $('kpi-f1-lift');
  if (base) {
    const d = best.f1 - base.f1;
    lift.textContent = sign(d);
    lift.style.color = d >= 0 ? 'var(--success-color)' : 'var(--danger-color)';
  } else { lift.textContent = '--'; }

  let t = `${best.method} with ${CLF_NAME[best.classifier] || best.classifier} selected ${best.features}` +
          (totalFeatures ? ` of ${totalFeatures} features (${pctFewer(best.features)}% fewer)` : ' features') +
          `, reaching G-Mean ${best.gmean.toFixed(3)} and F1 ${best.f1.toFixed(3)} in ${best.time} ms.`;
  if (base) t += ` Using all ${base.numFeatures} features gave G-Mean ${base.gmean.toFixed(3)} and F1 ${base.f1.toFixed(3)}.`;
  $('summaryText').textContent = t;
  $('summarySection').hidden = false;

  renderTable(best);
  updateChart();
}

function renderTable(best) {
  const body = $('resultsBody');
  body.innerHTML = '';
  resultRows.forEach(r => {
    const c = r.classifier, name = `<span class="clf-${c}">${CLF_NAME[c] || c}</span>`;
    const b = baselineRows.find(x => x.classifier === c);
    if (b) body.insertAdjacentHTML('beforeend', `
      <tr class="row-baseline"><td>${name}</td><td>All features</td><td>${b.gmean.toFixed(4)}</td><td>${b.f1.toFixed(4)}</td>
        <td>${b.precision.toFixed(4)}</td><td>${b.recall.toFixed(4)}</td><td>${b.numFeatures}</td><td>${b.durationMs}</td></tr>`);
    [['GA', 'ga'], ['PSO', 'pso']].forEach(([label, k]) => {
      const isBest = best.classifier === c && best.method === label;
      body.insertAdjacentHTML('beforeend', `
        <tr class="${isBest ? 'row-best' : ''}"><td>${name}</td><td>${label}</td><td>${r[k + 'GMean'].toFixed(4)}</td><td>${r[k + 'F1'].toFixed(4)}</td>
          <td>${r[k + 'Precision'].toFixed(4)}</td><td>${r[k + 'Recall'].toFixed(4)}</td><td>${r[k + 'Features']}</td><td>${r[k + 'DurationMs']}</td></tr>`);
    });
  });
  $('resultsSection').hidden = false;
}

function initChart() {
  perfChart = new Chart($('performanceChart').getContext('2d'), {
    type: 'bar',
    data: { labels: [], datasets: [] },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { color: TXT } } },
      scales: {
        y: { beginAtZero: true, max: 1.0, grid: { color: GRID }, ticks: { color: TXT } },
        x: { grid: { display: false }, ticks: { color: TXT } }
      }
    }
  });
}

function updateChart() {
  const m = METRIC[$('metric-select').value];
  perfChart.data.labels = resultRows.map(r => CLF_NAME[r.classifier] || r.classifier);
  const ds = [];
  if (baselineRows.length) ds.push({
    label: 'All features', backgroundColor: COL.base,
    data: resultRows.map(r => (baselineRows.find(b => b.classifier === r.classifier) || {})[m.b] || 0)
  });
  ds.push({ label: 'GA selected', backgroundColor: COL.ga,  data: resultRows.map(r => r[m.ga]) });
  ds.push({ label: 'PSO selected', backgroundColor: COL.pso, data: resultRows.map(r => r[m.pso]) });
  perfChart.data.datasets = resultRows.length ? ds : [];
  perfChart.update();
}
$('metric-select').addEventListener('change', updateChart);

function renderConvergence(d) {
  if (!d || !(d.ga || d.pso)) return;
  if (convChart) convChart.destroy();
  $('convergenceCard').hidden = false;
  const n = Math.max((d.ga || []).length, (d.pso || []).length);
  convChart = new Chart($('convergenceChart').getContext('2d'), {
    type: 'line',
    data: {
      labels: Array.from({ length: n }, (_, i) => i + 1),
      datasets: [
        { label: 'GA',  data: d.ga  || [], borderColor: COL.ga,  tension: 0.3, borderWidth: 2, pointRadius: 0 },
        { label: 'PSO', data: d.pso || [], borderColor: COL.pso, tension: 0.3, borderWidth: 2, pointRadius: 0 }
      ]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { color: TXT } } },
      scales: { x: { grid: { color: GRID }, ticks: { color: TXT } }, y: { grid: { color: GRID }, ticks: { color: TXT } } }
    }
  });
}

window.addEventListener('load', initChart);