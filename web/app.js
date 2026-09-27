// ============================================================
//  ELEMENTS
// ============================================================
const runBtn         = document.getElementById('runBtn');
const browseBtn      = document.getElementById('browseBtn');
const datasetInput   = document.getElementById('datasetPath');
const datasetName    = document.getElementById('datasetName');
const checkboxes     = document.querySelectorAll('.chip-v input[type=checkbox]');
const progressStrip  = document.getElementById('progressStrip');
const progressFill   = document.getElementById('progressFill');
const progressLabel  = document.getElementById('progressLabel');
const logSection     = document.getElementById('logSection');
const logEl          = document.getElementById('log');

const recommendation = document.getElementById('recommendation');
const recTitle       = document.getElementById('recTitle');
const recDesc        = document.getElementById('recDesc');
const recGmean       = document.getElementById('recGmean');
const recReduction   = document.getElementById('recReduction');
const recTime        = document.getElementById('recTime');

const optimizerGrid  = document.getElementById('optimizerGrid');
const chartsGrid     = document.getElementById('chartsGrid');
const resultsSection = document.getElementById('resultsSection');
const resultsBody    = document.getElementById('resultsBody');
const baselineSection= document.getElementById('baselineSection');
const baselineBody   = document.getElementById('baselineBody');
const featuresSection= document.getElementById('featuresSection');
const featureList    = document.getElementById('featureList');

const topOptimizerPill = document.getElementById('topOptimizerPill');
const activeClfPill    = document.getElementById('activeClfPill');
const metaSamples      = document.getElementById('metaSamples');
const metaFeatures     = document.getElementById('metaFeatures');
const metaClusters     = document.getElementById('metaClusters');

// ============================================================
//  STATE
// ============================================================
let allGroups      = [];
let selectedGroups = {};
let convergenceChart = null;
let comparisonChart  = null;
let baselineRows     = [];

// ============================================================
//  FILE PICKER
// ============================================================
browseBtn.addEventListener('click', pickFile);

async function pickFile() {
  browseBtn.disabled = true;
  try {
    const res  = await fetch('/pick-file');
    const data = await res.json();
    if (data.path) {
      datasetInput.value = data.path;
      datasetName.textContent = data.path;
    }
  } catch (err) {
    alert('Could not open file picker: ' + err.message);
  } finally {
    browseBtn.disabled = false;
  }
}

// ============================================================
//  RUN
// ============================================================
runBtn.addEventListener('click', runPipeline);

function runPipeline() {
  const selected = Array.from(checkboxes).filter(c => c.checked).map(c => c.value);
  if (!selected.length) return alert('Select at least one classifier.');

  const path = datasetInput.value.trim();
  if (!path) return alert('Choose a dataset first.');

  logEl.textContent = '';
  featureList.innerHTML = '';
  resultsBody.innerHTML = '';
  baselineBody.innerHTML = '';
  recommendation.hidden = true;
  optimizerGrid.hidden = true;
  chartsGrid.hidden = true;
  resultsSection.hidden = true;
  baselineSection.hidden = true;
  featuresSection.hidden = true;
  logSection.hidden = false;
  progressStrip.hidden = false;
  progressFill.style.width = '0%';
  progressLabel.textContent = '0%';

  allGroups = [];
  selectedGroups = {};
  baselineRows = [];

  runBtn.disabled = true;
  browseBtn.disabled = true;

  const url = '/run?dataset=' + encodeURIComponent(path)
            + '&classifiers=' + encodeURIComponent(selected.join(','));

  const source = new EventSource(url);

  source.addEventListener('log', e => {
    logEl.textContent += e.data + '\n';
    logEl.scrollTop = logEl.scrollHeight;
  });

  source.addEventListener('progress', e => {
    progressFill.style.width = e.data + '%';
    progressLabel.textContent = e.data + '%';
  });

  source.addEventListener('groups', e => {
    allGroups = JSON.parse(e.data);
    metaClusters.textContent = allGroups.length;
  });

  source.addEventListener('selectedGroups', e => {
    const data = JSON.parse(e.data);
    selectedGroups[data.classifier] = {
      gaGroups: data.gaGroups,
      psoGroups: data.psoGroups
    };
    renderFeatureGroups(allGroups, selectedGroups);
    featuresSection.hidden = false;
  });

  source.addEventListener('baseline', e => {
    baselineRows = JSON.parse(e.data);
    renderBaseline(baselineRows);
    baselineSection.hidden = false;
  });

  source.addEventListener('results', e => {
    const data = JSON.parse(e.data);
    renderResults(data);
    renderRecommendation(data);
    renderOptimizerCards(data);
    renderConvergenceChart();
    renderComparisonChart(data);
    resultsSection.hidden = false;
    optimizerGrid.hidden = false;
    chartsGrid.hidden = false;
    recommendation.hidden = false;
    topOptimizerPill.textContent = pickTopOptimizer(data);
    activeClfPill.textContent    = data[0]?.classifier || '—';
  });

  source.addEventListener('done', () => {
    source.close();
    runBtn.disabled = false;
    browseBtn.disabled = false;
    progressLabel.textContent = 'Done';
    progressFill.style.width = '100%';
  });

  source.onerror = () => {
    source.close();
    runBtn.disabled = false;
    browseBtn.disabled = false;
  };
}

