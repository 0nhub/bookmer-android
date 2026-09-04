package com.bookmer.browser.browser

import org.json.JSONArray

/**
 * Safari-like Hide Distracting Items — same bootstrap as iOS [BookmerHideElementsScript],
 * with Android bridge `BookmerHideBridge.postMessage(JSON)`.
 */
object HideElementsScript {
    const val BRIDGE_NAME = "BookmerHideBridge"

    fun applyJavaScript(selectors: List<String>): String {
        val encoded = JSONArray(selectors).toString()
        return "window.__bookmerHide && window.__bookmerHide.applyRules($encoded);"
    }

    const val ENSURE_BOOTSTRAP = """
(function() {
  if (window.__bookmerHide && typeof window.__bookmerHide.pickAt === 'function') return;
  try { delete window.__bookmerHide; } catch (e) { window.__bookmerHide = null; }

  var STYLE_ID = 'bookmer-hidden-elements-style';
  var HIGHLIGHT_STYLE_ID = 'bookmer-hide-highlight-style';
  var OVERLAY_ID = 'bookmer-hide-pick-overlay';
  var PREVIEW_ATTR = 'data-bookmer-hide-preview';
  var HIGHLIGHT_ATTR = 'data-bookmer-hide-highlight';
  var picking = false;
  var selected = null;
  var childStack = [];
  var overlay = null;
  var tapStartX = 0;
  var tapStartY = 0;
  var tapMoved = false;

  function ensureStyle() {
    var el = document.getElementById(STYLE_ID);
    if (!el) {
      el = document.createElement('style');
      el.id = STYLE_ID;
      (document.documentElement || document.head).appendChild(el);
    }
    return el;
  }

  function ensureHighlightStyle() {
    var s = document.getElementById(HIGHLIGHT_STYLE_ID);
    if (!s) {
      s = document.createElement('style');
      s.id = HIGHLIGHT_STYLE_ID;
      (document.documentElement || document.head).appendChild(s);
    }
    s.textContent =
      '[' + HIGHLIGHT_ATTR + ']{outline:3px solid #0A84FF !important;outline-offset:2px !important;cursor:pointer !important;}' +
      '[' + PREVIEW_ATTR + ']{display:none!important;}';
  }

  function cssEscape(value) {
    if (window.CSS && CSS.escape) return CSS.escape(value);
    return String(value).replace(/[^a-zA-Z0-9_-]/g, '\\$&');
  }

  function classNameString(el) {
    if (!el) return '';
    var c = el.className;
    if (!c) return '';
    if (typeof c === 'string') return c;
    if (typeof c.baseVal === 'string') return c.baseVal;
    return '';
  }

  function isOurUI(el) {
    if (!el || el.nodeType !== 1) return false;
    var id = el.id || '';
    return id === OVERLAY_ID || id === STYLE_ID || id === HIGHLIGHT_STYLE_ID;
  }

  function isSkippable(el) {
    if (!el || el.nodeType !== 1) return true;
    if (isOurUI(el)) return true;
    var tag = el.tagName;
    return tag === 'HTML' || tag === 'BODY' || tag === 'SCRIPT' || tag === 'STYLE'
      || tag === 'LINK' || tag === 'META' || tag === 'HEAD' || tag === 'NOSCRIPT'
      || tag === 'BR' || tag === 'WBR';
  }

  function buildSelector(el) {
    if (!el || isSkippable(el)) return '';

    var root = el.getRootNode && el.getRootNode();
    if (root && root !== document && root.host) {
      return buildSelector(root.host);
    }

    if (el.id && /^[A-Za-z][\w:-]*$/.test(el.id)) {
      var byId = '#' + cssEscape(el.id);
      try {
        if (document.querySelectorAll(byId).length === 1) return byId;
      } catch (e) {}
    }

    var cls = classNameString(el).trim().split(/\s+/).filter(Boolean).slice(0, 3);
    if (cls.length) {
      var byClass = el.tagName.toLowerCase() + '.' + cls.map(cssEscape).join('.');
      try {
        if (document.querySelectorAll(byClass).length === 1) return byClass;
      } catch (e) {}
    }

    if (el.tagName === 'IFRAME') {
      var src = el.getAttribute('src') || '';
      if (src) {
        try {
          var host = '';
          try { host = new URL(src, location.href).hostname; } catch (e2) {}
          if (host) {
            var bySrc = 'iframe[src*="' + cssEscape(host) + '"]';
            if (document.querySelectorAll(bySrc).length === 1) return bySrc;
          }
        } catch (e) {}
      }
      if (el.title) {
        var byTitle = 'iframe[title="' + cssEscape(el.title) + '"]';
        try {
          if (document.querySelectorAll(byTitle).length === 1) return byTitle;
        } catch (e) {}
      }
    }

    var parts = [];
    var node = el;
    var depth = 0;
    while (node && node.nodeType === 1 && node !== document.documentElement && depth < 12) {
      if (node.id && /^[A-Za-z][\w:-]*$/.test(node.id)) {
        parts.unshift('#' + cssEscape(node.id));
        break;
      }
      var tag = node.tagName.toLowerCase();
      var parent = node.parentElement;
      if (!parent) {
        parts.unshift(tag);
        break;
      }
      var siblings = Array.prototype.filter.call(parent.children, function(c) {
        return c.tagName === node.tagName;
      });
      if (siblings.length === 1) {
        parts.unshift(tag);
      } else {
        var index = siblings.indexOf(node) + 1;
        parts.unshift(tag + ':nth-of-type(' + index + ')');
      }
      node = parent;
      depth++;
    }
    return parts.join(' > ');
  }

  function labelFor(el) {
    if (!el) return '';
    var tag = (el.tagName || '').toLowerCase();
    if (tag === 'iframe') {
      var title = (el.title || el.getAttribute('name') || '').trim();
      if (title) return '<iframe> ' + title.slice(0, 48);
      return '<iframe>';
    }
    var text = (el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim();
    if (text.length > 48) text = text.slice(0, 45) + '\u2026';
    if (text) return '<' + tag + '> ' + text;
    if (el.id) return '<' + tag + ' id="' + el.id + '">';
    var cls = classNameString(el).trim().split(/\s+/).slice(0, 2).join('.');
    if (cls) return '<' + tag + '.' + cls + '>';
    return '<' + tag + '>';
  }

  function clearHighlight() {
    if (selected && selected.removeAttribute) {
      try { selected.removeAttribute(HIGHLIGHT_ATTR); } catch (e) {}
    }
    try {
      document.querySelectorAll('[' + HIGHLIGHT_ATTR + ']').forEach(function(n) {
        n.removeAttribute(HIGHLIGHT_ATTR);
      });
    } catch (e) {}
  }

  function post(payload) {
    try { window.__bookmerHideLastMessage = payload; } catch (e0) {}
    try {
      var json = JSON.stringify(payload);
      var bridge = window.BookmerHideBridge || window.bookmerHideBridge;
      if (bridge && typeof bridge.postMessage === 'function') {
        bridge.postMessage(json);
      }
    } catch (e) {}
  }

  function elementAtPoint(x, y) {
    var stack = [];
    try {
      if (document.elementsFromPoint) {
        stack = document.elementsFromPoint(x, y) || [];
      } else {
        var one = document.elementFromPoint(x, y);
        if (one) stack = [one];
      }
    } catch (e) {
      stack = [];
    }

    for (var i = 0; i < stack.length; i++) {
      var el = stack[i];
      if (!el || el.nodeType !== 1) continue;
      if (isOurUI(el)) continue;
      return normalizePickTarget(el);
    }
    return null;
  }

  function normalizePickTarget(el) {
    if (!el) return null;
    var root = el.getRootNode && el.getRootNode();
    if (root && root !== document && root.host) {
      var cur = el;
      while (cur && cur.nodeType === 1) {
        var r = cur.getRootNode && cur.getRootNode();
        if (!r || r === document) break;
        if (!isSkippable(cur)) {
          return r.host || cur;
        }
        cur = cur.parentElement || (r.host || null);
      }
      return root.host;
    }
    while (el && isSkippable(el)) el = el.parentElement;
    return el;
  }

  function selectAtPoint(x, y) {
    childStack = [];
    setSelected(elementAtPoint(x, y));
  }

  function ensureOverlay() {
    if (overlay && overlay.isConnected) return overlay;
    overlay = document.getElementById(OVERLAY_ID);
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = OVERLAY_ID;
      overlay.setAttribute('role', 'presentation');
    }
    // Visual only — Android WebView often drops overlay touch events, so native
    // OnTouchListener + pickAt() owns hit-testing. Keep a light tint so pick mode is obvious.
    overlay.style.cssText = [
      'position: fixed',
      'left: 0',
      'top: 0',
      'right: 0',
      'bottom: 0',
      'width: 100%',
      'height: 100%',
      'z-index: 2147483646',
      'cursor: crosshair',
      'touch-action: none',
      'background: rgba(10,132,255,0.07)',
      'pointer-events: none',
      '-webkit-tap-highlight-color: transparent',
      'user-select: none'
    ].join(';');
    (document.documentElement || document.body).appendChild(overlay);
    return overlay;
  }

  function removeOverlay() {
    if (overlay && overlay.parentNode) overlay.parentNode.removeChild(overlay);
    overlay = null;
  }

  function onDocPointerDown(ev) {
    if (!picking || !ev) return;
    if (ev.pointerType === 'mouse' && ev.button !== 0) return;
    tapStartX = ev.clientX;
    tapStartY = ev.clientY;
    tapMoved = false;
  }

  function onDocPointerMove(ev) {
    if (!picking || tapMoved || !ev) return;
    if (Math.abs(ev.clientX - tapStartX) > 14 || Math.abs(ev.clientY - tapStartY) > 14) {
      tapMoved = true;
    }
  }

  function onDocPointerUp(ev) {
    if (!picking || !ev) return;
    if (tapMoved) return;
    try {
      ev.preventDefault();
      ev.stopPropagation();
      ev.stopImmediatePropagation();
    } catch (e) {}
    selectAtPoint(ev.clientX, ev.clientY);
  }

  function onDocClick(ev) {
    if (!picking || !ev) return;
    try {
      ev.preventDefault();
      ev.stopPropagation();
      ev.stopImmediatePropagation();
    } catch (e) {}
    selectAtPoint(ev.clientX, ev.clientY);
  }

  function applyRules(selectors) {
    ensureStyle();
    var list = Array.isArray(selectors) ? selectors : [];
    var css = list.map(function(sel) {
      try {
        document.querySelector(sel);
        return sel + '{display:none!important;visibility:hidden!important;}';
      } catch (e) {
        return '';
      }
    }).filter(Boolean).join('\n');
    ensureStyle().textContent = css;
  }

  function revealAll() {
    ensureStyle().textContent = '';
    clearPreview();
  }

  function clearPreview() {
    if (selected && selected.removeAttribute) {
      try { selected.removeAttribute(PREVIEW_ATTR); } catch (e) {}
    }
    try {
      document.querySelectorAll('[' + PREVIEW_ATTR + ']').forEach(function(n) {
        n.removeAttribute(PREVIEW_ATTR);
      });
    } catch (e) {}
    if (selected && picking) {
      ensureHighlightStyle();
      try { selected.setAttribute(HIGHLIGHT_ATTR, '1'); } catch (e) {}
    }
  }

  function previewHide() {
    if (!selected) return;
    ensureHighlightStyle();
    clearHighlight();
    try { selected.setAttribute(PREVIEW_ATTR, '1'); } catch (e) {}
  }

  function setSelected(el) {
    clearPreviewAttrsOnly();
    clearHighlight();
    selected = el && !isSkippable(el) ? el : null;
    if (selected) {
      ensureHighlightStyle();
      try { selected.setAttribute(HIGHLIGHT_ATTR, '1'); } catch (e) {}
      try {
        var r = selected.getBoundingClientRect();
        if (r.width > 0 && r.height > 0 && (r.bottom < 0 || r.top > (window.innerHeight || 0))) {
          selected.scrollIntoView({ block: 'nearest', inline: 'nearest' });
        }
      } catch (e) {}
    }
    var selector = buildSelector(selected);
    post({
      type: 'selection',
      hasSelection: !!selected && !!selector,
      label: labelFor(selected),
      selector: selector
    });
  }

  function clearPreviewAttrsOnly() {
    if (selected && selected.removeAttribute) {
      try { selected.removeAttribute(PREVIEW_ATTR); } catch (e) {}
    }
    try {
      document.querySelectorAll('[' + PREVIEW_ATTR + ']').forEach(function(n) {
        n.removeAttribute(PREVIEW_ATTR);
      });
    } catch (e) {}
  }

  function bindDocPick() {
    ensureOverlay();
    document.addEventListener('pointerdown', onDocPointerDown, true);
    document.addEventListener('pointermove', onDocPointerMove, true);
    document.addEventListener('pointerup', onDocPointerUp, true);
    document.addEventListener('click', onDocClick, true);
  }

  function unbindDocPick() {
    document.removeEventListener('pointerdown', onDocPointerDown, true);
    document.removeEventListener('pointermove', onDocPointerMove, true);
    document.removeEventListener('pointerup', onDocPointerUp, true);
    document.removeEventListener('click', onDocClick, true);
  }

  function startPick() {
    // Always re-arm — SPA sites can drop the overlay without a full reload.
    if (picking) {
      unbindDocPick();
      picking = false;
    }
    picking = true;
    ensureHighlightStyle();
    childStack = [];
    tapMoved = false;
    bindDocPick();
    post({ type: 'pickStarted' });
  }

  function ensurePicking() {
    if (!picking) {
      startPick();
      return true;
    }
    if (!overlay || !overlay.isConnected) ensureOverlay();
    return true;
  }

  function stopPick(cancel) {
    if (!picking && !selected) return;
    picking = false;
    unbindDocPick();
    removeOverlay();
    clearHighlight();
    if (cancel) clearPreview();
    else clearPreviewAttrsOnly();
    selected = null;
    childStack = [];
    post({ type: 'pickStopped', cancelled: !!cancel });
  }

  function pickAt(x, y) {
    if (!picking) startPick();
    ensureOverlay();
    selectAtPoint(Number(x) || 0, Number(y) || 0);
    return selectionInfo();
  }

  function expand() {
    if (!selected) return;
    var parent = selected.parentElement;
    if (!parent || isSkippable(parent)) {
      var root = selected.getRootNode && selected.getRootNode();
      if (root && root.host && root.host !== selected) {
        childStack.push(selected);
        setSelected(root.host);
      }
      return;
    }
    childStack.push(selected);
    setSelected(parent);
  }

  function shrink() {
    if (childStack.length) {
      setSelected(childStack.pop());
      return;
    }
    if (!selected) return;
    var kids = Array.prototype.filter.call(selected.children || [], function(c) {
      return !isSkippable(c);
    });
    if (!kids.length) return;
    var best = kids[0];
    var bestArea = 0;
    for (var i = 0; i < kids.length; i++) {
      var r = kids[i].getBoundingClientRect();
      var area = Math.max(0, r.width) * Math.max(0, r.height);
      if (area > bestArea) {
        bestArea = area;
        best = kids[i];
      }
    }
    setSelected(best);
  }

  function selectionInfo() {
    if (!selected) return null;
    return { label: labelFor(selected), selector: buildSelector(selected) };
  }

  function commitHide() {
    if (!selected) return null;
    var info = selectionInfo();
    previewHide();
    return info;
  }

  window.__bookmerHide = {
    applyRules: applyRules,
    revealAll: revealAll,
    startPick: startPick,
    stopPick: stopPick,
    ensurePicking: ensurePicking,
    pickAt: pickAt,
    expand: expand,
    shrink: shrink,
    previewHide: previewHide,
    clearPreview: clearPreview,
    selectionInfo: selectionInfo,
    commitHide: commitHide,
    isPicking: function() { return !!picking; },
    takeLastMessage: function() {
      var m = window.__bookmerHideLastMessage || null;
      window.__bookmerHideLastMessage = null;
      return m;
    }
  };
})();
"""

