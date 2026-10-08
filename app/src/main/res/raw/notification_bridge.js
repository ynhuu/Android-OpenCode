(() => {
  if (window !== window.top || !window.OpenCodeNotifications) return;
  let permission = "default";
  let sequence = 0;
  const page = Date.now().toString(36) + Math.random().toString(36).slice(2);
  const pending = new Map();
  let permissionRequest;
  const notifications = new Map();
  const next = () => page + ":" + (++sequence);
  const send = (type, data = {}) => OpenCodeNotifications.postMessage(JSON.stringify({ type, ...data }));
  const apply = (state) => {
    if (["default", "denied", "granted"].includes(state.permission)) permission = state.permission;
  };
  const requestNativePermission = () => new Promise((resolve, reject) => {
    const id = next();
    const timer = setTimeout(() => { pending.delete(id); reject(new Error("Notification bridge timed out")); }, 60000);
    pending.set(id, () => { clearTimeout(timer); resolve(permission); });
    try { send("permission", { id }); }
    catch (error) { clearTimeout(timer); pending.delete(id); reject(error); }
  });
  OpenCodeNotifications.onmessage = ({ data }) => {
    let response;
    try { response = JSON.parse(data); } catch { return; }
    if (!response || typeof response !== "object") return;
    apply(response);
    if (response.result && response.result !== "ok" && response.result !== "posted") notifications.delete(response.id);
    pending.get(response.id)?.();
    pending.delete(response.id);
  };
  // Standard browser API compatibility; no frontend changes required.
  class AndroidNotification extends EventTarget {
    static get permission() { return permission; }
    static requestPermission(callback) {
      permissionRequest ??= requestNativePermission()
        .finally(() => { permissionRequest = undefined; });
      return permissionRequest.then((value) => { callback?.(value); return value; });
    }
    constructor(title, options = {}) {
      super();
      this.id = next();
      this.title = String(title);
      this.body = String(options.body ?? "");
      this.onclick = null;
      notifications.set(this.id, this);
      if (notifications.size > 100) notifications.delete(notifications.keys().next().value);
      try { send("show", { id: this.id, title: this.title, body: this.body, url: options.data?.url ?? location.href }); }
      catch (error) { notifications.delete(this.id); throw error; }
    }
    close() { notifications.delete(this.id); send("close", { id: this.id }); }
  }
  window.Notification = AndroidNotification;
  window.__openCodeNotificationState = apply;
  window.__openCodeNotificationClick = (id) => {
    const notification = notifications.get(id);
    if (!notification) return false;
    const event = new Event("click");
    try {
      notification.onclick?.(event);
      notification.dispatchEvent(event);
    } finally { notification.close(); }
    return true;
  };
  send("status");
})();