// ============================================================
//  RENDER HELPERS
// ============================================================
function renderResults(rows) {
  resultsBody.innerHTML = '';
  rows.forEach(r => {
    const c = r.classifier;
    resultsBody.insertAdjacentHTML('beforeend', `
      <tr>
        <td><span class="clf-${c}">${c}</span></td>
        <td>GA</td>
        <td>${r.gaGMean.toFixed(4)}</td>
        <td>${r.gaF1.toFixed(4)}</td>
        <td>${r.gaPrecision.toFixed(4)}</td>
        <td>${r.gaRecall.toFixed(4)}</td>
        <td>${r.gaFeatures}</td>
        <td>${r.gaDurationMs}</td>
      </tr>
      <tr>
        <td><span class="clf-${c}">${c}</span></td>
        <td>PSO</td>
        <td>${r.psoGMean.toFixed(4)}</td>
        <td>${r.psoF1.toFixed(4)}</td>
        <td>${r.psoPrecision.toFixed(4)}</td>
        <td>${r.psoRecall.toFixed(4)}</td>
        <td>${r.psoFeatures}</td>
        <td>${r.psoDurationMs}</td>
      </tr>
    `);
  });
}

function renderBaseline(rows) {
  baselineBody.innerHTML = '';
  rows.forEach(r => {
    baselineBody.insertAdjacentHTML('beforeend', `
      <tr>
        <td><span class="clf-${r.classifier}">${r.classifier}</span></td>
        <td>${r.gmean.toFixed(4)}</td>
        <td>${r.f1.toFixed(4)}</td>
        <td>${r.precision.toFixed(4)}</td>
        <td>${r.recall.toFixed(4)}</td>
        <td>${r.numFeatures}</td>
        <td>${r.durationMs}</td>
      </tr>
    `);
  });
}

function renderFeatureGroups(groups, selectedMap) {
  if (!groups.length) {
    featureList.innerHTML = '<div style="color:#7a94b4;padding:20px;text-align:center;">No groups yet.</div>';
    return;
  }
  const inGA = new Set(), inPSO = new Set();
  Object.values(selectedMap).forEach(entry => {
    (entry.gaGroups  || []).forEach(g => inGA.add(g));
    (entry.psoGroups || []).forEach(g => inPSO.add(g));
  });

  featureList.innerHTML = '';
  groups.forEach(g => {
    const ga = inGA.has(g.id), pso = inPSO.has(g.id);
    let cls = '', badge = '<span class="selected-badge none">—</span>';
    if (ga && pso)      { cls = 'selected-both'; badge = '<span class="selected-badge both">GA + PSO</span>'; }
    else if (ga)        { cls = 'selected-ga';   badge = '<span class="selected-badge ga">GA</span>'; }
    else if (pso)       { cls = 'selected-pso';  badge = '<span class="selected-badge pso">PSO</span>'; }

    const chips = g.features.map(f => `<span class="feature-chip">${f}</span>`).join('');
    featureList.insertAdjacentHTML('beforeend', `
      <div class="feature-row ${cls}">
        <div class="group-id">G${g.id}</div>
        <div class="feature-chips">${chips}</div>
        <div>${badge}</div>
      </div>
    `);
  });
}

function pickTopOptimizer(rows) {
  let bestGA = 0, bestPSO = 0;
  rows.forEach(r => {
    if (r.gaGMean  > bestGA)  bestGA  = r.gaGMean;
    if (r.psoGMean > bestPSO) bestPSO = r.psoGMean;
  });
  return bestGA >= bestPSO ? 'Genetic Algorithm' : 'Particle Swarm';
}

function renderRecommendation(rows) {
  let best = null;
  rows.forEach(r => {
    if (!best || r.gaGMean > best.gmean)
      best = { gmean: r.gaGMean, features: r.gaFeatures, time: r.gaDurationMs,
               method: 'GA', classifier: r.classifier };
    if (r.psoGMean > best.gmean)
      best = { gmean: r.psoGMean, features: r.psoFeatures, time: r.psoDurationMs,
               method: 'PSO', classifier: r.classifier };
  });

  const reduction = ((95 - best.features) / 95 * 100).toFixed(1);
  recTitle.textContent    = `${best.method} on ${best.classifier}`;
  recDesc.textContent     = `Best G-Mean ${best.gmean.toFixed(4)} using ${best.features} features`;
  recGmean.textContent    = best.gmean.toFixed(3);
  recReduction.textContent= reduction + '%';
  recTime.textContent     = best.time + ' ms';
}