    const val START_PICK = "window.__bookmerHide && window.__bookmerHide.startPick();"
    const val STOP_PICK = "window.__bookmerHide && window.__bookmerHide.stopPick(false);"
    const val CANCEL_PICK = "window.__bookmerHide && window.__bookmerHide.stopPick(true);"
    const val EXPAND = "window.__bookmerHide && window.__bookmerHide.expand();"
    const val SHRINK = "window.__bookmerHide && window.__bookmerHide.shrink();"
    const val PREVIEW_HIDE = "window.__bookmerHide && window.__bookmerHide.previewHide();"
    const val CLEAR_PREVIEW = "window.__bookmerHide && window.__bookmerHide.clearPreview();"
    const val REVEAL_ALL = "window.__bookmerHide && window.__bookmerHide.revealAll();"
    const val ENSURE_PICKING = "window.__bookmerHide && window.__bookmerHide.ensurePicking();"
    const val POLL_MESSAGE =
        "(function(){try{var h=window.__bookmerHide;if(!h)return null;var m=h.takeLastMessage&&h.takeLastMessage();return m?JSON.stringify(m):null;}catch(e){return null;}})();"
    const val ENSURE_READY =
        "(function(){try{return !!(window.__bookmerHide&&window.__bookmerHide.startPick&&window.__bookmerHide.pickAt);}catch(e){return false;}})();"

    fun pickAtJavaScript(x: Float, y: Float): String =
        "window.__bookmerHide && window.__bookmerHide.pickAt(${x.toDouble()}, ${y.toDouble()});"
}