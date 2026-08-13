(() => {
  "use strict";

  const API_BASE = "/api/v1";
  const CSRF_STORAGE_KEY = "trivocab-csrf-token";

  const elements = {
    themeToggle: document.querySelector("#themeToggle"),
    themeToggleLabel: document.querySelector("#themeToggleLabel"),
    themeColorMeta: document.querySelector("#themeColorMeta"),
    authCheckState: document.querySelector("#authCheckState"),
    authPanelContent: document.querySelector("#authPanelContent"),
    authGateError: document.querySelector("#authGateError"),
    authGateErrorMessage: document.querySelector("#authGateErrorMessage"),
    retryAuthButton: document.querySelector("#retryAuthButton"),
    authTabs: document.querySelector(".auth-tabs"),
    viewButtons: [...document.querySelectorAll("[data-auth-view]")],
    sections: [...document.querySelectorAll("[data-auth-section]")],
    loginForm: document.querySelector("#loginForm"),
    registerForm: document.querySelector("#registerForm"),
    forgotForm: document.querySelector("#forgotForm"),
    resetForm: document.querySelector("#resetForm"),
    loginStatus: document.querySelector("#loginStatus"),
    registerStatus: document.querySelector("#registerStatus"),
    forgotStatus: document.querySelector("#forgotStatus"),
    resetStatus: document.querySelector("#resetStatus"),
    forgotEmail: document.querySelector("#forgotEmail"),
    resetEmail: document.querySelector("#resetEmail"),
    resetCodeNote: document.querySelector("#resetCodeNote"),
    toastRegion: document.querySelector("#authToastRegion")
  };

  document.addEventListener("DOMContentLoaded", init);

  function init() {
    syncThemeControl();
    bindEvents();
    checkExistingSession();
  }

  function bindEvents() {
    elements.themeToggle.addEventListener("click", toggleTheme);
    elements.retryAuthButton.addEventListener("click", checkExistingSession);
    elements.viewButtons.forEach((button) => {
      button.addEventListener("click", () => switchView(button.dataset.authView));
    });
    elements.loginForm.addEventListener("submit", handleLogin);
    elements.registerForm.addEventListener("submit", handleRegister);
    elements.forgotForm.addEventListener("submit", handleForgotPassword);
    elements.resetForm.addEventListener("submit", handleResetPassword);
    document.querySelectorAll("input").forEach((input) => {
      input.addEventListener("input", () => input.removeAttribute("aria-invalid"));
    });
  }

  async function checkExistingSession() {
    setGateState("loading");
    try {
      const user = await request("/auth/me", { authCheck: true });
      if (user?.id) {
        rememberCsrf(user.csrfToken);
        redirectByRole(user.role);
        return;
      }
      showAuthPanel();
    } catch (error) {
      if (error.status === 401) {
        clearCsrf();
        showAuthPanel();
        return;
      }
      setGateState("error", readError(error, "请检查服务状态后重试。"));
    }
  }

  function showAuthPanel() {
    document.body.classList.remove("is-checking-auth");
    elements.authCheckState.hidden = true;
    elements.authGateError.hidden = true;
    elements.authPanelContent.hidden = false;
    const requestedMode = new URLSearchParams(window.location.search).get("mode");
    switchView(["register", "forgot", "reset"].includes(requestedMode) ? requestedMode : "login");
  }

  function setGateState(state, message = "") {
    const loading = state === "loading";
    elements.authCheckState.hidden = !loading;
    elements.authGateError.hidden = state !== "error";
    elements.authPanelContent.hidden = true;
    if (message) elements.authGateErrorMessage.textContent = message;
  }

  function switchView(view) {
    const target = ["login", "register", "forgot", "reset"].includes(view) ? view : "login";
    elements.sections.forEach((section) => {
      section.hidden = section.dataset.authSection !== target;
    });
    elements.viewButtons.forEach((button) => {
      if (button.getAttribute("role") !== "tab") return;
      button.setAttribute("aria-selected", String(button.dataset.authView === target));
    });
    elements.authTabs.hidden = target === "forgot" || target === "reset";
    clearFormStatuses();
  }

  async function handleLogin(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!validateForm(form, elements.loginStatus)) return;
    const values = formValues(form);
    setSubmitting(form, true, "正在登录…", elements.loginStatus);
    try {
      const user = await request("/auth/login", {
        method: "POST",
        body: {
          identifier: values.identifier.trim(),
          password: values.password,
          timeZone: detectTimeZone()
        },
        publicRequest: true
      });
      rememberCsrf(user?.csrfToken);
      setStatus(elements.loginStatus, "登录成功，正在进入系统。", "success");
      redirectByRole(user?.role);
    } catch (error) {
      setStatus(elements.loginStatus, readError(error, "登录失败，请检查用户名和密码。"), "error");
    } finally {
      setSubmitting(form, false);
    }
  }

  async function handleRegister(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!validateForm(form, elements.registerStatus)) return;
    const values = formValues(form);
    if (values.password !== values.passwordConfirm) {
      markInvalid(form.elements.passwordConfirm);
      setStatus(elements.registerStatus, "两次输入的密码不一致。", "error");
      return;
    }

    setSubmitting(form, true, "正在创建账号…", elements.registerStatus);
    try {
      const user = await request("/auth/register", {
        method: "POST",
        body: {
          username: values.username.trim(),
          displayName: values.displayName.trim(),
          email: values.email.trim(),
          password: values.password,
          timeZone: detectTimeZone()
        },
        publicRequest: true
      });

      form.reset();
      if (user?.csrfToken && user?.role) {
        rememberCsrf(user.csrfToken);
        redirectByRole(user.role);
        return;
      }
      switchView("login");
      setStatus(elements.loginStatus, "注册成功，请使用新账号登录。", "success");
    } catch (error) {
      setStatus(elements.registerStatus, readError(error, "注册失败，请检查填写内容。"), "error");
    } finally {
      setSubmitting(form, false);
    }
  }

  async function handleForgotPassword(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!validateForm(form, elements.forgotStatus)) return;
    const email = formValues(form).email.trim();
    setSubmitting(form, true, "正在生成验证码…", elements.forgotStatus);
    try {
      const result = await request("/auth/forgot-password", {
        method: "POST",
        body: { email },
        publicRequest: true
      });
      elements.resetEmail.value = email;
      const resetCode = cleanText(result?.resetCode);
      elements.resetCodeNote.hidden = !resetCode;
      if (resetCode) {
        elements.resetCodeNote.innerHTML = `开发环境验证码<strong>${escapeHtml(resetCode)}</strong>`;
      } else {
        elements.resetCodeNote.textContent = "";
      }
      switchView("reset");
      setStatus(elements.resetStatus, "验证码已发送，请继续设置新密码。", "success");
    } catch (error) {
      setStatus(elements.forgotStatus, readError(error, "验证码发送失败，请稍后重试。"), "error");
    } finally {
      setSubmitting(form, false);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!validateForm(form, elements.resetStatus)) return;
    const values = formValues(form);
    if (values.newPassword !== values.newPasswordConfirm) {
      markInvalid(form.elements.newPasswordConfirm);
      setStatus(elements.resetStatus, "两次输入的新密码不一致。", "error");
      return;
    }

    setSubmitting(form, true, "正在保存新密码…", elements.resetStatus);
    try {
      await request("/auth/reset-password", {
        method: "POST",
        body: {
          email: values.email.trim(),
          code: values.code.trim(),
          newPassword: values.newPassword
        },
        publicRequest: true
      });
      form.reset();
      elements.resetCodeNote.hidden = true;
      switchView("login");
      setStatus(elements.loginStatus, "密码已重置，请使用新密码登录。", "success");
    } catch (error) {
      setStatus(elements.resetStatus, readError(error, "密码重置失败，请检查验证码。"), "error");
    } finally {
      setSubmitting(form, false);
    }
  }

  function validateForm(form, statusElement) {
    clearInvalid(form);
    if (form.checkValidity()) return true;
    [...form.elements].forEach((field) => {
      if (field instanceof HTMLInputElement && !field.validity.valid) markInvalid(field);
    });
    setStatus(statusElement, "请检查必填项和输入格式。", "error");
    return false;
  }

  function clearInvalid(form) {
    form.querySelectorAll("[aria-invalid='true']").forEach((field) => field.removeAttribute("aria-invalid"));
  }

  function markInvalid(field) {
    if (field) field.setAttribute("aria-invalid", "true");
  }

  function setSubmitting(form, submitting, message, statusElement) {
    [...form.elements].forEach((field) => { field.disabled = submitting; });
    if (submitting && message && statusElement) setStatus(statusElement, message, "loading");
  }

  function setStatus(element, message, type) {
    if (!element) return;
    element.hidden = !message;
    element.className = `auth-form-status${type ? ` is-${type}` : ""}`;
    element.textContent = message || "";
  }

  function clearFormStatuses() {
    [elements.loginStatus, elements.registerStatus, elements.forgotStatus, elements.resetStatus]
      .forEach((element) => {
        if (!element) return;
        element.hidden = true;
        element.textContent = "";
        element.className = "auth-form-status";
      });
  }

  function formValues(form) {
    return Object.fromEntries(new FormData(form).entries());
  }

  function detectTimeZone() {
    try {
      const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
      return zone && /^[A-Za-z_]+(\/[A-Za-z0-9_+-]+)+$/.test(zone) ? zone : "Asia/Seoul";
    } catch (error) {
      return "Asia/Seoul";
    }
  }

  async function request(path, options = {}) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 12000);
    const method = String(options.method || "GET").toUpperCase();
    const headers = { Accept: "application/json" };
    if (options.body !== undefined) headers["Content-Type"] = "application/json";
    if (!options.publicRequest && method !== "GET" && method !== "HEAD") {
      headers["X-CSRF-Token"] = readCsrf();
    }

    try {
      const response = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        credentials: "same-origin",
        signal: controller.signal,
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
      });
      const contentType = response.headers.get("content-type") || "";
      const payload = contentType.includes("application/json") ? await response.json() : null;
      if (!response.ok || payload?.success === false) {
        const error = new Error(payload?.message || `请求失败（${response.status}）`);
        error.status = response.status;
        error.details = payload?.errors;
        throw error;
      }
      return payload && Object.prototype.hasOwnProperty.call(payload, "data") ? payload.data : payload;
    } catch (error) {
      if (error.name === "AbortError") throw new Error("请求超时，请稍后重试。");
      throw error;
    } finally {
      window.clearTimeout(timeout);
    }
  }

  function redirectByRole(role) {
    const destination = String(role || "USER").toUpperCase() === "ADMIN"
      ? "/admin.html"
      : safeReturnPath();
    window.location.replace(destination);
  }

  function safeReturnPath() {
    const next = new URLSearchParams(window.location.search).get("next");
    if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/login.html")) return "/";
    return next;
  }

  function rememberCsrf(token) {
    if (!token) return;
    try {
      sessionStorage.setItem(CSRF_STORAGE_KEY, String(token));
    } catch (error) {
      // The authenticated page will refresh the token through /auth/me.
    }
  }

  function readCsrf() {
    try {
      return sessionStorage.getItem(CSRF_STORAGE_KEY) || "";
    } catch (error) {
      return "";
    }
  }

  function clearCsrf() {
    try {
      sessionStorage.removeItem(CSRF_STORAGE_KEY);
    } catch (error) {
      // No stored token needs to be cleared when storage is unavailable.
    }
  }

  function toggleTheme() {
    const nextTheme = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nextTheme;
    try {
      localStorage.setItem("trivocab-theme", nextTheme);
    } catch (error) {
      // Theme still applies for this page when storage is unavailable.
    }
    syncThemeControl();
  }

  function syncThemeControl() {
    const dark = document.documentElement.dataset.theme === "dark";
    elements.themeToggle.setAttribute("aria-pressed", String(dark));
    elements.themeToggle.setAttribute("aria-label", dark ? "切换为浅色模式" : "切换为深色模式");
    elements.themeToggleLabel.textContent = dark ? "深色" : "浅色";
    elements.themeColorMeta.setAttribute("content", dark ? "#101512" : "#f4f2eb");
  }

  function cleanText(value) {
    return String(value || "").trim();
  }

  function readError(error, fallback) {
    return error instanceof Error && error.message ? error.message : fallback;
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, (character) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      "'": "&#39;",
      "\"": "&quot;"
    })[character]);
  }
})();
