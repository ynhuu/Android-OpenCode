import { describe, expect, test } from "bun:test"
import { readFileSync } from "node:fs"
import { runInNewContext } from "node:vm"

const source = readFileSync(new URL("../app/src/main/res/raw/notification_bridge.js", import.meta.url), "utf8")

function page(nested = false) {
  const messages: any[] = []
  const state = { permission: "granted", result: "posted" }
  const bridge: any = {
    postMessage(raw: string) {
      const message = JSON.parse(raw)
      messages.push(message)
      if (["status", "show", "permission"].includes(message.type)) {
        bridge.onmessage({ data: JSON.stringify({ id: message.id, ...state }) })
      }
    },
  }
  const window: any = { OpenCodeNotifications: bridge }
  window.top = nested ? {} : window
  runInNewContext(source, { window, OpenCodeNotifications: bridge, Event, EventTarget,
    setTimeout, clearTimeout, location: { href: "http://localhost:8080/session/test" } })
  return { window, messages, state, bridge }
}

describe("Android notification bridge", () => {
  test("does not expose APIs in subframes", () => {
    const { window, messages } = page(true)
    expect(window.OpenCodeAndroidNotifications).toBeUndefined()
    expect(window.Notification).toBeUndefined()
    expect(messages).toHaveLength(0)
  })
  test("native permission state updates the browser API", () => {
    const { window } = page()
    expect(window.Notification.permission).toBe("granted")
    window.__openCodeNotificationState({ permission: "denied" })
    expect(window.Notification.permission).toBe("denied")
  })
  test("standard notification retains the correct click callback", () => {
    const { window, messages } = page()
    let clicked = 0
    const route = "http://localhost:8080/session/target"
    const notification = new window.Notification("title", { body: "body", data: { url: route } })
    notification.onclick = () => clicked++
    const message = messages.find((m) => m.type === "show")
    expect(message.url).toBe(route)
    expect(window.__openCodeNotificationClick(message.id)).toBe(true)
    expect(clicked).toBe(1)
    expect(window.__openCodeNotificationClick(message.id)).toBe(false)
  })
  test("experimental test API is not exposed", () => {
    expect(page().window.OpenCodeAndroidNotifications).toBeUndefined()
  })
  test("old browser-style notifications still click and close", () => {
    const { window, messages } = page()
    const notification = new window.Notification("legacy", { body: "body" })
    let clicked = false
    notification.onclick = () => { clicked = true }
    expect(window.__openCodeNotificationClick(notification.id)).toBe(true)
    expect(clicked).toBe(true)
    expect(messages.at(-1)).toMatchObject({ type: "close", id: notification.id })
  })
  test("IDs from reloaded pages cannot click a new page's notification", () => {
    const old = page()
    const next = page()
    const notification = new old.window.Notification("old")
    new next.window.Notification("new")
    expect(next.window.__openCodeNotificationClick(notification.id)).toBe(false)
  })
  test("concurrent permission requests share one native request", async () => {
    const { window, messages } = page()
    let callbackValue = ""
    const values = await Promise.all([
      window.Notification.requestPermission((value: string) => { callbackValue = value }),
      window.Notification.requestPermission(),
    ])
    expect(values).toEqual(["granted", "granted"])
    expect(callbackValue).toBe("granted")
    expect(messages.filter((message) => message.type === "permission")).toHaveLength(1)
    await window.Notification.requestPermission()
    expect(messages.filter((message) => message.type === "permission")).toHaveLength(2)
  })
  test("malformed replies do not corrupt permission state", () => {
    const { window, bridge } = page()
    for (const data of ["invalid", "null", '{"permission":"invalid"}']) {
      expect(() => bridge.onmessage({ data })).not.toThrow()
    }
    expect(window.Notification.permission).toBe("granted")
  })
  test("blocked notifications release their click callback", () => {
    const { window, state } = page()
    state.result = "blocked-foreground"
    const notification = new window.Notification("blocked")
    expect(window.__openCodeNotificationClick(notification.id)).toBe(false)
  })
  test("throwing click callbacks still close the notification", () => {
    const { window, messages } = page()
    const notification = new window.Notification("title")
    notification.onclick = () => { throw new Error("callback failed") }
    expect(() => window.__openCodeNotificationClick(notification.id)).toThrow("callback failed")
    expect(window.__openCodeNotificationClick(notification.id)).toBe(false)
    expect(messages.at(-1)).toMatchObject({ type: "close", id: notification.id })
  })
})
