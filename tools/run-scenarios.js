// Runs a lab's test scenarios against a local Camunda 8 Run cluster and records the real results.
// Usage: node tools/run-scenarios.js lab1-loan-approval
// Needs Node 18+, Camunda 8 Run and the lab's worker app. No npm packages are required.
const fs = require('fs');
const path = require('path');

const API = 'http://localhost:8080/v2';
const lab = process.argv[2];
const labDir = path.join(__dirname, '..', lab);
const cfg = JSON.parse(fs.readFileSync(path.join(labDir, 'tests', 'scenarios.json'), 'utf8'));
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function api(method, url, body) {
  const res = await fetch(API + url, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${method} ${url} -> ${res.status} ${text}`);
  return text ? JSON.parse(text) : {};
}
const search = async (what, filter) => (await api('POST', `/${what}/search`, { filter })).items || [];

async function deploy() {
  const form = new FormData();
  for (const f of cfg.resources) {
    const file = path.join(labDir, 'models', f);
    form.append('resources', new Blob([fs.readFileSync(file)]), f);
  }
  const res = await fetch(`${API}/deployments`, { method: 'POST', body: form });
  const json = await res.json();
  if (!res.ok) throw new Error(JSON.stringify(json));
  return json;
}

async function setFault(name, down) {
  await fetch(`${cfg.workerUrl}/admin/fault?name=${name}&down=${down}`, { method: 'POST' });
}

async function runScenario(sc) {
  const started = Date.now();
  if (sc.fault) await setFault(sc.fault, true);
  const inst = await api('POST', '/process-instances', { processDefinitionId: cfg.processId, variables: sc.variables });
  const key = String(inst.processInstanceKey);
  const notes = [];
  const done = new Set();
  let state = 'ACTIVE';

  for (let i = 0; i < 180 && state !== 'COMPLETED'; i++) {
    await sleep(1000);
    const incidents = sc.fault ? await search('incidents', { processInstanceKey: key, state: 'ACTIVE' }) : [];
    for (const inc of incidents) {
      notes.push(`Incident on ${inc.elementId}: ${inc.errorMessage}`.slice(0, 200));
      await setFault(sc.fault, false);
      if (inc.jobKey) await api('PATCH', `/jobs/${inc.jobKey}`, { changeset: { retries: 1 } });
      await api('POST', `/incidents/${inc.incidentKey}/resolution`, {});
      notes.push('Cause fixed, job retried');
      await sleep(2000);
    }
    const tasks = await search('user-tasks', { processInstanceKey: key, state: 'CREATED' });
    for (const t of tasks) {
      if (done.has(t.userTaskKey)) continue;
      const vars = (sc.review || {})[t.elementId];
      if (!vars) throw new Error(`${sc.id}: no answer for user task ${t.elementId}`);
      await api('POST', `/user-tasks/${t.userTaskKey}/completion`, { variables: vars });
      done.add(t.userTaskKey);
      notes.push(`Completed task ${t.elementId}`);
    }
    state = (await search('process-instances', { processInstanceKey: key }))[0]?.state || state;
  }

  await sleep(2500);
  const elements = await search('element-instances', { processInstanceKey: key });
  const end = elements.find(e => e.type === 'END_EVENT')?.elementId || '(not finished)';
  const vars = await search('variables', { processInstanceKey: key });
  const value = n => { const v = vars.find(x => x.name === n); return v ? JSON.parse(v.value) : undefined; };
  const status = value(cfg.statusVariable);
  const pass = state === 'COMPLETED' && end === sc.expectedEnd && status === sc.expectedStatus;
  if (sc.fault) await setFault(sc.fault, false);
  return { id: sc.id, name: sc.name, input: sc.inputSummary, expectedRoute: sc.expectedRoute, expectedEnd: sc.expectedEnd, expectedStatus: sc.expectedStatus,
    actualEnd: end, actualStatus: status, processInstanceKey: key, state, result: pass ? 'PASS' : 'FAIL', notes, seconds: Math.round((Date.now() - started) / 1000) };
}

async function evaluateDecisions() {
  const out = [];
  for (const s of cfg.dmnSamples || []) {
    const r = await api('POST', '/decision-definitions/evaluation', { decisionDefinitionId: cfg.decisionId, variables: s });
    out.push({ input: s, output: JSON.parse(r.output) });
  }
  return out;
}

(async () => {
  const deployment = await deploy();
  console.log('Deployed', deployment.deployments.map(d => Object.values(d)[0].processDefinitionId || Object.values(d)[0].decisionDefinitionId || Object.values(d)[0].formId).join(', '));
  await sleep(3000);
  const results = [];
  for (const sc of cfg.scenarios) {
    const r = await runScenario(sc);
    console.log(`${r.id} ${r.result}  end=${r.actualEnd}  status=${r.actualStatus}  key=${r.processInstanceKey}  ${r.seconds}s`);
    results.push(r);
  }
  const dmn = await evaluateDecisions();
  const evidence = path.join(labDir, 'evidence');
  fs.writeFileSync(path.join(evidence, 'test-results.json'), JSON.stringify({ runAt: new Date().toISOString(), deployment, results, dmn }, null, 2));
  console.log(`${results.filter(r => r.result === 'PASS').length}/${results.length} passed`);
})().catch(e => { console.error(e); process.exit(1); });
