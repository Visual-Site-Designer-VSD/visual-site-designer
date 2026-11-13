#!/usr/bin/env python3
"""End-to-end smoke test for a running VSD live runtime (demo profile).

    python scripts/smoke_test.py [http://localhost:8090]

Talks to the real HTTP API with a minimal cookie-aware client, so it checks what a browser
would see: sessions, CSRF, events, idempotency, sign-in rotation, SSE fan-out and isolation.
"""
import http.client
import json
import re
import sys
import threading
import time
import uuid
from urllib.parse import urlparse

BASE = urlparse(sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8090")
COOKIE = "VSDSESSID"
failures = []
RUN_ID = uuid.uuid4().hex[:8]  # keeps the rate-limit probe independent between runs


def check(name, cond, detail=""):
    print(("PASS  " if cond else "FAIL  ") + name + ("" if cond else f"  -> {detail}"))
    if not cond:
        failures.append(name)


class Client:
    """One visitor: a cookie jar of one cookie plus the CSRF token from the page shell."""

    def __init__(self):
        self.cookie = None
        self.csrf = None
        self.rev = 0

    def req(self, method, path, body=None, headers=None, raw=False):
        conn = http.client.HTTPConnection(BASE.hostname, BASE.port, timeout=10)
        h = {"Host": f"{BASE.hostname}:{BASE.port}"}
        if self.cookie:
            h["Cookie"] = f"{COOKIE}={self.cookie}"
        if self.csrf and method == "POST":
            h["X-VSD-CSRF"] = self.csrf
        if body is not None:
            h["Content-Type"] = "application/json"
        h.update(headers or {})
        conn.request(method, path, json.dumps(body) if body is not None else None, h)
        resp = conn.getresponse()
        data = resp.read().decode("utf-8")
        for k, v in resp.getheaders():
            if k.lower() == "set-cookie" and v.startswith(COOKIE + "="):
                self.cookie = v.split(";", 1)[0].split("=", 1)[1]
        conn.close()
        if raw:
            return resp, data
        try:
            return resp.status, json.loads(data) if data else {}
        except ValueError:
            return resp.status, {"_raw": data}

    def load_page(self, path="/"):
        resp, html = self.req("GET", path, raw=True)
        m = re.search(r'<script id="vsd-boot" type="application/json">(.*?)</script>', html, re.S)
        boot = json.loads(m.group(1)) if m else None
        if boot:
            self.csrf = boot["session"]["csrf"]
            self.rev = boot["session"]["rev"]
        return resp, html, boot

    def event(self, type_, payload, event_id=None, plugin="newsletter-form-plugin", page="home"):
        return self.req("POST", "/_vsd/api/events", {
            "eventId": event_id or str(uuid.uuid4()), "type": type_, "pluginId": plugin,
            "componentId": "NewsletterForm", "instanceId": "news-1", "page": page, "payload": payload})


def entries_by_key(boot_or_session):
    return {(e["scope"], e["owner"], e["key"]): e["value"] for e in boot_or_session["entries"]}


# ---------------------------------------------------------------------------------------------
a = Client()
resp, html, boot = a.load_page("/")
check("home page renders a shell with boot data", resp.status == 200 and boot is not None)
check("session cookie is HttpOnly + SameSite=Lax",
      any(k.lower() == "set-cookie" and "HttpOnly" in v and "SameSite=Lax" in v for k, v in resp.getheaders()))
check("HTML is not cacheable (it embeds session state)", "no-store" in (resp.getheader("Cache-Control") or ""))
check("page JSON is script-safe (no raw '</')", "</script" not in html.split('id="vsd-boot"')[1].split("</script>")[0].lower())
check("boot pins the plugins the demo pages use", boot and {"auth-component-plugin", "navbar-component-plugin", "newsletter-form-plugin"} <= {p["id"] for p in boot["plugins"]})
check("anonymous visitor has no user", ("SESSION", "$auth", "user") not in entries_by_key(boot["session"]))

resp, _, about = a.load_page("/about")
check("second page served from the same snapshot", resp.status == 200 and about["page"]["name"] == "about")
resp, _ = Client().req("GET", "/does-not-exist", raw=True)
check("unknown path -> 404", resp.status == 404)

# --- static / plugin assets ---------------------------------------------------------------
resp, body = a.req("GET", "/_vsd/client/runtime.js", raw=True)
check("client bundle served", resp.status == 200 and len(body) > 10000)
resp, body = a.req("GET", "/_vsd/plugins/newsletter-form-plugin/bundle.js?v=1.0.0", raw=True)
check("pinned plugin bundle served", resp.status == 200 and "NewsletterFormPlugin" in body)
resp, _ = a.req("GET", "/_vsd/plugins/nope-plugin/bundle.js", raw=True)
check("unpinned plugin bundle -> 404", resp.status == 404)
resp, _ = a.req("GET", "/_vsd/plugins/newsletter-form-plugin/..%2f..%2fapplication.properties", raw=True)
check("plugin path traversal rejected", resp.status in (400, 404))
resp, _ = a.req("GET", "/assets/../site.json", raw=True)
check("asset path traversal rejected", resp.status in (400, 404))
resp, _ = a.req("GET", "/_vsd/client/secret.txt", raw=True)
check("client dir is whitelisted", resp.status == 404)
resp, _ = a.req("GET", "/_vsd/plugins/auth-component-plugin/bundle.css?v=1.0.0", raw=True)
check("plugin stylesheet with non-standard name served as bundle.css", resp.status == 200)

# --- CSRF / origin ---------------------------------------------------------------------------
saved = a.csrf
a.csrf = None
status, body = a.event("subscribe", {"email": "x@y.io"})
check("POST without CSRF header -> 403", status == 403 and body.get("message") == "csrf", (status, body))
a.csrf = "wrong-token"
status, _ = a.event("subscribe", {"email": "x@y.io"})
check("POST with wrong CSRF -> 403", status == 403)
a.csrf = saved
status, _ = a.req("POST", "/_vsd/api/events", {"eventId": "e-origin", "type": "subscribe", "pluginId": "newsletter-form-plugin",
                                                "payload": {"email": "x@y.io"}}, headers={"Origin": "http://evil.example"})
check("POST from a foreign Origin -> 403", status == 403)
status, _ = Client().req("POST", "/_vsd/api/events", {"eventId": "e1"})
check("POST without a session -> 401", status == 401)

# --- events ------------------------------------------------------------------------------------
status, body = a.event("subscribe", {"email": "not-an-email"})
check("invalid email -> failure with field error", status == 200 and body["status"] == "failure" and body["errors"].get("email"), body)
status, s = a.req("GET", "/_vsd/api/session")
check("failed handler changed no state", ("PLUGIN", "newsletter-form-plugin", "subscription") not in entries_by_key(s))

eid = str(uuid.uuid4())
status, body = a.event("subscribe", {"email": "me@example.com"}, event_id=eid)
patch = body["patches"][0] if body.get("patches") else {}
check("valid subscribe succeeds and returns a versioned patch",
      status == 200 and body["status"] == "success" and patch.get("rev", 0) > 0 and patch["value"]["email"] == "me@example.com", body)
rev_after = body["rev"]
status, again = a.event("subscribe", {"email": "other@example.com"}, event_id=eid)
check("retried eventId returns the original answer without re-running", again == body, again)
status, s = a.req("GET", "/_vsd/api/session")
check("state holds the first subscription only", entries_by_key(s)[("PLUGIN", "newsletter-form-plugin", "subscription")]["email"] == "me@example.com")
check("revision did not advance on the retry", s["rev"] == rev_after)

status, body = a.event("subscribe", {"email": "a@b.co"}, plugin="auth-component-plugin")
check("event for a plugin with no handler -> 404", status == 404)
status, body = a.event("subscribe", {"email": "a@b.co"}, plugin="not-pinned-plugin")
check("event for an unpinned plugin -> 404", status == 404)
status, body = a.event("subscribe", {"email": "a@b.co"}, page="no-such-page")
check("event for an unknown page -> 400", status == 400)

# --- direct state writes -----------------------------------------------------------------------
w = lambda p: a.req("POST", "/_vsd/api/state", {"patches": p})
status, body = w([{"op": "set", "scope": "COMPONENT", "owner": "tabs-1", "key": "active", "value": 2}])
check("client can write ordinary state", status == 200 and body["patches"][0]["rev"] > 0, body)
status, _ = w([{"op": "set", "scope": "SESSION", "owner": "$auth", "key": "user", "value": {"roles": ["ADMIN"]}}])
check("client cannot write reserved ($auth) state", status == 400)
status, _ = w([{"op": "set", "scope": "SESSION", "owner": "a/b", "key": "k", "value": 1}])
check("owner with a separator rejected", status == 400)
status, _ = w([{"op": "set", "scope": "PAGE", "owner": "home", "key": "big", "value": "x" * 20000}])
check("oversized value rejected", status == 400)
status, _ = w([{"op": "set", "scope": "BOGUS", "owner": "o", "key": "k", "value": 1}])
check("unknown scope rejected", status == 400)

# --- isolation + SSE fan-out ----------------------------------------------------------------------
b = Client()
b.load_page("/")
check("another visitor has their own empty session",
      ("PLUGIN", "newsletter-form-plugin", "subscription") not in entries_by_key(b.req("GET", "/_vsd/api/session")[1]))

received = []
stream_ready = threading.Event()


def listen(client, out, since):
    conn = http.client.HTTPConnection(BASE.hostname, BASE.port, timeout=15)
    conn.request("GET", f"/_vsd/api/stream?since={since}", headers={"Cookie": f"{COOKIE}={client.cookie}"})
    r = conn.getresponse()
    out.append(("status", r.status))
    ev = None
    while True:
        line = r.readline().decode("utf-8")
        if not line:
            break
        line = line.rstrip("\n")
        if line.startswith("event:"):
            ev = line.split(":", 1)[1].strip()
            if ev == "ready":
                stream_ready.set()
        elif line.startswith("data:"):
            out.append((ev, json.loads(line.split(":", 1)[1])))
            if ev == "patch":
                break


# Tab 2 of visitor A listens; visitor B listens too and must never hear A.
tab2, b_events = [], []
fresh_rev = a.req("GET", "/_vsd/api/session")[1]["rev"]
t1 = threading.Thread(target=listen, args=(a, tab2, fresh_rev), daemon=True)
t1.start()
stream_ready.wait(5)
stream_ready.clear()
t2 = threading.Thread(target=listen, args=(b, b_events, 0), daemon=True)
t2.start()
stream_ready.wait(5)
time.sleep(0.2)
a.req("POST", "/_vsd/api/state", {"patches": [{"op": "set", "scope": "SESSION", "owner": "demo", "key": "ping", "value": "hello"}]})
t1.join(5)
got = [p for e, d in tab2 if e == "patch" for p in d["patches"]]
check("SSE: a second tab receives the patch", any(p["key"] == "ping" and p["value"] == "hello" for p in got), tab2)
check("SSE: a different visitor receives nothing", not any(e == "patch" for e, _ in b_events), b_events)

# Resume: a client that missed patches gets them replayed from its last revision.
resume = []
t3 = threading.Thread(target=listen, args=(a, resume, 0), daemon=True)
t3.start()
t3.join(5)
replayed = [p for e, d in resume if e in ("patch", "snapshot") for p in (d.get("patches") or d.get("entries") or [])]
check("SSE: reconnecting from an old revision replays missed state", any(p.get("key") == "ping" for p in replayed), resume)

# --- auth ---------------------------------------------------------------------------------------------
old_cookie, old_csrf = a.cookie, a.csrf
status, body = a.req("POST", "/_vsd/api/auth/login", {"username": "demo", "password": "wrong"})
check("wrong password -> 401, no detail leak", status == 401 and body["message"] == "Invalid credentials", body)
check("failed login keeps the session cookie", a.cookie == old_cookie)

status, body = a.req("POST", "/_vsd/api/auth/login", {"username": "demo@example.com", "password": "demo-pass"})
check("login by email works", status == 200 and body["status"] == "success", body)
check("login rotates the session id (fixation defence)", a.cookie != old_cookie)
check("login rotates the CSRF token", body.get("csrf") and body["csrf"] != old_csrf)
user_patch = [p for p in body["patches"] if p["owner"] == "$auth"]
check("identity is published as reserved state", user_patch and user_patch[0]["value"]["displayName"] == "Demo User", body)
a.csrf = body["csrf"]
fix = Client()
fix.cookie, fix.csrf = old_cookie, old_csrf
status, _ = fix.req("GET", "/_vsd/api/session")
check("the pre-login session id no longer works", status == 200 and fix.cookie != old_cookie)
status, s = a.req("GET", "/_vsd/api/session")
check("state survived the sign-in", ("PLUGIN", "newsletter-form-plugin", "subscription") in entries_by_key(s))
status, _ = a.event("subscribe", {"email": "again@example.com"}, event_id=str(uuid.uuid4()))
check("handler sees the signed-in user", status == 200)
check("subscription records who was signed in",
      entries_by_key(a.req("GET", "/_vsd/api/session")[1])[("PLUGIN", "newsletter-form-plugin", "subscription")]["signedInAs"] == "demo")

lim = Client()
lim.load_page("/")
codes = [lim.req("POST", "/_vsd/api/auth/login", {"username": "ratelimit-" + RUN_ID, "password": "bad%d" % i})[0] for i in range(7)]
check("login attempts are rate limited (429)", 429 in codes, codes)

status, body = a.req("POST", "/_vsd/api/auth/logout", {})
check("logout succeeds and clears user + state", status == 200 and any(p["op"] == "remove" and p["owner"] == "$auth" for p in body["patches"]), body)
a.csrf = body["csrf"]
status, s = a.req("GET", "/_vsd/api/session")
check("after logout no state remains", s["entries"] == [], s)

print()
print("ALL PASSED" if not failures else f"{len(failures)} FAILED: " + "; ".join(failures))
sys.exit(1 if failures else 0)
