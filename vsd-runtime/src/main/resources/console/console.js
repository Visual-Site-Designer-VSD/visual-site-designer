'use strict';
(function () {
  var API = '/_vsd/console/api';
  var TYPES = ['string', 'number', 'integer', 'boolean', 'object', 'array', 'any'];
  var $ = function (id) { return document.getElementById(id); };
  var token = '';
  try { token = sessionStorage.getItem('vsd-admin-token') || ''; } catch (e) { /* private mode */ }
  var current = null; // name being edited, or null for a new one

  function el(tag, props, children) {
    var n = document.createElement(tag);
    Object.keys(props || {}).forEach(function (k) { n[k] = props[k]; });
    (children || []).forEach(function (c) { n.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return n;
  }

  function say(text, kind) {
    var m = $('message');
    m.textContent = text || '';
    m.className = kind || '';
  }

  function call(method, path, body) {
    var headers = { 'Accept': 'application/json' };
    if (token) headers['X-VSD-Admin-Token'] = token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    return fetch(API + path, {
      method: method, headers: headers, credentials: 'same-origin', cache: 'no-store',
      body: body === undefined ? undefined : JSON.stringify(body)
    }).then(function (res) {
      return res.json().catch(function () { return {}; }).then(function (json) {
        if (!res.ok) throw new Error(json.message || ('Request failed (' + res.status + ')'));
        return json;
      });
    });
  }

  // ---- tabs -----------------------------------------------------------------------------------

  document.querySelectorAll('.tab').forEach(function (b) {
    b.addEventListener('click', function () {
      document.querySelectorAll('.tab').forEach(function (x) { x.classList.toggle('active', x === b); });
      $('tab-state').hidden = b.dataset.tab !== 'state';
      $('tab-sessions').hidden = b.dataset.tab !== 'sessions';
      say('');
      if (b.dataset.tab === 'sessions') loadSessions(); else loadStates();
    });
  });

  $('token-form').addEventListener('submit', function (e) {
    e.preventDefault();
    token = $('token').value.trim();
    try { sessionStorage.setItem('vsd-admin-token', token); } catch (err) { /* ignore */ }
    $('token').value = '';
    loadStates();
  });

  // ---- shared state list ------------------------------------------------------------------------

  function loadStates() {
    call('GET', '/shared-state').then(function (data) {
      var list = $('state-list');
      list.textContent = '';
      if (!data.definitions.length) list.appendChild(el('li', { textContent: 'Nothing declared yet.' }));
      data.definitions.forEach(function (d) {
        var li = el('li', { className: d.name === current ? 'current' : '' }, [
          el('strong', { textContent: d.name }),
          el('small', { textContent: d.kind.toLowerCase() + ' · ' + d.lifetime.toLowerCase() +
            (d.writers.length ? ' · writers: ' + d.writers.join(', ') : '') +
            (d.clientWritable ? '' : ' · server-only writes') })
        ]);
        li.addEventListener('click', function () { openEditor(d); });
        list.appendChild(li);
      });
      $('strict-note').textContent = data.strict
        ? 'Strict mode: writes to undeclared session state are refused.'
        : 'Lenient mode: undeclared session state is still accepted (vsd.runtime.strict-shared-state=false).';
    }).catch(function (e) { say(e.message, 'error'); });
  }

  // ---- editor -----------------------------------------------------------------------------------

  var valueType = $('f-valuetype');
  TYPES.forEach(function (t) { valueType.appendChild(el('option', { value: t, textContent: t })); });

  function addFieldRow(name, f) {
    var type = el('select', {});
    TYPES.forEach(function (t) { type.appendChild(el('option', { value: t, textContent: t })); });
    type.value = (f && f.type) || 'string';
    var req = el('input', { type: 'checkbox', checked: !!(f && f.required) });
    var del = el('button', { type: 'button', textContent: '×' });
    var row = el('tr', {}, [
      el('td', {}, [el('input', { value: name || '', placeholder: 'field name', pattern: '[A-Za-z_][A-Za-z0-9_]{0,63}' })]),
      el('td', {}, [type]), el('td', {}, [req]), el('td', {}, [del])
    ]);
    del.addEventListener('click', function () { row.remove(); });
    $('fields').tBodies[0].appendChild(row);
  }

  function syncKind() {
    var obj = $('f-kind').value === 'OBJECT';
    $('object-part').hidden = !obj;
    $('map-part').hidden = obj;
    var login = $('f-lifetime').value === 'LOGIN';
    $('f-onsignin').disabled = login;
    $('f-onsignout').disabled = login;
    $('lifetime-note').textContent = login
      ? 'Login state is empty while signed out, starts fresh at every sign-in and is gone at sign-out.'
      : 'Visitor state starts with the session. By default it survives sign-in and is cleared at sign-out.';
  }
  $('f-kind').addEventListener('change', syncKind);
  $('f-lifetime').addEventListener('change', syncKind);
  $('add-field').addEventListener('click', function () { addFieldRow('', null); });

  function openEditor(d) {
    current = d ? d.name : null;
    $('editor').hidden = false;
    $('editor-title').textContent = d ? 'Edit ' + d.name : 'New shared state';
    $('f-name').value = d ? d.name : '';
    $('f-name').readOnly = !!d;
    $('f-description').value = d ? d.description : '';
    $('f-kind').value = d ? d.kind : 'OBJECT';
    $('f-lifetime').value = d ? d.lifetime : 'VISITOR';
    $('fields').tBodies[0].textContent = '';
    if (d) Object.keys(d.fields || {}).forEach(function (k) { addFieldRow(k, d.fields[k]); });
    else addFieldRow('', null);
    $('f-valuetype').value = d ? d.valueType : 'any';
    $('f-maxentries').value = d ? d.maxEntries : 100;
    $('f-default').value = d && d.defaultValue != null ? JSON.stringify(d.defaultValue, null, 2) : '';
    $('f-writers').value = d ? d.writers.join(', ') : '';
    $('f-clientwritable').checked = d ? d.clientWritable : true;
    $('f-onsignin').value = d ? d.onSignIn : 'KEEP';
    $('f-onsignout').value = d ? d.onSignOut : 'CLEAR';
    $('delete').hidden = !d;
    syncKind();
    loadStates();
  }

  function readEditor() {
    var def = {
      name: $('f-name').value.trim(),
      description: $('f-description').value,
      kind: $('f-kind').value,
      lifetime: $('f-lifetime').value,
      writers: $('f-writers').value.split(',').map(function (s) { return s.trim(); }).filter(Boolean),
      clientWritable: $('f-clientwritable').checked,
      onSignIn: $('f-onsignin').value,
      onSignOut: $('f-onsignout').value
    };
    if (def.kind === 'OBJECT') {
      def.fields = {};
      Array.prototype.forEach.call($('fields').tBodies[0].rows, function (r) {
        var n = r.cells[0].firstChild.value.trim();
        if (n) def.fields[n] = { type: r.cells[1].firstChild.value, required: r.cells[2].firstChild.checked };
      });
    } else {
      def.valueType = $('f-valuetype').value;
      def.maxEntries = parseInt($('f-maxentries').value, 10) || 100;
    }
    var text = $('f-default').value.trim();
    if (text) def.defaultValue = JSON.parse(text);
    return def;
  }

  $('editor').addEventListener('submit', function (e) {
    e.preventDefault();
    var def;
    try { def = readEditor(); } catch (err) { say('The starting value is not valid JSON.', 'error'); return; }
    call('PUT', '/shared-state/' + encodeURIComponent(def.name), def).then(function (saved) {
      say('Saved ' + saved.name + '. New sessions pick it up at once.', 'ok');
      current = saved.name;
      openEditor(saved);
    }).catch(function (err) { say(err.message, 'error'); });
  });

  $('new-state').addEventListener('click', function () { say(''); openEditor(null); });
  $('cancel').addEventListener('click', function () { $('editor').hidden = true; current = null; loadStates(); });
  $('delete').addEventListener('click', function () {
    if (!current || !window.confirm('Delete shared state "' + current + '"? Plugins writing to it will be refused.')) return;
    call('DELETE', '/shared-state/' + encodeURIComponent(current)).then(function () {
      say('Deleted ' + current + '.', 'ok');
      $('editor').hidden = true;
      current = null;
      loadStates();
    }).catch(function (err) { say(err.message, 'error'); });
  });

  // ---- live sessions ----------------------------------------------------------------------------

  function loadSessions() {
    call('GET', '/sessions').then(function (data) {
      var body = $('sessions').tBodies[0];
      body.textContent = '';
      data.sessions.forEach(function (s) {
        var tr = el('tr', {}, [
          el('td', { textContent: s.handle }), el('td', { textContent: s.user || 'anonymous' }),
          el('td', { textContent: String(s.entries) }), el('td', { textContent: String(s.rev) }),
          el('td', { textContent: s.idleSeconds + ' s' })
        ]);
        tr.addEventListener('click', function () { showSession(s.handle); });
        body.appendChild(tr);
      });
      say(data.total + ' live session(s)' + (data.total > data.sessions.length ? ', showing the most recent ' + data.sessions.length : ''));
    }).catch(function (e) { say(e.message, 'error'); });
  }

  function showSession(handle) {
    call('GET', '/sessions/' + encodeURIComponent(handle) + '/state').then(function (data) {
      $('state-title').hidden = false;
      $('session-state').hidden = false;
      $('state-handle').textContent = handle;
      $('session-state').textContent = JSON.stringify(data.state, null, 2);
    }).catch(function (e) { say(e.message, 'error'); });
  }

  $('refresh').addEventListener('click', loadSessions);
  loadStates();
})();
