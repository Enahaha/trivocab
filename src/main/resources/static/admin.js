(() => {
  "use strict";

  const API_BASE = "/api/v1";
  const CSRF_STORAGE_KEY = "trivocab-csrf-token";
  const PAGE_SIZE = 10;

  const state = {
    user: null,
    csrfToken: "",
    route: "overview",
    dashboardLoaded: false,
    books: { items: [], loaded: false },
    users: pageState(),
    messages: pageState(),
    words: pageState(),
    confirmAction: null,
    confirmBusy: false,
    wordEditorBusy: false,
    bookEditorBusy: false
  };

  const elements = {
    boot: document.querySelector("#adminBoot"),
    bootTitle: document.querySelector("#adminBootTitle"),
    bootMessage: document.querySelector("#adminBootMessage"),
    bootRetry: document.querySelector("#adminBootRetry"),
    shell: document.querySelector("#adminShell"),
    routeButtons: [...document.querySelectorAll("[data-admin-route]")],
    views: [...document.querySelectorAll("[data-admin-view]")],
    pageTitle: document.querySelector("#adminPageTitle"),
    pageDescription: document.querySelector("#adminPageDescription"),
    themeToggle: document.querySelector("#adminThemeToggle"),
    themeLabel: document.querySelector("#adminThemeLabel"),
    themeColorMeta: document.querySelector("#themeColorMeta"),
    logoutButtons: [document.querySelector("#adminLogoutButton"), document.querySelector("#adminMobileLogoutButton")],
    avatar: document.querySelector("#adminAvatar"),
    displayName: document.querySelector("#adminDisplayName"),
    username: document.querySelector("#adminUsername"),
    refreshDashboard: document.querySelector("#refreshAdminDashboard"),
    overviewState: document.querySelector("#overviewState"),
    metricGrid: document.querySelector("#adminMetricGrid"),
    totalUsers: document.querySelector("#adminTotalUsers"),
    enabledUsers: document.querySelector("#adminEnabledUsers"),
    newMessages: document.querySelector("#adminNewMessages"),
    totalMessages: document.querySelector("#adminTotalMessages"),
    totalWords: document.querySelector("#adminTotalWords"),
    todayReviews: document.querySelector("#adminTodayReviews"),
    userSearchForm: document.querySelector("#userSearchForm"),
    userKeyword: document.querySelector("#userKeyword"),
    usersState: document.querySelector("#usersState"),
    usersTableBody: document.querySelector("#usersTableBody"),
    usersEmpty: document.querySelector("#usersEmpty"),
    usersPagination: document.querySelector("#usersPagination"),
    messageFilterForm: document.querySelector("#messageFilterForm"),
    messageKeyword: document.querySelector("#messageKeyword"),
    messageStatusFilter: document.querySelector("#messageStatusFilter"),
    messagesState: document.querySelector("#messagesState"),
    messageList: document.querySelector("#adminMessageList"),
    messagesEmpty: document.querySelector("#adminMessagesEmpty"),
    messagesPagination: document.querySelector("#messagesPagination"),
    addBookButton: document.querySelector("#addBookButton"),
    booksState: document.querySelector("#booksState"),
    booksTableBody: document.querySelector("#adminBooksTableBody"),
    booksEmpty: document.querySelector("#adminBooksEmpty"),
    bookFilter: document.querySelector("#adminBookFilter"),
    bookEditorModal: document.querySelector("#bookEditorModal"),
    bookEditorTitle: document.querySelector("#bookEditorTitle"),
    bookEditorForm: document.querySelector("#bookEditorForm"),
    bookEditorStatus: document.querySelector("#bookEditorStatus"),
    closeBookEditor: document.querySelector("#closeBookEditorButton"),
    cancelBookEditor: document.querySelector("#cancelBookEditorButton"),
    addWordButton: document.querySelector("#addWordButton"),
    wordSearchForm: document.querySelector("#wordSearchForm"),
    wordKeyword: document.querySelector("#adminWordKeyword"),
    wordBookSelect: document.querySelector("#wordBookSelect"),
    wordsState: document.querySelector("#wordsState"),
    wordsTableBody: document.querySelector("#adminWordsTableBody"),
    wordsEmpty: document.querySelector("#adminWordsEmpty"),
    wordsPagination: document.querySelector("#wordsPagination"),
    confirmModal: document.querySelector("#confirmModal"),
    confirmTitle: document.querySelector("#confirmTitle"),
    confirmMessage: document.querySelector("#confirmMessage"),
    cancelConfirm: document.querySelector("#cancelConfirmButton"),
    confirmDelete: document.querySelector("#confirmDeleteButton"),
    wordEditorModal: document.querySelector("#wordEditorModal"),
    wordEditorTitle: document.querySelector("#wordEditorTitle"),
    wordEditorForm: document.querySelector("#wordEditorForm"),
    wordEditorStatus: document.querySelector("#wordEditorStatus"),
    closeWordEditor: document.querySelector("#closeWordEditorButton"),
    cancelWordEditor: document.querySelector("#cancelWordEditorButton"),
    toastRegion: document.querySelector("#adminToastRegion")
  };

  document.addEventListener("DOMContentLoaded", init);

  function init() {
    syncThemeControl();
    bindEvents();
    authenticateAdmin();
  }

  function bindEvents() {
    elements.routeButtons.forEach((button) => {
      button.addEventListener("click", (event) => {
        event.preventDefault();
        navigate(button.dataset.adminRoute);
      });
    });
    elements.themeToggle.addEventListener("click", toggleTheme);
    elements.logoutButtons.filter(Boolean).forEach((button) => button.addEventListener("click", logout));
    elements.bootRetry.addEventListener("click", authenticateAdmin);
    elements.refreshDashboard.addEventListener("click", () => loadDashboard(true));

    elements.userSearchForm.addEventListener("submit", (event) => {
      event.preventDefault();
      state.users.keyword = elements.userKeyword.value.trim();
      loadUsers(0);
    });
    elements.messageFilterForm.addEventListener("submit", (event) => {
      event.preventDefault();
      state.messages.keyword = elements.messageKeyword.value.trim();
      state.messages.status = elements.messageStatusFilter.value;
      loadMessages(0);
    });
    document.addEventListener("change", (event) => {
      const input = event.target.closest("[data-page-input]");
      if (input) jumpToAdminPage(input);
    });
    document.addEventListener("keydown", (event) => {
      if (event.key !== "Enter") return;
      const input = event.target.closest("[data-page-input]");
      if (input) {
        event.preventDefault();
        jumpToAdminPage(input);
      }
    });
    elements.wordSearchForm.addEventListener("submit", (event) => {
      event.preventDefault();
      state.words.keyword = elements.wordKeyword.value.trim();
      loadWords(0);
    });
    elements.bookFilter.addEventListener("change", (event) => {
      state.words.bookId = event.target.value ? Number(event.target.value) : "";
      loadWords(0);
    });

    elements.usersTableBody.addEventListener("click", handleUserAction);
    elements.messageList.addEventListener("click", handleMessageAction);
    elements.wordsTableBody.addEventListener("click", handleWordAction);
    elements.booksTableBody.addEventListener("click", handleBookAction);
    [elements.usersPagination, elements.messagesPagination, elements.wordsPagination]
      .forEach((pagination) => pagination.addEventListener("click", handlePagination));

    elements.addWordButton.addEventListener("click", () => openWordEditor());
    elements.addBookButton.addEventListener("click", () => openBookEditor());
    elements.closeBookEditor.addEventListener("click", closeBookEditor);
    elements.cancelBookEditor.addEventListener("click", closeBookEditor);
    elements.bookEditorForm.addEventListener("submit", saveBook);
    elements.bookEditorModal.addEventListener("click", (event) => {
      if (event.target === elements.bookEditorModal) closeBookEditor();
    });
    elements.closeWordEditor.addEventListener("click", closeWordEditor);
    elements.cancelWordEditor.addEventListener("click", closeWordEditor);
    elements.wordEditorForm.addEventListener("submit", saveWord);
    elements.wordEditorModal.addEventListener("click", (event) => {
      if (event.target === elements.wordEditorModal) closeWordEditor();
    });

    elements.cancelConfirm.addEventListener("click", closeConfirm);
    elements.confirmDelete.addEventListener("click", runConfirmAction);
    elements.confirmModal.addEventListener("click", (event) => {
      if (event.target === elements.confirmModal) closeConfirm();
    });

    document.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") return;
      if (!elements.confirmModal.hidden) closeConfirm();
      else if (!elements.bookEditorModal.hidden) closeBookEditor();
      else if (!elements.wordEditorModal.hidden) closeWordEditor();
    });

    document.addEventListener("click", (event) => {
      const retryButton = event.target.closest("[data-retry-section]");
      if (!retryButton) return;
      retrySection(retryButton.dataset.retrySection);
    });
  }

  async function authenticateAdmin() {
    showBoot("正在进入管理后台", "正在确认管理员权限…", false);
    try {
      const user = await apiRequest("/auth/me", { authRedirect: false });
      if (String(user?.role || "").toUpperCase() !== "ADMIN") {
        window.location.replace("/");
        return;
      }
      state.user = user;
      state.csrfToken = cleanText(user.csrfToken) || readStoredCsrf();
      storeCsrf(state.csrfToken);
      renderAdminUser();
      elements.boot.hidden = true;
      elements.shell.hidden = false;
      elements.shell.removeAttribute("inert");
      document.body.classList.remove("auth-pending");
      const requested = window.location.hash.replace("#", "");
      navigate(["overview", "users", "messages", "books", "words"].includes(requested) ? requested : "overview", false);
    } catch (error) {
      if (error.status === 401) {
        redirectToLogin();
        return;
      }
      showBoot("无法进入管理后台", readError(error, "请检查服务状态后重试。"), true);
    }
  }

  function showBoot(title, message, retryVisible) {
    elements.boot.hidden = false;
    elements.shell.hidden = true;
    elements.shell.setAttribute("inert", "");
    elements.bootTitle.textContent = title;
    elements.bootMessage.textContent = message;
    elements.bootRetry.hidden = !retryVisible;
  }

  function renderAdminUser() {
    const displayName = cleanText(state.user?.displayName) || cleanText(state.user?.username) || "管理员";
    elements.displayName.textContent = displayName;
    elements.username.textContent = state.user?.username ? `@${state.user.username}` : "ADMIN";
    elements.avatar.textContent = Array.from(displayName)[0] || "管";
  }

  function navigate(route, updateHash = true) {
    if (!elements.views.some((view) => view.dataset.adminView === route)) return;
    state.route = route;
    elements.views.forEach((view) => {
      const active = view.dataset.adminView === route;
      view.hidden = !active;
      view.classList.toggle("is-active", active);
    });
    elements.routeButtons.forEach((button) => {
      const active = button.dataset.adminRoute === route;
      button.classList.toggle("is-active", active);
      if (active) button.setAttribute("aria-current", "page");
      else button.removeAttribute("aria-current");
    });
    const pageMeta = {
      overview: ["管理概览", "查看平台的实际运行数据。"],
      users: ["用户管理", "查看账号、状态和学习量。"],
      messages: ["留言管理", "阅读反馈并记录处理结果。"],
      books: ["词书管理", "添加、修改或删除词书。"],
      words: ["词库管理", "维护英文、中文和韩语词条。"]
    }[route];
    elements.pageTitle.textContent = pageMeta[0];
    elements.pageDescription.textContent = pageMeta[1];
    if (updateHash) history.replaceState(null, "", `#${route}`);
    window.scrollTo({ top: 0, behavior: prefersReducedMotion() ? "auto" : "smooth" });
    loadRoute(route);
  }

  function loadRoute(route) {
    if (route === "overview" && !state.dashboardLoaded) loadDashboard();
    if (route === "users" && !state.users.loaded) loadUsers(0);
    if (route === "messages" && !state.messages.loaded) loadMessages(0);
    if (route === "books" && !state.books.loaded) loadBooks();
    if (route === "words" && !state.books.loaded) loadBooks();
    if (route === "words" && !state.words.loaded) loadWords(0);
  }

  async function loadDashboard(force = false) {
    if (elements.metricGrid.getAttribute("aria-busy") === "true" && !force && state.dashboardLoaded) return;
    elements.metricGrid.setAttribute("aria-busy", "true");
    elements.refreshDashboard.disabled = true;
    setInlineState(elements.overviewState, "正在读取概览数据…", "loading");
    try {
      const data = await apiRequest("/admin/dashboard");
      elements.totalUsers.textContent = formatNumber(numberValue(data?.totalUsers, data?.userCount, 0));
      elements.enabledUsers.textContent = formatNumber(numberValue(data?.todayActiveUsers, data?.activeUsers, 0));
      elements.newMessages.textContent = formatNumber(numberValue(data?.todayMessageCount, data?.newMessages, 0));
      elements.totalMessages.textContent = formatNumber(numberValue(data?.todayNewUsers, data?.todayLoginCount, 0));
      elements.totalWords.textContent = formatNumber(numberValue(data?.totalWords, data?.wordCount, 0));
      elements.todayReviews.textContent = formatNumber(numberValue(data?.todayReviewCount, data?.reviewCountToday, 0));
      state.dashboardLoaded = true;
      clearInlineState(elements.overviewState);
    } catch (error) {
      setInlineError(elements.overviewState, readError(error, "概览数据加载失败。"), "overview");
    } finally {
      elements.metricGrid.setAttribute("aria-busy", "false");
      elements.refreshDashboard.disabled = false;
    }
  }

  async function loadUsers(page = 0) {
    state.users.loading = true;
    renderTableSkeleton(elements.usersTableBody, 6);
    elements.usersEmpty.hidden = true;
    setInlineState(elements.usersState, "正在读取用户…", "loading");
    try {
      const query = listQuery(page, state.users.keyword);
      const data = await apiRequest(`/admin/users?${query}`);
      Object.assign(state.users, normalizePage(data, page), { loaded: true, loading: false });
      renderUsers();
      clearInlineState(elements.usersState);
    } catch (error) {
      state.users.loading = false;
      elements.usersTableBody.innerHTML = "";
      setInlineError(elements.usersState, readError(error, "用户列表加载失败。"), "users");
    }
  }

  function renderUsers() {
    elements.usersTableBody.innerHTML = state.users.items.map((user) => {
      const isAdmin = String(user.role || "USER").toUpperCase() === "ADMIN";
      const isSelf = Number(user.id) === Number(state.user?.id);
      const canDelete = !isAdmin && !isSelf;
      const displayName = cleanText(user.displayName) || cleanText(user.username) || `用户 ${user.id ?? ""}`;
      return `
        <tr>
          <td data-label="用户"><div class="admin-primary-cell"><strong>${escapeHtml(displayName)}</strong><small>${escapeHtml(user.username ? `@${user.username}` : user.email || "无用户名")}</small></div></td>
          <td data-label="角色"><span class="admin-status${isAdmin ? " is-active" : ""}">${isAdmin ? "管理员" : "学习者"}</span></td>
          <td data-label="学习单词">${formatNumber(numberValue(user.learnedWords, user.learnedCount, 0))}</td>
          <td data-label="最后登录">${escapeHtml(formatUtcDateTime(user.lastLoginAt, "尚未登录"))}</td>
          <td data-label="状态"><span class="admin-status${user.enabled !== false ? " is-active" : ""}">${user.enabled !== false ? "已启用" : "已停用"}</span></td>
          <td data-label="操作"><div class="admin-table-actions"><button class="admin-table-action is-danger" type="button" data-delete-user="${escapeHtml(user.id)}" ${canDelete ? "" : "disabled"} title="${canDelete ? "删除用户" : (isSelf ? "不能删除当前账号" : "不能删除管理员")}">${canDelete ? "删除" : "不可删除"}</button></div></td>
        </tr>`;
    }).join("");
    elements.usersEmpty.hidden = state.users.items.length > 0;
    renderPagination(elements.usersPagination, state.users, "users");
  }

  async function loadMessages(page = 0) {
    state.messages.loading = true;
    elements.messageList.setAttribute("aria-busy", "true");
    elements.messageList.innerHTML = renderMessageSkeletons();
    elements.messagesEmpty.hidden = true;
    setInlineState(elements.messagesState, "正在读取留言…", "loading");
    try {
      const query = listQuery(page, state.messages.keyword, { status: state.messages.status });
      const data = await apiRequest(`/admin/messages?${query}`);
      Object.assign(state.messages, normalizePage(data, page), { loaded: true, loading: false });
      renderMessages();
      clearInlineState(elements.messagesState);
    } catch (error) {
      state.messages.loading = false;
      elements.messageList.innerHTML = "";
      setInlineError(elements.messagesState, readError(error, "留言列表加载失败。"), "messages");
    } finally {
      elements.messageList.setAttribute("aria-busy", "false");
    }
  }

  function renderMessages() {
    elements.messageList.innerHTML = state.messages.items.map((message) => {
      const status = normalizeMessageStatus(message.status);
      const displayName = cleanText(message.displayName) || cleanText(message.username) || `用户 ${message.userId ?? ""}`;
      return `
        <article class="admin-message-card" data-message-id="${escapeHtml(message.id)}">
          <div class="admin-message-author">
            <strong>${escapeHtml(displayName)}</strong>
            <small>${escapeHtml(message.username ? `@${message.username}` : `用户 ID ${message.userId ?? "未知"}`)}</small>
            <span class="admin-status ${status.css}">${status.label}</span>
            <time class="admin-message-time" datetime="${escapeHtml(dateTimeAttribute(message.createdAt))}">${escapeHtml(formatDateTime(message.createdAt, "时间未知"))}</time>
          </div>
          <div class="admin-message-content">
            <p>${escapeHtml(message.content || "留言内容为空")}</p>
            <div class="admin-message-controls">
              <div class="admin-select-field"><label for="messageStatus-${escapeHtml(message.id)}">处理状态</label><select id="messageStatus-${escapeHtml(message.id)}" data-message-status>${messageStatusOptions(status.value)}</select></div>
              <div class="admin-field"><label for="messageReply-${escapeHtml(message.id)}">管理员回复</label><textarea id="messageReply-${escapeHtml(message.id)}" data-message-reply rows="2" maxlength="2000" placeholder="输入给学习者的回复">${escapeHtml(message.adminReply || "")}</textarea></div>
              <div class="admin-message-actions"><button class="admin-button admin-button-primary" type="button" data-save-message>保存</button><button class="admin-button admin-button-secondary" type="button" data-delete-message>删除</button></div>
            </div>
            <div class="admin-message-action-state" data-message-action-state role="status" aria-live="polite"></div>
          </div>
        </article>`;
    }).join("");
    elements.messagesEmpty.hidden = state.messages.items.length > 0;
    renderPagination(elements.messagesPagination, state.messages, "messages");
  }

  async function loadWords(page = 0) {
    state.words.loading = true;
    renderTableSkeleton(elements.wordsTableBody, 7);
    elements.wordsEmpty.hidden = true;
    setInlineState(elements.wordsState, "正在读取词库…", "loading");
    try {
      const query = listQuery(page, state.words.keyword, {
        ...(state.words.bookId ? { bookId: state.words.bookId } : {})
      });
      const data = await apiRequest(`/admin/words?${query}`);
      Object.assign(state.words, normalizePage(data, page), { loaded: true, loading: false });
      renderWords();
      clearInlineState(elements.wordsState);
    } catch (error) {
      state.words.loading = false;
      elements.wordsTableBody.innerHTML = "";
      setInlineError(elements.wordsState, readError(error, "词库加载失败。"), "words");
    }
  }

  function renderWords() {
    elements.wordsTableBody.innerHTML = state.words.items.map((word) => `
      <tr data-word-id="${escapeHtml(word.id)}">
        <td data-label="Word ID"><span class="admin-truncate">${escapeHtml(word.wordId || "未编号")}</span></td>
        <td data-label="顺序">${formatNumber(numberValue(word.priorityRank, word.id, 0))}</td>
        <td data-label="单词"><div class="admin-primary-cell admin-word-cell"><strong lang="en">${escapeHtml(word.word || "…")}</strong><small>${escapeHtml([formatPhonetic(word.phonetic), word.partOfSpeech].filter(Boolean).join(" / ") || "无音标")}</small></div></td>
        <td data-label="中文释义"><span class="admin-truncate">${escapeHtml(word.chineseMeaning || "待补充")}</span></td>
        <td data-label="한국어 뜻"><span class="admin-truncate" lang="ko" title="${escapeHtml([word.koreanMeaning, word.koreanSourceFlag].filter(Boolean).join(" · "))}">${escapeHtml(word.koreanMeaning || "추가 필요")}${word.koreanSourceFlag ? `<small class="admin-source-flag">${escapeHtml(word.koreanSourceFlag)}</small>` : ""}</span></td>
        <td data-label="阶段"><span class="admin-truncate">${escapeHtml(word.learningStage || "未分类")}</span></td>
        <td data-label="操作"><div class="admin-table-actions"><button class="admin-table-action" type="button" data-edit-word>编辑</button><button class="admin-table-action is-danger" type="button" data-delete-word>删除</button></div></td>
      </tr>`).join("");
    elements.wordsEmpty.hidden = state.words.items.length > 0;
    renderPagination(elements.wordsPagination, state.words, "words");
  }

  function handleUserAction(event) {
    const button = event.target.closest("[data-delete-user]");
    if (!button || button.disabled) return;
    const id = button.dataset.deleteUser;
    const user = state.users.items.find((item) => String(item.id) === String(id));
    if (!user || String(user.role || "").toUpperCase() === "ADMIN" || Number(user.id) === Number(state.user?.id)) return;
    openConfirm({
      title: "删除用户",
      message: `确认删除用户“${cleanText(user.displayName) || cleanText(user.username) || user.id}”？该操作无法恢复。`,
      action: async () => {
        await apiRequest(`/admin/users/${encodeURIComponent(id)}`, { method: "DELETE" });
        showToast("用户已删除。");
        await loadUsers(pageAfterDelete(state.users));
      }
    });
  }

  function handleMessageAction(event) {
    const card = event.target.closest("[data-message-id]");
    if (!card) return;
    const id = card.dataset.messageId;
    if (event.target.closest("[data-save-message]")) saveMessage(card, id);
    if (event.target.closest("[data-delete-message]")) {
      openConfirm({
        title: "删除留言",
        message: "确认删除这条留言和已保存的回复？该操作无法恢复。",
        action: async () => {
          await apiRequest(`/admin/messages/${encodeURIComponent(id)}`, { method: "DELETE" });
          showToast("留言已删除。");
          await loadMessages(pageAfterDelete(state.messages));
        }
      });
    }
  }

  async function saveMessage(card, id) {
    const status = card.querySelector("[data-message-status]").value;
    const adminReply = card.querySelector("[data-message-reply]").value.trim();
    const controls = [...card.querySelectorAll("button, select, textarea")];
    const actionState = card.querySelector("[data-message-action-state]");
    controls.forEach((control) => { control.disabled = true; });
    setMessageActionState(actionState, "正在保存这条留言…");
    try {
      const updated = await apiRequest(`/admin/messages/${encodeURIComponent(id)}`, {
        method: "PATCH",
        body: { status, adminReply }
      });
      const effectiveStatus = cleanText(updated?.status) || (adminReply ? "REPLIED" : status);
      const itemIndex = state.messages.items.findIndex((item) => String(item.id) === String(id));
      if (itemIndex >= 0) {
        state.messages.items[itemIndex] = {
          ...state.messages.items[itemIndex],
          ...updated,
          status: effectiveStatus,
          adminReply: updated?.adminReply ?? adminReply
        };
      }
      const statusMeta = normalizeMessageStatus(effectiveStatus);
      const statusBadge = card.querySelector(".admin-message-author .admin-status");
      const statusSelect = card.querySelector("[data-message-status]");
      if (statusBadge) {
        statusBadge.className = `admin-status ${statusMeta.css}`.trim();
        statusBadge.textContent = statusMeta.label;
      }
      if (statusSelect) statusSelect.value = effectiveStatus;
      setMessageActionState(actionState, "已保存状态和回复。");
      showToast("留言处理结果已保存。");
    } catch (error) {
      setMessageActionState(actionState, readError(error, "这条留言保存失败。"), true);
    } finally {
      controls.forEach((control) => { control.disabled = false; });
    }
  }

  function handleWordAction(event) {
    const row = event.target.closest("[data-word-id]");
    if (!row) return;
    const id = row.dataset.wordId;
    const word = state.words.items.find((item) => String(item.id) === String(id));
    if (!word) return;
    if (event.target.closest("[data-edit-word]")) openWordEditor(word);
    if (event.target.closest("[data-delete-word]")) {
      openConfirm({
        title: "删除单词",
        message: `确认删除单词“${cleanText(word.word) || word.id}”？相关学习记录可能同时受影响。`,
        action: async () => {
          await apiRequest(`/admin/words/${encodeURIComponent(id)}`, { method: "DELETE" });
          showToast("单词已删除。");
          await loadWords(pageAfterDelete(state.words));
        }
      });
    }
  }

  function handlePagination(event) {
    const button = event.target.closest("[data-page-scope][data-page-target]");
    if (!button || button.disabled) return;
    const page = Math.max(0, Number(button.dataset.pageTarget) || 0);
    const scope = button.dataset.pageScope;
    if (scope === "users") loadUsers(page);
    if (scope === "messages") loadMessages(page);
    if (scope === "words") loadWords(page);
  }

  function renderPagination(element, data, scope) {
    const totalPages = Math.max(0, numberValue(data.totalPages, 0));
    if (totalPages <= 1) {
      element.innerHTML = data.total > 0 ? `<span>共 ${formatNumber(data.total)} 条</span>` : "";
      return;
    }
    const current = clamp(numberValue(data.page, 0), 0, totalPages - 1);
    element.innerHTML = `
      <span>第 ${current + 1} / ${totalPages} 页，共 ${formatNumber(data.total)} 条</span>
      <button type="button" data-page-scope="${scope}" data-page-target="${current - 1}" ${current <= 0 ? "disabled" : ""}>上一页</button>
      <button type="button" data-page-scope="${scope}" data-page-target="${current + 1}" ${current + 1 >= totalPages ? "disabled" : ""}>下一页</button>
      <label class="admin-page-jump">跳至 <input type="number" min="1" max="${totalPages}" value="${current + 1}" data-page-scope="${scope}" data-page-input aria-label="跳转页码"> 页</label>`;
  }

  function jumpToAdminPage(input) {
    const scope = input.dataset.pageScope;
    const totalPages = Math.max(1, numberValue(input.max, 1));
    const requested = Math.floor(Number(input.value) || 0);
    const page = clamp(requested - 1, 0, totalPages - 1);
    input.value = String(page + 1);
    if (scope === "users") loadUsers(page);
    if (scope === "messages") loadMessages(page);
    if (scope === "words") loadWords(page);
  }

  async function loadBooks() {
    state.books.loading = true;
    setInlineState(elements.booksState, "正在读取词书…", "loading");
    try {
      const data = await apiRequest("/admin/books");
      state.books = { items: asArray(data), loaded: true, loading: false };
      clearInlineState(elements.booksState);
      renderBooks();
      populateBookSelects();
    } catch (error) {
      state.books.loading = false;
      setInlineError(elements.booksState, readError(error, "词书加载失败。"), "books");
    }
  }

  function renderBooks() {
    elements.booksTableBody.innerHTML = state.books.items.map((book) => `
      <tr data-book-id="${escapeHtml(book.id)}">
        <td>${formatNumber(numberValue(book.id, 0))}</td>
        <td><code>${escapeHtml(book.code || "未编码")}</code></td>
        <td><div class="admin-primary-cell"><strong>${escapeHtml(book.name || "未命名词书")}</strong></div></td>
        <td><span class="admin-truncate">${escapeHtml(book.description || "暂无说明")}</span></td>
        <td>${formatNumber(numberValue(book.totalWords, 0))}</td>
        <td><div class="admin-table-actions"><button class="admin-table-action" type="button" data-edit-book>编辑</button><button class="admin-table-action is-danger" type="button" data-delete-book>删除</button></div></td>
      </tr>`).join("");
    elements.booksEmpty.hidden = state.books.items.length > 0;
  }

  function populateBookSelects() {
    const options = state.books.items.map((book) =>
      `<option value="${escapeHtml(book.id)}">${escapeHtml(book.name || `词书 ${book.id}`)}（${formatNumber(numberValue(book.totalWords, 0))} 词）</option>`
    ).join("");
    elements.bookFilter.innerHTML = `<option value="">全部词书</option>${options}`;
    elements.wordBookSelect.innerHTML = options || "<option value=\"\">请先新增词书</option>";
    elements.wordBookSelect.value = String(state.words.bookId || "");
  }

  function handleBookAction(event) {
    const row = event.target.closest("[data-book-id]");
    if (!row) return;
    const id = row.dataset.bookId;
    const book = state.books.items.find((item) => String(item.id) === String(id));
    if (!book) return;
    if (event.target.closest("[data-edit-book]")) openBookEditor(book);
    if (event.target.closest("[data-delete-book]")) {
      openConfirm({
        title: "删除词书",
        message: `确认删除词书“${cleanText(book.name) || book.code || book.id}”？该词书下的 ${formatNumber(numberValue(book.totalWords, 0))} 个单词、学习进度与复习记录都会被删除，且无法恢复。`,
        action: async () => {
          await apiRequest(`/admin/books/${encodeURIComponent(id)}`, { method: "DELETE" });
          showToast("词书已删除。");
          state.books.loaded = false;
          state.words.loaded = false;
          state.words.bookId = "";
          await loadBooks();
          if (state.route === "words") await loadWords(0);
        }
      });
    }
  }

  function openBookEditor(book = null) {
    elements.bookEditorForm.reset();
    clearInvalid(elements.bookEditorForm);
    clearInlineState(elements.bookEditorStatus);
    const fields = elements.bookEditorForm.elements;
    fields.id.value = book?.id ?? "";
    fields.code.value = book?.code ?? "";
    fields.name.value = book?.name ?? "";
    fields.description.value = book?.description ?? "";
    elements.bookEditorTitle.textContent = book ? "编辑词书" : "新增词书";
    elements.bookEditorModal.hidden = false;
    elements.shell.setAttribute("inert", "");
    document.body.classList.add("has-modal");
  }

  function closeBookEditor() {
    if (state.bookEditorBusy) return;
    elements.bookEditorModal.hidden = true;
    elements.shell.removeAttribute("inert");
    document.body.classList.remove("has-modal");
  }

  async function saveBook(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearInvalid(form);
    if (!form.checkValidity()) {
      [...form.elements].forEach((field) => {
        if ((field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) && !field.validity.valid) {
          field.setAttribute("aria-invalid", "true");
        }
      });
      setInlineState(elements.bookEditorStatus, "请检查必填项和输入格式。", "error");
      return;
    }

    const values = Object.fromEntries(new FormData(form).entries());
    const id = cleanText(values.id);
    const payload = {
      code: cleanText(values.code).toUpperCase(),
      name: cleanText(values.name),
      description: emptyToNull(values.description)
    };

    state.bookEditorBusy = true;
    [...form.elements].forEach((field) => { field.disabled = true; });
    setInlineState(elements.bookEditorStatus, id ? "正在保存修改…" : "正在新增词书…", "loading");
    try {
      await apiRequest(id ? `/admin/books/${encodeURIComponent(id)}` : "/admin/books", {
        method: id ? "PUT" : "POST",
        body: payload
      });
      showToast(id ? "词书修改已保存。" : "词书已新增，现在可以添加单词了。");
      state.books.loaded = false;
      await loadBooks();
      state.bookEditorBusy = false;
      [...form.elements].forEach((field) => { field.disabled = false; });
      closeBookEditor();
    } catch (error) {
      setInlineState(elements.bookEditorStatus, readError(error, "词书保存失败。"), "error");
    } finally {
      state.bookEditorBusy = false;
      [...form.elements].forEach((field) => { field.disabled = false; });
    }
  }

  function openWordEditor(word = null) {
    elements.wordEditorForm.reset();
    clearInvalid(elements.wordEditorForm);
    clearInlineState(elements.wordEditorStatus);
    const fields = elements.wordEditorForm.elements;
    fields.id.value = word?.id ?? "";
    populateBookSelects();
    fields.bookId.value = String(numberValue(word?.bookId, state.books.items[0]?.id || 1));
    fields.wordId.value = word?.wordId ?? "";
    fields.priorityRank.value = word?.priorityRank ?? "";
    fields.word.value = word?.word ?? "";
    fields.phonetic.value = word?.phonetic ?? "";
    fields.partOfSpeech.value = word?.partOfSpeech ?? "";
    fields.chineseMeaning.value = word?.chineseMeaning ?? "";
    fields.koreanMeaning.value = word?.koreanMeaning ?? "";
    fields.koreanEquivalents.value = word?.koreanEquivalents ?? "";
    fields.koreanDefinition.value = word?.koreanDefinition ?? "";
    fields.koreanSourceFlag.value = word?.koreanSourceFlag ?? "";
    fields.englishExample.value = word?.englishExample ?? "";
    fields.koreanExample.value = word?.koreanExample ?? "";
    fields.learningStage.value = word?.learningStage ?? "";
    fields.selectionBasis.value = word?.selectionBasis ?? "";
    fields.sourceName.value = word?.sourceName ?? "";
    fields.sourceUrl.value = word?.sourceUrl ?? "";
    elements.wordEditorTitle.textContent = word ? "编辑单词" : "新增单词";
    elements.wordEditorModal.hidden = false;
    elements.shell.setAttribute("inert", "");
    document.body.classList.add("has-modal");
  }

  function closeWordEditor() {
    if (state.wordEditorBusy) return;
    elements.wordEditorModal.hidden = true;
    elements.shell.removeAttribute("inert");
    document.body.classList.remove("has-modal");
  }

  async function saveWord(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearInvalid(form);
    if (!form.checkValidity()) {
      [...form.elements].forEach((field) => {
        if ((field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) && !field.validity.valid) {
          field.setAttribute("aria-invalid", "true");
        }
      });
      setInlineState(elements.wordEditorStatus, "请检查必填项和输入格式。", "error");
      return;
    }

    const values = Object.fromEntries(new FormData(form).entries());
    const id = cleanText(values.id);
    const payload = {
      bookId: Number(values.bookId),
      wordId: emptyToNull(values.wordId),
      priorityRank: Number(values.priorityRank),
      word: cleanText(values.word),
      phonetic: emptyToNull(values.phonetic),
      partOfSpeech: emptyToNull(values.partOfSpeech),
      chineseMeaning: cleanText(values.chineseMeaning),
      koreanMeaning: cleanText(values.koreanMeaning),
      koreanEquivalents: emptyToNull(values.koreanEquivalents),
      koreanDefinition: emptyToNull(values.koreanDefinition),
      koreanSourceFlag: emptyToNull(values.koreanSourceFlag),
      englishExample: emptyToNull(values.englishExample),
      koreanExample: emptyToNull(values.koreanExample),
      learningStage: emptyToNull(values.learningStage),
      selectionBasis: emptyToNull(values.selectionBasis),
      sourceName: emptyToNull(values.sourceName),
      sourceUrl: emptyToNull(values.sourceUrl)
    };

    state.wordEditorBusy = true;
    [...form.elements].forEach((field) => { field.disabled = true; });
    setInlineState(elements.wordEditorStatus, id ? "正在保存修改…" : "正在新增单词…", "loading");
    try {
      await apiRequest(id ? `/admin/words/${encodeURIComponent(id)}` : "/admin/words", {
        method: id ? "PUT" : "POST",
        body: payload
      });
      showToast(id ? "单词修改已保存。" : "新单词已加入词库。");
      state.wordEditorBusy = false;
      [...form.elements].forEach((field) => { field.disabled = false; });
      closeWordEditor();
      await loadWords(id ? state.words.page : 0);
    } catch (error) {
      setInlineState(elements.wordEditorStatus, readError(error, "单词保存失败。"), "error");
    } finally {
      state.wordEditorBusy = false;
      [...form.elements].forEach((field) => { field.disabled = false; });
    }
  }

  function openConfirm({ title, message, action }) {
    if (state.confirmBusy) return;
    state.confirmAction = action;
    elements.confirmTitle.textContent = title;
    elements.confirmMessage.textContent = message;
    elements.confirmDelete.textContent = "确认删除";
    elements.confirmDelete.disabled = false;
    elements.cancelConfirm.disabled = false;
    elements.confirmModal.hidden = false;
    elements.shell.setAttribute("inert", "");
    document.body.classList.add("has-modal");
  }

  function closeConfirm() {
    if (state.confirmBusy) return;
    elements.confirmModal.hidden = true;
    elements.shell.removeAttribute("inert");
    document.body.classList.remove("has-modal");
    state.confirmAction = null;
  }

  async function runConfirmAction() {
    if (state.confirmBusy || typeof state.confirmAction !== "function") return;
    state.confirmBusy = true;
    elements.confirmDelete.disabled = true;
    elements.cancelConfirm.disabled = true;
    elements.confirmDelete.textContent = "正在删除…";
    try {
      await state.confirmAction();
      state.confirmBusy = false;
      closeConfirm();
    } catch (error) {
      elements.confirmMessage.textContent = readError(error, "删除失败，请稍后重试。");
      elements.confirmDelete.textContent = "重试删除";
      elements.confirmDelete.disabled = false;
      elements.cancelConfirm.disabled = false;
      state.confirmBusy = false;
    }
  }

  function retrySection(section) {
    if (section === "overview") loadDashboard(true);
    if (section === "users") loadUsers(state.users.page);
    if (section === "messages") loadMessages(state.messages.page);
    if (section === "books") loadBooks();
    if (section === "words") loadWords(state.words.page);
  }

  async function logout() {
    elements.logoutButtons.filter(Boolean).forEach((button) => {
      button.disabled = true;
      button.textContent = "退出中…";
    });
    try {
      await apiRequest("/auth/logout", { method: "POST", authRedirect: false });
      clearStoredCsrf();
      window.location.replace("/login.html");
    } catch (error) {
      showToast(readError(error, "退出失败，请稍后重试。"), "error");
      elements.logoutButtons.filter(Boolean).forEach((button) => {
        button.disabled = false;
        button.textContent = "退出";
      });
    }
  }

  async function apiRequest(path, options = {}) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 12000);
    const method = String(options.method || "GET").toUpperCase();
    const headers = { Accept: "application/json" };
    if (options.body !== undefined) headers["Content-Type"] = "application/json";
    if (method !== "GET" && method !== "HEAD") headers["X-CSRF-Token"] = state.csrfToken || readStoredCsrf();

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
        throw error;
      }
      return payload && Object.prototype.hasOwnProperty.call(payload, "data") ? payload.data : payload;
    } catch (error) {
      if (error.name === "AbortError") throw new Error("请求超时，请稍后重试。");
      if (error.status === 401 && options.authRedirect !== false) redirectToLogin();
      throw error;
    } finally {
      window.clearTimeout(timeout);
    }
  }

  function setInlineState(element, message, type = "") {
    if (!element) return;
    element.hidden = !message;
    element.className = `admin-inline-state${type ? ` is-${type}` : ""}`;
    element.textContent = message || "";
  }

  function setInlineError(element, message, section) {
    element.hidden = false;
    element.className = "admin-inline-state is-error";
    element.innerHTML = `${escapeHtml(message)} <button class="admin-table-action" type="button" data-retry-section="${escapeHtml(section)}">重试</button>`;
  }

  function clearInlineState(element) {
    if (!element) return;
    element.hidden = true;
    element.textContent = "";
    element.className = "admin-inline-state";
  }

  function setMessageActionState(element, message, isError = false) {
    element.textContent = message;
    element.className = `admin-message-action-state${isError ? " is-error" : ""}`;
  }

  function renderTableSkeleton(target, columns) {
    target.innerHTML = Array.from({ length: 5 }, () => `<tr class="admin-skeleton-row">${"<td></td>".repeat(columns)}</tr>`).join("");
  }

  function renderMessageSkeletons() {
    return Array.from({ length: 3 }, () => `
      <article class="admin-message-card admin-skeleton-row" aria-hidden="true">
        <div class="admin-message-author"><span></span><span></span></div>
        <div class="admin-message-content"><p>正在读取留言内容…</p></div>
      </article>`).join("");
  }

  function messageStatusOptions(selected) {
    const statuses = [["NEW", "新留言"], ["READ", "已阅读"], ["REPLIED", "已回复"], ["CLOSED", "已关闭"]];
    return statuses.map(([value, label]) => `<option value="${value}" ${value === selected ? "selected" : ""}>${label}</option>`).join("");
  }

  function normalizeMessageStatus(value) {
    const status = String(value || "NEW").toUpperCase();
    const statuses = {
      NEW: { value: "NEW", label: "新留言", css: "is-new" },
      READ: { value: "READ", label: "已阅读", css: "" },
      REPLIED: { value: "REPLIED", label: "已回复", css: "is-replied" },
      CLOSED: { value: "CLOSED", label: "已关闭", css: "" }
    };
    return statuses[status] || statuses.NEW;
  }

  function pageState() {
    return { items: [], page: 0, size: PAGE_SIZE, total: 0, totalPages: 0, keyword: "", status: "", loaded: false, loading: false };
  }

  function normalizePage(data, fallbackPage) {
    const items = Array.isArray(data) ? data : Array.isArray(data?.items) ? data.items : Array.isArray(data?.content) ? data.content : [];
    const total = numberValue(data?.total, data?.totalElements, items.length);
    const size = Math.max(1, numberValue(data?.size, PAGE_SIZE));
    return {
      items,
      page: Math.max(0, numberValue(data?.page, data?.number, fallbackPage)),
      size,
      total,
      totalPages: Math.max(0, numberValue(data?.totalPages, Math.ceil(total / size)))
    };
  }

  function listQuery(page, keyword, extras = {}) {
    const query = new URLSearchParams({ page: String(Math.max(0, page)), size: String(PAGE_SIZE) });
    if (keyword) query.set("keyword", keyword);
    Object.entries(extras).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== "") query.set(key, value);
    });
    return query.toString();
  }

  function pageAfterDelete(page) {
    return page.items.length <= 1 && page.page > 0 ? page.page - 1 : page.page;
  }

  function redirectToLogin() {
    clearStoredCsrf();
    const next = `${window.location.pathname}${window.location.hash}`;
    window.location.replace(`/login.html?next=${encodeURIComponent(next)}`);
  }

  function storeCsrf(token) {
    if (!token) return;
    try { sessionStorage.setItem(CSRF_STORAGE_KEY, token); } catch (error) { /* Token remains in memory. */ }
  }

  function readStoredCsrf() {
    try { return sessionStorage.getItem(CSRF_STORAGE_KEY) || ""; } catch (error) { return ""; }
  }

  function clearStoredCsrf() {
    state.csrfToken = "";
    try { sessionStorage.removeItem(CSRF_STORAGE_KEY); } catch (error) { /* Storage is unavailable. */ }
  }

  function toggleTheme() {
    const nextTheme = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nextTheme;
    try { localStorage.setItem("trivocab-theme", nextTheme); } catch (error) { /* Theme remains active. */ }
    syncThemeControl();
  }

  function syncThemeControl() {
    const dark = document.documentElement.dataset.theme === "dark";
    elements.themeToggle.setAttribute("aria-pressed", String(dark));
    elements.themeToggle.setAttribute("aria-label", dark ? "切换为浅色模式" : "切换为深色模式");
    elements.themeLabel.textContent = dark ? "深色" : "浅色";
    elements.themeColorMeta.setAttribute("content", dark ? "#111713" : "#edf0eb");
  }

  function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `admin-toast${type === "error" ? " is-error" : ""}`;
    toast.setAttribute("role", type === "error" ? "alert" : "status");
    toast.textContent = message;
    elements.toastRegion.append(toast);
    window.setTimeout(() => toast.remove(), 3800);
  }

  function clearInvalid(form) {
    form.querySelectorAll("[aria-invalid='true']").forEach((field) => field.removeAttribute("aria-invalid"));
  }

  function formatDateTime(value, fallback = "") {
    if (!value) return fallback;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return fallback;
    return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(date);
  }

  function formatUtcDateTime(value, fallback = "") {
    if (!value) return fallback;
    const text = String(value);
    const date = new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(text) ? text : `${text}Z`);
    if (Number.isNaN(date.getTime())) return fallback;
    return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(date);
  }

  function dateTimeAttribute(value) {
    if (!value) return "";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "" : date.toISOString();
  }

  function formatPhonetic(value) {
    const text = cleanText(value);
    if (!text) return "";
    return /^[/\[].*[/\]]$/.test(text) ? text : `/${text}/`;
  }

  function emptyToNull(value) {
    const text = cleanText(value);
    return text || null;
  }

  function cleanText(value) {
    return String(value ?? "").trim();
  }

  function asArray(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.items)) return data.items;
    if (Array.isArray(data?.content)) return data.content;
    return [];
  }

  function numberValue(...values) {
    for (const value of values) {
      if (value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value))) return Number(value);
    }
    return 0;
  }

  function formatNumber(value) {
    return new Intl.NumberFormat("zh-CN").format(numberValue(value, 0));
  }

  function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
  }

  function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }

  function readError(error, fallback) {
    return error instanceof Error && error.message ? error.message : fallback;
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, (character) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", "\"": "&quot;"
    })[character]);
  }
})();