function renderOptimizerCards(rows) {
  // Aggregate best per optimizer across classifiers
  let bestGA = 0, bestPSO = 0;
  let gaFeat = 0, psoFeat = 0;
  let gaTime = 0, psoTime = 0;
  let gaF1 = 0, psoF1 = 0;

  rows.forEach(r => {
    if (r.gaGMean > bestGA) { bestGA = r.gaGMean; gaFeat = r.gaFeatures; gaTime = r.gaDurationMs; gaF1 = r.gaF1; }
    if (r.psoGMean > bestPSO) { bestPSO = r.psoGMean; psoFeat = r.psoFeatures; psoTime = r.psoDurationMs; psoF1 = r.psoF1; }
  });

  document.getElementById('gaGmean').textContent    = bestGA.toFixed(4);
  document.getElementById('gaF1').textContent       = gaF1.toFixed(4);
  document.getElementById('gaFeatures').textContent = gaFeat;
  document.getElementById('gaTime').textContent     = gaTime + ' ms';

  document.getElementById('psoGmean').textContent    = bestPSO.toFixed(4);
  document.getElementById('psoF1').textContent       = psoF1.toFixed(4);
  document.getElementById('psoFeatures').textContent = psoFeat;
  document.getElementById('psoTime').textContent     = psoTime + ' ms';

  const gaRed = ((95 - gaFeat) / 95 * 100).toFixed(1);
  const psoRed = ((95 - psoFeat) / 95 * 100).toFixed(1);

  document.getElementById('gaReduction').textContent  = gaRed + '%';
  document.getElementById('psoReduction').textContent = psoRed + '%';
  document.getElementById('gaBar').style.width  = gaRed + '%';
  document.getElementById('psoBar').style.width = psoRed + '%';

  // Mark best card
  const cardGA  = document.getElementById('cardGA');
  const cardPSO = document.getElementById('cardPSO');
  const badgeGA  = document.getElementById('badgeGA');
  const badgePSO = document.getElementById('badgePSO');

  cardGA.classList.remove('best');
  cardPSO.classList.remove('best');
  badgeGA.classList.add('hidden');
  badgePSO.classList.add('hidden');

  if (bestGA >= bestPSO) {
    cardGA.classList.add('best');
    badgeGA.textContent = 'Best Performer';
    badgeGA.classList.remove('hidden');
  } else {
    cardPSO.classList.add('best');
    badgePSO.textContent = 'Best Performer';
    badgePSO.classList.remove('hidden');
  }
}

function renderConvergenceChart() {
  if (convergenceChart) convergenceChart.destroy();
  const ctx = document.getElementById('convergenceChart').getContext('2d');

  // Placeholder — replace with real data if you emit it
  const iterations = Array.from({ length: 20 }, (_, i) => i);
  const gaCurve  = iterations.map(i => 40 + 55 * (1 - Math.exp(-i / 5)));
  const psoCurve = iterations.map(i => 42 + 53 * (1 - Math.exp(-i / 4)));

  convergenceChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: iterations,
      datasets: [
        { label: 'GA',  data: gaCurve,  borderColor: '#3ba7ff', backgroundColor: 'rgba(59,167,255,0.1)', tension: 0.4, borderWidth: 2, fill: true, pointRadius: 0 },
        { label: 'PSO', data: psoCurve, borderColor: '#22d3ee', backgroundColor: 'rgba(34,211,238,0.05)', tension: 0.4, borderWidth: 2, pointRadius: 0 }
      ]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { color: '#7a94b4', font: { size: 11 } } } },
      scales: {
        x: { grid: { color: 'rgba(122,148,180,0.1)' }, ticks: { color: '#7a94b4', font: { size: 10 } } },
        y: { grid: { color: 'rgba(122,148,180,0.1)' }, ticks: { color: '#7a94b4', font: { size: 10 } } }
      }
    }
  });
}

function renderComparisonChart(rows) {
  if (comparisonChart) comparisonChart.destroy();
  const ctx = document.getElementById('comparisonChart').getContext('2d');

  const classifiers = rows.map(r => r.classifier);
  const gaData  = rows.map(r => r.gaGMean);
  const psoData = rows.map(r => r.psoGMean);

  comparisonChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: classifiers,
      datasets: [
        { label: 'GA Selected',  data: gaData,  backgroundColor: '#3ba7ff' },
        { label: 'PSO Selected', data: psoData, backgroundColor: '#22d3ee' }
      ]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { color: '#7a94b4', font: { size: 11 } } } },
      scales: {
        x: { grid: { display: false }, ticks: { color: '#7a94b4', font: { size: 11 } } },
        y: { grid: { color: 'rgba(122,148,180,0.1)' }, ticks: { color: '#7a94b4', font: { size: 10 } }, beginAtZero: true }
      }
    }
  });
}