(() => {
  "use strict";

  const API_BASE = "/api/v1";
  const WORD_PAGE_SIZE = 20;
  const DEFAULT_DAILY_GOAL = 20;
  const MIN_DAILY_GOAL = 10;
  const MAX_DAILY_GOAL = 100;
  const DAILY_GOAL_STEP = 10;
  const RETRY_GAP_SIZE = 3;
  const CSRF_STORAGE_KEY = "trivocab-csrf-token";

  const state = {
    currentRoute: "dashboard",
    previousRoute: "dashboard",
    user: null,
    csrfToken: "",
    bookId: 1,
    selection: null,
    dashboard: null,
    dashboardReady: false,
    todayReviewed: 0,
    books: [],
    words: [],
    wordPage: 0,
    wordTotalPages: 0,
    wordTotal: 0,
    keyword: "",
    queue: [],
    queueIndex: 0,
    initialWords: [],
    completedWordKeys: new Set(),
    pendingRetryKeys: new Set(),
    sessionResults: new Map(),
    wordShownAt: 0,
    submittingReview: false,
    session: { attempts: 0, firstPass: 0 },
    refreshAfterStudy: false,
    messages: [],
    messagesLoaded: false,
    messagesLoading: false,
    statsRange: "week",
    statsData: null,
    calendarYear: 0,
    calendarMonth: 0,
    checkinDates: new Set(),
    dailyGoal: DEFAULT_DAILY_GOAL,
    dailyGoalSaving: false,
    planTotalWords: 3000,
    planLearnedWords: 0,
    currentBatchIsExtra: false
  };

  const elements = {
    views: [...document.querySelectorAll("[data-view]")],
    routeButtons: [...document.querySelectorAll("[data-route]")],
    startButtons: [...document.querySelectorAll("[data-start-study]")],
    appBootState: document.querySelector("#appBootState"),
    appBootTitle: document.querySelector("#appBootTitle"),
    appBootMessage: document.querySelector("#appBootMessage"),
    retryAppAuthButton: document.querySelector("#retryAppAuthButton"),
    appShell: document.querySelector("#appShell"),
    currentUserAvatar: document.querySelector("#currentUserAvatar"),
    currentUserDisplayName: document.querySelector("#currentUserDisplayName"),
    currentUsername: document.querySelector("#currentUsername"),
    topbarUserName: document.querySelector("#topbarUserName"),
    logoutButtons: [document.querySelector("#logoutButton"), document.querySelector("#mobileLogoutButton")],
    greeting: document.querySelector("#greeting"),
    todayLabel: document.querySelector("#todayLabel"),
    planDate: document.querySelector("#planDate"),
    refreshButton: document.querySelector("#refreshButton"),
    themeToggle: document.querySelector("#themeToggle"),
    themeToggleLabel: document.querySelector("#themeToggleLabel"),
    themeColorMeta: document.querySelector("#themeColorMeta"),
    dueCount: document.querySelector("#dueCount"),
    todayLearnedCount: document.querySelector("#todayLearnedCount"),
    masteredCount: document.querySelector("#masteredCount"),
    streakCount: document.querySelector("#streakCount"),
    currentBookName: document.querySelector("#currentBookName"),
    bookSelect: document.querySelector("#bookSelect"),
    vocabularyBookLabel: document.querySelector("#vocabularyBookLabel"),
    bookProgressLabel: document.querySelector("#bookProgressLabel"),
    bookProgressBar: document.querySelector("#bookProgressBar"),
    bookProgressTrack: document.querySelector("#bookProgressTrack"),
    learnedBookCount: document.querySelector("#learnedBookCount"),
    totalBookCount: document.querySelector("#totalBookCount"),
    legendLearned: document.querySelector("#legendLearned"),
    legendMastered: document.querySelector("#legendMastered"),
    dailyRing: document.querySelector("#dailyRing"),
    dailyProgressCount: document.querySelector("#dailyProgressCount"),
    dailyGoalCount: document.querySelector("#dailyGoalCount"),
    dailyGoalControl: document.querySelector("#dailyGoalControl"),
    dailyGoalSelect: document.querySelector("#dailyGoalSelect"),
    saveDailyGoalButton: document.querySelector("#saveDailyGoalButton"),
    dailyGoalEstimate: document.querySelector("#dailyGoalEstimate"),
    dailyGoalState: document.querySelector("#dailyGoalState"),
    planMessage: document.querySelector("#planMessage"),
    sidebarTaskText: document.querySelector("#sidebarTaskText"),
    sidebarProgress: document.querySelector("#sidebarProgress"),
    sidebarProgressTrack: document.querySelector("#sidebarProgressTrack"),
    wordSearch: document.querySelector("#wordSearch"),
    wordList: document.querySelector("#wordList"),
    wordEmpty: document.querySelector("#wordEmpty"),
    wordTotalLabel: document.querySelector("#wordTotalLabel"),
    wordPagination: document.querySelector("#wordPagination"),
    wordPageLabel: document.querySelector("#wordPageLabel"),
    wordPageInput: document.querySelector("#wordPageInput"),
    wordPrevPageButton: document.querySelector("#wordPrevPageButton"),
    wordNextPageButton: document.querySelector("#wordNextPageButton"),
    skeletonTemplate: document.querySelector("#wordSkeletonTemplate"),
    studyLoading: document.querySelector("#studyLoading"),
    studyStage: document.querySelector("#studyStage"),
    studyComplete: document.querySelector("#studyComplete"),
    studyCompleteEyebrow: document.querySelector("#studyCompleteEyebrow"),
    studyCompleteTitle: document.querySelector("#studyCompleteTitle"),
    studyCompleteDescription: document.querySelector("#studyCompleteDescription"),
    studyError: document.querySelector("#studyError"),
    studyErrorTitle: document.querySelector("#studyErrorTitle"),
    studyErrorMessage: document.querySelector("#studyErrorMessage"),
    closeStudyButton: document.querySelector("#closeStudyButton"),
    continueSessionButton: document.querySelector("#continueSessionButton"),
    flashcard: document.querySelector("#flashcard"),
    flashcardFront: document.querySelector(".flashcard-front"),
    flashcardBack: document.querySelector(".flashcard-back"),
    revealButton: document.querySelector("#revealButton"),
    ratingArea: document.querySelector("#ratingArea"),
    ratingButtons: [...document.querySelectorAll("[data-rating]")],
    studyProgressLabel: document.querySelector("#studyProgressLabel"),
    studyAttemptLabel: document.querySelector("#studyAttemptLabel"),
    studyProgressBar: document.querySelector("#studyProgressBar"),
    studyProgressTrack: document.querySelector("#studyProgressTrack"),
    studyRank: document.querySelector("#studyRank"),
    studyWord: document.querySelector("#studyWord"),
    studyPhonetic: document.querySelector("#studyPhonetic"),
    studyPartOfSpeech: document.querySelector("#studyPartOfSpeech"),
    backWord: document.querySelector("#backWord"),
    backMeta: document.querySelector("#backMeta"),
    chineseMeaning: document.querySelector("#chineseMeaning"),
    koreanMeaning: document.querySelector("#koreanMeaning"),
    englishExample: document.querySelector("#englishExample"),
    koreanExample: document.querySelector("#koreanExample"),
    sessionReviewed: document.querySelector("#sessionReviewed"),
    sessionAttempts: document.querySelector("#sessionAttempts"),
    sessionGoodRate: document.querySelector("#sessionGoodRate"),
    sessionSummary: document.querySelector("#sessionSummary"),
    sessionWordReview: document.querySelector("#sessionWordReview"),
    sessionWordList: document.querySelector("#sessionWordList"),
    messageForm: document.querySelector("#messageForm"),
    messageContent: document.querySelector("#messageContent"),
    messageCharacterCount: document.querySelector("#messageCharacterCount"),
    messageFormState: document.querySelector("#messageFormState"),
    refreshMessagesButton: document.querySelector("#refreshMessagesButton"),
    myMessagesState: document.querySelector("#myMessagesState"),
    myMessageList: document.querySelector("#myMessageList"),
    myMessagesEmpty: document.querySelector("#myMessagesEmpty"),
    meAvatar: document.querySelector("#meAvatar"),
    meDisplayName: document.querySelector("#meDisplayName"),
    meUsername: document.querySelector("#meUsername"),
    meUserId: document.querySelector("#meUserId"),
    meEmail: document.querySelector("#meEmail"),
    meRole: document.querySelector("#meRole"),
    meBookName: document.querySelector("#meBookName"),
    meLogoutButton: document.querySelector("#meLogoutButton"),
    meQuitButton: document.querySelector("#meQuitButton"),
    meTotalMinutes: document.querySelector("#meTotalMinutes"),
    meTotalLearned: document.querySelector("#meTotalLearned"),
    meStreak: document.querySelector("#meStreak"),
    meTotalCheckins: document.querySelector("#meTotalCheckins"),
    checkinButton: document.querySelector("#checkinButton"),
    calendarPrev: document.querySelector("#calendarPrev"),
    calendarNext: document.querySelector("#calendarNext"),
    calendarTitle: document.querySelector("#calendarTitle"),
    calendarGrid: document.querySelector("#calendarGrid"),
    openStatsButton: document.querySelector("#openStatsButton"),
    statsModal: document.querySelector("#statsModal"),
    closeStatsModal: document.querySelector("#closeStatsModal"),
    wordsChart: document.querySelector("#wordsChart"),
    minutesChart: document.querySelector("#minutesChart"),
    wordsChartSummary: document.querySelector("#wordsChartSummary"),
    minutesChartSummary: document.querySelector("#minutesChartSummary"),
    toastRegion: document.querySelector("#toastRegion")
  };

  document.addEventListener("DOMContentLoaded", init);

  function init() {
    renderDate();
    syncThemeControl();
    bindEvents();
    syncStudyEntryAvailability();
    authenticateAndStart();
  }

  function bindEvents() {
    elements.routeButtons.forEach((button) => {
      button.addEventListener("click", (event) => {
        event.preventDefault();
        const route = button.dataset.route;
        if (route) navigate(route);
      });
    });

    elements.startButtons.forEach((button) => {
      button.addEventListener("click", () => startStudy({
        extraBatch: state.currentBatchIsExtra && elements.studyError.contains(button)
      }));
    });
    elements.themeToggle.addEventListener("click", toggleTheme);
    elements.refreshButton.addEventListener("click", refreshCurrentView);
    elements.bookSelect.addEventListener("change", (event) => switchBook(event.target.value));
    elements.closeStudyButton.addEventListener("click", closeStudy);
    elements.continueSessionButton.addEventListener("click", () => startStudy({ extraBatch: true }));
    elements.retryAppAuthButton.addEventListener("click", authenticateAndStart);
    elements.logoutButtons.filter(Boolean).forEach((button) => button.addEventListener("click", logout));
    elements.revealButton.addEventListener("click", revealCurrentCard);
    elements.wordPrevPageButton.addEventListener("click", () => {
      if (state.wordPage > 0) loadWords(state.wordPage - 1);
    });
    elements.wordNextPageButton.addEventListener("click", () => {
      if (state.wordPage + 1 < state.wordTotalPages) loadWords(state.wordPage + 1);
    });
    elements.wordPageInput.addEventListener("change", jumpToWordPage);
    elements.wordPageInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        jumpToWordPage();
      }
    });
    elements.ratingButtons.forEach((button) => {
      button.addEventListener("click", () => submitRating(button.dataset.rating));
    });

    elements.wordSearch.addEventListener("input", debounce(() => {
      state.keyword = elements.wordSearch.value.trim();
      loadWords(0, false);
    }, 320));

    elements.messageContent.addEventListener("input", updateMessageCharacterCount);
    elements.messageForm.addEventListener("submit", submitMessage);
    elements.refreshMessagesButton.addEventListener("click", () => loadMessages(true));
    elements.meLogoutButton.addEventListener("click", logout);
    elements.meQuitButton.addEventListener("click", quitApp);
    elements.checkinButton.addEventListener("click", submitCheckin);
    elements.calendarPrev.addEventListener("click", () => shiftCalendar(-1));
    elements.calendarNext.addEventListener("click", () => shiftCalendar(1));
    elements.openStatsButton.addEventListener("click", openStatsModal);
    elements.closeStatsModal.addEventListener("click", closeStatsModal);
    elements.statsModal.addEventListener("click", (event) => {
      if (event.target === elements.statsModal) closeStatsModal();
    });
    [...document.querySelectorAll("[data-open-stats]")].forEach((card) => {
      card.addEventListener("click", openStatsModal);
    });
    [...document.querySelectorAll("[data-stats-range]")].forEach((button) => {
      button.addEventListener("click", () => {
        document.querySelectorAll("[data-stats-range]").forEach((item) => item.classList.remove("is-active"));
        button.classList.add("is-active");
        loadStatsChart(button.dataset.statsRange);
      });
    });
    elements.dailyGoalSelect.addEventListener("change", handleDailyGoalPreview);
    elements.saveDailyGoalButton.addEventListener("click", saveDailyGoal);

    window.addEventListener("hashchange", () => {
      const route = window.location.hash.replace("#", "");
      if (["dashboard", "vocabulary", "messages", "me"].includes(route)) {
        navigate(route, { updateHash: false });
      }
    });

    document.addEventListener("keydown", handleKeyboard);
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && !elements.statsModal.hidden) closeStatsModal();
    });
  }

  async function authenticateAndStart() {
    showAppBoot("正在打开学习记录", "正在确认登录状态…", false);
    try {
      const user = await apiRequest("/auth/me", { authRedirect: false });
      if (String(user?.role || "USER").toUpperCase() === "ADMIN") {
        window.location.replace("/admin.html");
        return;
      }
      state.user = user || {};
      state.csrfToken = cleanText(user?.csrfToken) || readStoredCsrf();
      storeCsrf(state.csrfToken);
      renderCurrentUser();
      elements.appBootState.hidden = true;
      elements.appShell.removeAttribute("inert");
      document.body.classList.remove("auth-pending");
      const initialHash = window.location.hash.replace("#", "");
      const initialRoute = ["vocabulary", "messages", "me"].includes(initialHash) ? initialHash : "dashboard";
      navigate(initialRoute, { updateHash: false });
      loadInitialData();
    } catch (error) {
      if (error.status === 401) {
        redirectToLogin();
        return;
      }
      showAppBoot("暂时无法打开学习页", readError(error, "请检查服务状态后重试。"), true);
    }
  }

  function showAppBoot(title, message, retryVisible) {
    document.body.classList.add("auth-pending");
    elements.appShell.setAttribute("inert", "");
    elements.appBootState.hidden = false;
    setText(elements.appBootTitle, title);
    setText(elements.appBootMessage, message);
    elements.retryAppAuthButton.hidden = !retryVisible;
  }

  function renderCurrentUser() {
    const displayName = cleanText(state.user?.displayName) || cleanText(state.user?.username) || "学习者";
    setText(elements.currentUserDisplayName, displayName);
    setText(elements.currentUsername, state.user?.username ? `@${state.user.username}` : "learner");
    setText(elements.topbarUserName, displayName);
    setText(elements.currentUserAvatar, Array.from(displayName)[0] || "学");
  }

  async function loadInitialData() {
    state.dashboardReady = false;
    syncStudyEntryAvailability();
    setRefreshState(true);
    try {
      const selection = await apiRequest("/profile/book-selection");
      state.selection = selection || null;
      const selected = selection?.selectedBookId || selection?.books?.[0]?.id;
      if (selected) state.bookId = Number(selected);
    } catch (error) {
      state.selection = null;
    }

    const [dashboardResult, booksResult] = await Promise.allSettled([
      apiRequest(`/dashboard?bookId=${encodeURIComponent(state.bookId)}`),
      apiRequest("/books")
    ]);

    if (booksResult.status === "fulfilled") {
      state.books = asArray(booksResult.value);
      const firstBook = state.books[0];
      if (firstBook?.id && !state.selection) state.bookId = firstBook.id;
    }

    if (dashboardResult.status === "fulfilled") {
      state.dashboard = dashboardResult.value || {};
      if (state.dashboard.bookId) state.bookId = state.dashboard.bookId;
      state.dashboardReady = true;
      renderDashboard(state.dashboard);
    } else {
      state.dashboardReady = false;
      renderDashboard({});
      showToast(readError(dashboardResult.reason, "暂时无法读取学习进度"), "error");
    }

    if (booksResult.status === "rejected" && dashboardResult.status === "rejected") {
      showToast("请确认 Spring Boot 服务已启动，然后重试。", "error");
    }

    renderBookSelector(state.selection);
    renderMeView();
    loadMeData();
    setRefreshState(false);
    if (state.currentRoute === "vocabulary") loadWords(0, false);
  }

  function renderMeView() {
    const user = state.user || {};
    const displayName = cleanText(user.displayName) || cleanText(user.username) || "学习者";
    setText(elements.meDisplayName, displayName);
    setText(elements.meUsername, user.username ? `@${user.username}` : "@learner");
    setText(elements.meAvatar, Array.from(displayName)[0] || "学");
    setText(elements.meUserId, user.id != null ? String(user.id) : "…");
    setText(elements.meEmail, cleanText(user.email) || "未填写邮箱");
    const role = String(user.role || "USER").toUpperCase();
    setText(elements.meRole, role === "ADMIN" ? "管理员" : "普通用户");
    elements.meQuitButton.hidden = !Boolean(user.allowShutdown);
    const books = asArray(state.selection?.books || state.books);
    const book = books.find((item) => Number(item.id) === Number(state.bookId)) || books[0];
    setText(elements.meBookName, cleanText(book?.name) || state.dashboard?.bookName || "…");
  }

  async function quitApp() {
    if (!window.confirm("确定要退出 TrVocab 应用吗？退出后需要重新双击应用才能启动。")) return;
    elements.meQuitButton.disabled = true;
    elements.meQuitButton.textContent = "正在退出…";
    try {
      await apiRequest("/system/shutdown", { method: "POST" });
      showToast("应用正在退出…");
      try {
        window.close();
      } catch (ignored) {
        // 部分浏览器会拦截脚本关闭标签页，由应用侧脚本兜底关闭。
      }
    } catch (error) {
      elements.meQuitButton.disabled = false;
      elements.meQuitButton.textContent = "退出应用";
      showToast(readError(error, "退出失败，请稍后重试"), "error");
    }
  }

  function formatMinutes(totalMinutes) {
    const minutes = Math.max(0, Math.round(Number(totalMinutes) || 0));
    if (minutes < 60) return `${minutes} 分钟`;
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest > 0 ? `${hours} 小时 ${rest} 分` : `${hours} 小时`;
  }

  async function loadMeData() {
    const now = new Date();
    state.calendarYear = now.getFullYear();
    state.calendarMonth = now.getMonth() + 1;
    try {
      const [stats, checkins] = await Promise.all([
        apiRequest("/profile/stats?range=week"),
        apiRequest(`/profile/checkins?year=${state.calendarYear}&month=${state.calendarMonth}`)
      ]);
      state.statsData = stats || null;
      mergeCheckinDates(asArray(checkins?.dates));
      renderMeStats(state.statsData?.summary);
      renderCalendar();
    } catch (error) {
      showToast(readError(error, "学习统计加载失败"), "error");
    }
  }

  function renderMeStats(summary = {}) {
    setText(elements.meTotalMinutes, formatMinutes(summary.totalStudyMinutes));
    setText(elements.meTotalLearned, formatNumber(summary.totalLearnedWords));
    setText(elements.meStreak, `${formatNumber(summary.checkinStreak)} 天`);
    setText(elements.meTotalCheckins, `${formatNumber(summary.totalCheckins)} 天`);
    const checkedIn = Boolean(summary.todayCheckedIn);
    elements.checkinButton.disabled = checkedIn;
    elements.checkinButton.textContent = checkedIn ? "已签到" : "签到";
  }

  function mergeCheckinDates(dates) {
    asArray(dates).forEach((date) => {
      if (typeof date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
        state.checkinDates.add(date);
      }
    });
  }

  function dateKey(year, month, day) {
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  function renderCalendar() {
    const year = state.calendarYear;
    const month = state.calendarMonth;
    setText(elements.calendarTitle, `${year} 年 ${month} 月`);
    const firstWeekday = new Date(year, month - 1, 1).getDay();
    const daysInMonth = new Date(year, month, 0).getDate();
    const today = new Date();
    const isCurrentMonth = today.getFullYear() === year && today.getMonth() + 1 === month;
    const todayDay = isCurrentMonth ? today.getDate() : 0;

    let html = "";
    for (let index = 0; index < firstWeekday; index += 1) {
      html += "<span class=\"calendar-cell is-empty\"></span>";
    }
    for (let day = 1; day <= daysInMonth; day += 1) {
      const key = dateKey(year, month, day);
      const checked = state.checkinDates.has(key);
      const isToday = day === todayDay;
      html += `<span class="calendar-cell${checked ? " is-checked" : ""}${isToday ? " is-today" : ""}">${day}${checked ? "<i aria-label=\"已签到\"></i>" : ""}</span>`;
    }
    elements.calendarGrid.innerHTML = html;
    elements.calendarPrev.disabled = false;
    elements.calendarNext.disabled = isCurrentMonth;
  }

  async function shiftCalendar(delta) {
    let year = state.calendarYear;
    let month = state.calendarMonth + delta;
    if (month < 1) {
      month = 12;
      year -= 1;
    } else if (month > 12) {
      month = 1;
      year += 1;
    }
    const today = new Date();
    if (year > today.getFullYear() || (year === today.getFullYear() && month > today.getMonth() + 1)) {
      return;
    }
    state.calendarYear = year;
    state.calendarMonth = month;
    try {
      const checkins = await apiRequest(`/profile/checkins?year=${year}&month=${month}`);
      mergeCheckinDates(asArray(checkins?.dates));
    } catch (error) {
      // 历史月份加载失败不阻塞日历切换。
    }
    renderCalendar();
  }

  async function submitCheckin() {
    if (elements.checkinButton.disabled) return;
    elements.checkinButton.disabled = true;
    elements.checkinButton.textContent = "签到中…";
    try {
      const data = await apiRequest("/profile/checkin", { method: "POST" });
      mergeCheckinDates(asArray(data?.dates));
      const summary = {
        ...(state.statsData?.summary || {}),
        checkinStreak: data?.streak,
        totalCheckins: data?.totalCheckins,
        todayCheckedIn: true
      };
      state.statsData = { ...(state.statsData || {}), summary };
      renderMeStats(summary);
      renderCalendar();
      showToast(`签到成功，已连续签到 ${formatNumber(data?.streak)} 天。`);
    } catch (error) {
      elements.checkinButton.disabled = false;
      elements.checkinButton.textContent = "签到";
      showToast(readError(error, "签到失败，请稍后重试"), "error");
    }
  }

  function openStatsModal() {
    elements.statsModal.hidden = false;
    document.body.classList.add("has-modal");
    elements.appShell.setAttribute("inert", "");
    loadStatsChart(state.statsRange);
  }

  function closeStatsModal() {
    elements.statsModal.hidden = true;
    document.body.classList.remove("has-modal");
    elements.appShell.removeAttribute("inert");
  }

  async function loadStatsChart(range) {
    state.statsRange = range;
    elements.wordsChart.innerHTML = "<p class=\"stats-chart-empty\">加载中…</p>";
    elements.minutesChart.innerHTML = "<p class=\"stats-chart-empty\">加载中…</p>";
    try {
      const data = await apiRequest(`/profile/stats?range=${encodeURIComponent(range)}`);
      state.statsData = data || state.statsData;
      const days = asArray(data?.days);
      renderWordsChart(days);
      renderMinutesChart(days);
    } catch (error) {
      elements.wordsChart.innerHTML = `<p class="stats-chart-empty">${escapeHtml(readError(error, "统计加载失败"))}</p>`;
    }
  }

  function chartDayLabel(date, range) {
    if (!date) return "";
    const parts = String(date).split("-");
    const month = Number(parts[1] || 0);
    const day = Number(parts[2] || 0);
    return range === "month" ? String(day) : `${month}/${day}`;
  }

  function renderWordsChart(days) {
    const max = Math.max(1, ...days.map((day) => Math.max(day.learnedWords || 0, day.reviewedWords || 0)));
    const totalLearned = days.reduce((sum, day) => sum + (day.learnedWords || 0), 0);
    const totalReviewed = days.reduce((sum, day) => sum + (day.reviewedWords || 0), 0);
    setText(elements.wordsChartSummary, `学习 ${formatNumber(totalLearned)} 词 · 复习 ${formatNumber(totalReviewed)} 词`);
    if (!days.length) {
      elements.wordsChart.innerHTML = "<p class=\"stats-chart-empty\">暂无学习记录</p>";
      return;
    }
    elements.wordsChart.innerHTML = days.map((day) => {
      const learned = day.learnedWords || 0;
      const reviewed = day.reviewedWords || 0;
      const learnedHeight = learned > 0 ? Math.max(5, Math.round((learned / max) * 100)) : 0;
      const reviewedHeight = reviewed > 0 ? Math.max(5, Math.round((reviewed / max) * 100)) : 0;
      return `
        <div class="chart-day">
          <div class="chart-bars">
            <span class="chart-bar bar-learned" style="height:${learnedHeight}%" title="学习 ${learned}"></span>
            <span class="chart-bar bar-reviewed" style="height:${reviewedHeight}%" title="复习 ${reviewed}"></span>
          </div>
          <small>${escapeHtml(chartDayLabel(day.date, state.statsRange))}</small>
        </div>`;
    }).join("");
  }

  function renderMinutesChart(days) {
    const max = Math.max(1, ...days.map((day) => day.studyMinutes || 0));
    const total = days.reduce((sum, day) => sum + (day.studyMinutes || 0), 0);
    setText(elements.minutesChartSummary, `共 ${formatNumber(total)} 分钟`);
    if (!days.length) {
      elements.minutesChart.innerHTML = "<p class=\"stats-chart-empty\">暂无学习记录</p>";
      return;
    }
    elements.minutesChart.innerHTML = days.map((day) => {
      const minutes = day.studyMinutes || 0;
      const height = minutes > 0 ? Math.max(5, Math.round((minutes / max) * 100)) : 0;
      return `
        <div class="chart-day">
          <div class="chart-bars">
            <span class="chart-bar bar-minutes${minutes > 0 ? "" : " is-zero"}" style="height:${height}%" title="${minutes} 分钟"></span>
          </div>
          <small>${escapeHtml(chartDayLabel(day.date, state.statsRange))}</small>
        </div>`;
    }).join("");
  }

  function renderBookSelector(selection) {
    const books = asArray(selection?.books || state.books);
    if (!books.length) {
      elements.bookSelect.innerHTML = "<option value=\"\">暂无词书</option>";
      return;
    }
    const selectedId = Number(selection?.selectedBookId || state.bookId);
    elements.bookSelect.innerHTML = books.map((book) => {
      const id = Number(book.id);
      const parts = [
        cleanText(book.name) || `词书 ${id}`,
        `${formatNumber(numberValue(book.totalWords, 0))} 词`,
        numberValue(book.dailyGoal, 0) > 0 ? `每日 ${book.dailyGoal} 词` : ""
      ].filter(Boolean);
      return `<option value="${id}"${id === selectedId ? " selected" : ""}>${escapeHtml(parts.join(" · "))}</option>`;
    }).join("");
    elements.bookSelect.disabled = books.length <= 1;
  }

  async function switchBook(rawBookId) {
    const bookId = Number(rawBookId);
    if (!bookId) {
      renderBookSelector(state.selection);
      return;
    }
    if (bookId === Number(state.bookId)) {
      renderBookSelector(state.selection);
      return;
    }
    elements.bookSelect.disabled = true;
    try {
      const selection = await apiRequest("/profile/book-selection", {
        method: "PUT",
        body: { bookId }
      });
      state.selection = selection || null;
      state.bookId = Number(selection?.selectedBookId || bookId);
      state.words = [];
      renderBookSelector(state.selection);
      await refreshDashboardSilently();
      if (state.currentRoute === "vocabulary") await loadWords(0, false);
      const item = asArray(selection?.books).find((book) => Number(book.id) === state.bookId);
      if (item) {
        const completion = numberValue(item.estimatedDays, 0) > 0
          ? `约需 ${formatNumber(item.estimatedDays)} 天完成`
          : "该词书已完成";
        showToast(`已切换到《${cleanText(item.name)}》，${completion}。`);
      }
    } catch (error) {
      showToast(readError(error, "词书切换失败"), "error");
      renderBookSelector(state.selection);
    } finally {
      elements.bookSelect.disabled = asArray(state.books).length <= 1;
    }
  }

  async function refreshCurrentView() {
    if (state.currentRoute === "study") return;
    if (state.currentRoute === "messages") {
      await loadMessages(true);
      return;
    }
    state.dashboardReady = false;
    syncStudyEntryAvailability();
    setRefreshState(true);
    try {
      const dashboard = await apiRequest(`/dashboard?bookId=${encodeURIComponent(state.bookId)}`);
      state.dashboard = dashboard || {};
      if (dashboard?.bookId) state.bookId = dashboard.bookId;
      state.dashboardReady = true;
      renderDashboard(state.dashboard);
      renderMeView();
      if (state.currentRoute === "vocabulary") await loadWords(0, false);
      showToast("学习数据已更新");
    } catch (error) {
      state.dashboardReady = false;
      syncStudyEntryAvailability();
      showToast(readError(error, "刷新失败，请稍后重试"), "error");
    } finally {
      setRefreshState(false);
    }
  }

  function renderDashboard(data) {
    const book = state.books.find((item) => Number(item.id) === Number(data.bookId)) || state.books[0] || {};
    const totalWords = numberValue(data.totalWords, book.totalWords, 3000);
    const learnedWords = numberValue(data.learnedWords, book.learnedWords, 0);
    const masteredWords = numberValue(data.masteredWords, 0);
    const dueWords = numberValue(data.dueWords, data.dueCount, 0);
    const todayReviewed = numberValue(data.todayReviewed, data.todayLearned, 0);
    const dailyGoal = normalizeDailyGoal(data.dailyGoal, state.dailyGoal);
    const streakDays = numberValue(data.streakDays, data.streak, 0);
    const calculatedProgress = totalWords > 0 ? (learnedWords / totalWords) * 100 : 0;
    const progress = clamp(numberValue(data.progressPercent, book.progressPercent, calculatedProgress), 0, 100);
    const dailyProgress = clamp((todayReviewed / dailyGoal) * 100, 0, 100);

    state.dailyGoal = dailyGoal;
    state.todayReviewed = Math.max(0, todayReviewed);
    state.planTotalWords = Math.max(0, totalWords);
    state.planLearnedWords = clamp(learnedWords, 0, state.planTotalWords);
    syncDailyGoalSelect(dailyGoal);
    renderDailyGoalEstimate(dailyGoal, {
      remainingWords: data.remainingWords,
      estimatedDays: data.estimatedDays,
      estimatedCompletionDate: data.estimatedCompletionDate
    });

    setText(elements.dueCount, formatNumber(dueWords));
    setText(elements.todayLearnedCount, formatNumber(todayReviewed));
    setText(elements.masteredCount, formatNumber(masteredWords));
    setText(elements.streakCount, formatNumber(streakDays));
    setText(elements.currentBookName, data.bookName || book.name || "雅思核心词汇");
    setText(elements.vocabularyBookLabel, `当前词书：${data.bookName || book.name || "雅思核心词汇"}`);
    setText(elements.bookProgressLabel, `${formatPercent(progress)}%`);
    elements.bookProgressBar.style.width = `${progress}%`;
    updateProgressAccessibility(elements.bookProgressTrack, progress, `${formatPercent(progress)}%`);
    setText(elements.learnedBookCount, formatNumber(learnedWords));
    setText(elements.totalBookCount, formatNumber(totalWords));
    setText(elements.legendLearned, formatNumber(learnedWords));
    setText(elements.legendMastered, formatNumber(masteredWords));
    elements.dailyRing.style.setProperty("--progress", String(dailyProgress));
    updateProgressAccessibility(elements.dailyRing, dailyProgress, `${todayReviewed} / ${dailyGoal} 个词`);
    setText(elements.dailyProgressCount, formatNumber(todayReviewed));
    setText(elements.dailyGoalCount, formatNumber(dailyGoal));
    setText(elements.sidebarTaskText, `${todayReviewed} / ${dailyGoal} 个词已完成`);
    elements.sidebarProgress.style.width = `${dailyProgress}%`;
    updateProgressAccessibility(elements.sidebarProgressTrack, dailyProgress, `${todayReviewed} / ${dailyGoal} 个词`);
    syncStudyEntryAvailability();

    if (todayReviewed >= dailyGoal) {
      setText(elements.planMessage, "今日计划已完成。如果还有余力，可以再巩固一组。");
    } else if (dueWords > 0) {
      setText(elements.planMessage, `还有 ${dueWords} 个待复习词，先把它们稳稳地接回来。`);
    } else {
      setText(elements.planMessage, "从第一个词开始，今天的进步会很具体。");
    }
  }

  function handleDailyGoalPreview() {
    clearDailyGoalState();
    renderDailyGoalEstimate(normalizeDailyGoal(elements.dailyGoalSelect.value, state.dailyGoal));
  }

  function syncStudyEntryAvailability() {
    const normalBatchAvailable = state.dashboardReady && state.todayReviewed < state.dailyGoal;
    elements.startButtons.forEach((button) => {
      const retriesFailedExtraBatch = state.dashboardReady
        && state.currentBatchIsExtra
        && !elements.studyError.hidden
        && elements.studyError.contains(button);
      const available = normalBatchAvailable || retriesFailedExtraBatch;
      button.disabled = !available;
      button.setAttribute("aria-disabled", String(!available));
      if (!state.dashboardReady) {
        button.title = "正在读取今日学习进度";
      } else if (!available) {
        button.title = "今日学习目标已完成";
      } else {
        button.removeAttribute("title");
      }
    });
  }

  async function saveDailyGoal() {
    if (state.dailyGoalSaving) return;
    const dailyGoal = normalizeDailyGoal(elements.dailyGoalSelect.value, state.dailyGoal);
    state.dailyGoalSaving = true;
    elements.dailyGoalControl.setAttribute("aria-busy", "true");
    elements.dailyGoalSelect.disabled = true;
    elements.saveDailyGoalButton.disabled = true;
    elements.saveDailyGoalButton.textContent = "保存中…";
    setDailyGoalState("正在保存每日目标…", "loading");

    try {
      const profileQuery = new URLSearchParams({ bookId: String(state.bookId) });
      const response = await apiRequest(`/profile/daily-goal?${profileQuery}`, {
        method: "PATCH",
        body: { dailyGoal }
      });
      const savedGoal = normalizeDailyGoal(response?.dailyGoal, dailyGoal);
      state.dailyGoal = savedGoal;
      state.dashboard = {
        ...(state.dashboard || {}),
        dailyGoal: savedGoal,
        remainingWords: response?.remainingWords,
        estimatedDays: response?.estimatedDays,
        estimatedCompletionDate: response?.estimatedCompletionDate
      };
      renderDashboard(state.dashboard);
      setDailyGoalState(`每日目标已更新为 ${savedGoal} 个词。`, "success");
      refreshBookSelectionSilently();
    } catch (error) {
      setDailyGoalState(readError(error, "每日目标保存失败，请稍后重试。"), "error");
    } finally {
      state.dailyGoalSaving = false;
      elements.dailyGoalControl.removeAttribute("aria-busy");
      elements.dailyGoalSelect.disabled = false;
      elements.saveDailyGoalButton.disabled = false;
      elements.saveDailyGoalButton.textContent = "保存";
    }
  }

  function syncDailyGoalSelect(dailyGoal) {
    elements.dailyGoalSelect.value = String(dailyGoal);
  }

  function renderDailyGoalEstimate(dailyGoal, estimate = {}) {
    const localRemainingWords = Math.max(0, state.planTotalWords - state.planLearnedWords);
    const remainingCandidate = optionalNumber(estimate.remainingWords);
    const remainingWords = remainingCandidate === null ? localRemainingWords : Math.max(0, remainingCandidate);
    if (remainingWords === 0) {
      setText(elements.dailyGoalEstimate, "这本词书已经全部完成。");
      return;
    }

    const estimatedDays = optionalNumber(estimate.estimatedDays);
    const hasServerEstimate = estimatedDays !== null && estimatedDays > 0;
    const daysNeeded = hasServerEstimate
      ? Math.max(1, Math.ceil(estimatedDays))
      : Math.max(1, Math.ceil(remainingWords / dailyGoal));
    const completionDate = (hasServerEstimate && firstValidDate(estimate.estimatedCompletionDate))
      || dateAfterDays(daysNeeded - 1);
    setText(
      elements.dailyGoalEstimate,
      `理想情况下，仅按新词数量估算：剩余 ${formatNumber(remainingWords)} 个词，按每日 ${dailyGoal} 个词约需 ${formatNumber(daysNeeded)} 天，预计 ${formatCompletionDate(completionDate)} 完成。`
    );
  }

  function setDailyGoalState(message, type = "") {
    elements.dailyGoalState.hidden = !message;
    elements.dailyGoalState.className = `daily-goal-state${type ? ` is-${type}` : ""}`;
    elements.dailyGoalState.textContent = message || "";
  }

  function clearDailyGoalState() {
    setDailyGoalState("");
  }

  function normalizeDailyGoal(value, fallback = DEFAULT_DAILY_GOAL) {
    if (value === null || value === undefined || value === "") {
      return normalizeDailyGoal(fallback, DEFAULT_DAILY_GOAL);
    }
    const numericValue = Number(value);
    if (!Number.isFinite(numericValue)) return normalizeDailyGoal(fallback, DEFAULT_DAILY_GOAL);
    const steppedValue = Math.round(numericValue / DAILY_GOAL_STEP) * DAILY_GOAL_STEP;
    return clamp(steppedValue, MIN_DAILY_GOAL, MAX_DAILY_GOAL);
  }

  function formatCompletionDate(date) {
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "long",
      day: "numeric"
    }).format(date);
  }

  async function loadWords(page = 0, append = false) {
    const expectedKeyword = state.keyword;
    if (!append) showWordSkeletons();
    setWordPaginationDisabled(true);

    try {
      const query = new URLSearchParams({
        page: String(page),
        size: String(WORD_PAGE_SIZE),
        keyword: expectedKeyword
      });
      const data = await apiRequest(`/books/${encodeURIComponent(state.bookId)}/words?${query}`);

      if (expectedKeyword !== state.keyword) return;
      const items = asArray(data);
      state.words = items;
      state.wordPage = numberValue(data?.page, page);
      state.wordTotalPages = numberValue(data?.totalPages, items.length < WORD_PAGE_SIZE ? page + 1 : page + 2);
      state.wordTotal = numberValue(data?.total, state.words.length);
      renderWords();
    } catch (error) {
      if (!append) state.words = [];
      renderWords();
      showToast(readError(error, "词表加载失败"), "error");
    } finally {
      setWordPaginationDisabled(false);
    }
  }

  function renderWords() {
    elements.wordList.innerHTML = state.words.map((word) => {
      const status = normalizeStatus(word.progressStatus || word.status);
      const phonetic = formatPhonetic(word.phonetic);
      const meta = [phonetic, word.partOfSpeech].filter(Boolean).join(" · ");
      const koreanMeaning = preferredKoreanMeaning(word);
      return `
        <article class="word-row">
          <div class="word-cell-main">
            <strong lang="en">${escapeHtml(word.word || "…")}</strong>
            <span>${escapeHtml(meta || `IELTS 核心词 · #${word.priorityRank || word.id || "…"}`)}</span>
          </div>
          <div class="meaning-cell" title="${escapeHtml(word.chineseMeaning || "")}">${escapeHtml(word.chineseMeaning || "中文释义待补充")}</div>
          <div class="meaning-cell" lang="ko" title="${escapeHtml(koreanMeaning)}">${escapeHtml(koreanMeaning || "한국어 뜻 준비 중")}</div>
          <span class="status-pill status-${status.css}">${status.label}</span>
        </article>`;
    }).join("");

    const hasWords = state.words.length > 0;
    elements.wordEmpty.hidden = hasWords;
    elements.wordList.setAttribute("aria-busy", "false");
    elements.wordTotalLabel.textContent = formatNumber(state.wordTotal);
    const totalPages = Math.max(1, state.wordTotalPages);
    const showPagination = hasWords && totalPages > 1;
    elements.wordPagination.hidden = !showPagination;
    if (showPagination) {
      setText(
        elements.wordPageLabel,
        `第 ${formatNumber(state.wordPage + 1)} / ${formatNumber(totalPages)} 页，共 ${formatNumber(state.wordTotal)} 词`
      );
      elements.wordPageInput.value = String(state.wordPage + 1);
      elements.wordPageInput.max = String(totalPages);
      elements.wordPrevPageButton.disabled = state.wordPage <= 0;
      elements.wordNextPageButton.disabled = state.wordPage + 1 >= totalPages;
    }
  }

  function jumpToWordPage() {
    const totalPages = Math.max(1, state.wordTotalPages);
    const requested = Math.floor(Number(elements.wordPageInput.value) || 0);
    const page = clamp(requested - 1, 0, totalPages - 1);
    elements.wordPageInput.value = String(page + 1);
    if (page !== state.wordPage) loadWords(page);
  }

  function setWordPaginationDisabled(disabled) {
    elements.wordPrevPageButton.disabled = disabled;
    elements.wordNextPageButton.disabled = disabled;
    elements.wordPageInput.disabled = disabled;
  }

  function showWordSkeletons() {
    elements.wordEmpty.hidden = true;
    elements.wordPagination.hidden = true;
    elements.wordList.setAttribute("aria-busy", "true");
    elements.wordList.innerHTML = "";
    for (let index = 0; index < 7; index += 1) {
      elements.wordList.append(elements.skeletonTemplate.content.cloneNode(true));
    }
  }

  function updateMessageCharacterCount() {
    const length = Array.from(elements.messageContent.value).length;
    setText(elements.messageCharacterCount, `${length} / 1000`);
    elements.messageCharacterCount.classList.toggle("is-limit", length > 900);
    elements.messageContent.removeAttribute("aria-invalid");
    if (!elements.messageFormState.classList.contains("is-loading")) clearMessageState(elements.messageFormState);
  }

  async function submitMessage(event) {
    event.preventDefault();
    const content = elements.messageContent.value.trim();
    const length = Array.from(content).length;
    if (length < 10 || length > 1000) {
      elements.messageContent.setAttribute("aria-invalid", "true");
      setMessageState(elements.messageFormState, "请输入 10-1000 个字符的留言内容。", "error");
      return;
    }

    setMessageFormDisabled(true);
    setMessageState(elements.messageFormState, "正在提交留言…", "loading");
    try {
      await apiRequest("/messages", { method: "POST", body: { content } });
      elements.messageForm.reset();
      updateMessageCharacterCount();
      setMessageState(elements.messageFormState, "留言已提交，管理员处理后会在这里回复。", "success");
      await loadMessages(true);
    } catch (error) {
      setMessageState(elements.messageFormState, readError(error, "留言提交失败，请稍后重试。"), "error");
    } finally {
      setMessageFormDisabled(false);
    }
  }

  async function loadMessages(force = false) {
    if (state.messagesLoading || (state.messagesLoaded && !force)) return;
    state.messagesLoading = true;
    elements.refreshMessagesButton.disabled = true;
    elements.myMessageList.setAttribute("aria-busy", "true");
    elements.myMessagesEmpty.hidden = true;
    elements.myMessageList.innerHTML = Array.from({ length: 3 }, () => '<article class="my-message-card my-message-skeleton" aria-hidden="true"></article>').join("");
    setMessageState(elements.myMessagesState, "正在读取我的留言…", "loading");
    try {
      const data = await apiRequest("/messages");
      state.messages = asArray(data);
      state.messagesLoaded = true;
      renderMyMessages();
      clearMessageState(elements.myMessagesState);
    } catch (error) {
      elements.myMessageList.innerHTML = "";
      setMessageState(elements.myMessagesState, readError(error, "留言列表加载失败，可点击刷新重试。"), "error");
    } finally {
      state.messagesLoading = false;
      elements.refreshMessagesButton.disabled = false;
      elements.myMessageList.setAttribute("aria-busy", "false");
    }
  }

  function renderMyMessages() {
    elements.myMessageList.innerHTML = state.messages.map((message) => {
      const status = messageStatusMeta(message.status);
      const reply = cleanText(message.adminReply || message.reply);
      return `
        <article class="my-message-card">
          <div class="my-message-meta">
            <span class="message-status ${status.css}">${status.label}</span>
            <time datetime="${escapeHtml(dateTimeAttribute(message.createdAt))}">${escapeHtml(formatMessageDate(message.createdAt))}</time>
          </div>
          <p class="my-message-content">${escapeHtml(message.content || "留言内容为空")}</p>
          ${reply ? `<div class="my-message-reply"><strong>管理员回复</strong>${escapeHtml(reply)}</div>` : ""}
        </article>`;
    }).join("");
    elements.myMessagesEmpty.hidden = state.messages.length > 0;
  }

  function messageStatusMeta(value) {
    const status = String(value || "NEW").toUpperCase();
    const statuses = {
      NEW: { label: "待处理", css: "is-new" },
      READ: { label: "已阅读", css: "" },
      REPLIED: { label: "已回复", css: "is-replied" },
      CLOSED: { label: "已关闭", css: "" }
    };
    return statuses[status] || statuses.NEW;
  }

  function setMessageFormDisabled(disabled) {
    [...elements.messageForm.elements].forEach((field) => { field.disabled = disabled; });
  }

  function setMessageState(element, message, type = "") {
    element.hidden = !message;
    const baseClass = element === elements.messageFormState ? "message-form-state" : "my-message-state";
    element.className = `${baseClass}${type ? ` is-${type}` : ""}`;
    element.textContent = message || "";
  }

  function clearMessageState(element) {
    setMessageState(element, "");
  }

  function formatMessageDate(value) {
    if (!value) return "时间未知";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "时间未知";
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
    }).format(date);
  }

  function dateTimeAttribute(value) {
    if (!value) return "";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "" : date.toISOString();
  }

  async function startStudy({ extraBatch = false } = {}) {
    if (!state.dashboardReady) {
      showToast("正在读取今日进度，请稍候。", "error");
      return;
    }

    const remainingGoal = Math.max(0, state.dailyGoal - state.todayReviewed);
    if (!extraBatch && remainingGoal === 0) {
      syncStudyEntryAvailability();
      showToast("今日学习目标已完成。");
      return;
    }

    const queueLimit = extraBatch ? state.dailyGoal : remainingGoal;
    state.currentBatchIsExtra = extraBatch;
    if (state.currentRoute !== "study") state.previousRoute = state.currentRoute;
    navigate("study", { updateHash: false });
    state.queue = [];
    state.queueIndex = 0;
    state.initialWords = [];
    state.completedWordKeys = new Set();
    state.pendingRetryKeys = new Set();
    state.sessionResults = new Map();
    state.session = { attempts: 0, firstPass: 0 };
    state.refreshAfterStudy = false;
    elements.studyLoading.hidden = false;
    elements.studyStage.hidden = true;
    elements.studyComplete.hidden = true;
    elements.studyError.hidden = true;
    if (elements.sessionWordList) elements.sessionWordList.innerHTML = "";
    updateStudyProgress();

    try {
      const query = new URLSearchParams({ bookId: String(state.bookId), limit: String(queueLimit) });
      const data = await apiRequest(`/study/queue?${query}`);
      state.initialWords = uniqueSessionWords(asArray(data));
      state.queue = state.initialWords.map((word) => createQueueItem(word));
      state.initialWords.forEach((word) => {
        const key = sessionWordKey(word);
        state.sessionResults.set(key, {
          word,
          attempts: 0,
          finalRating: null,
          review: null
        });
      });
      elements.studyLoading.hidden = true;
      if (state.queue.length === 0) {
        finishSession({ empty: true });
        return;
      }
      elements.studyStage.hidden = false;
      showCurrentWord();
    } catch (error) {
      elements.studyLoading.hidden = true;
      showStudyError(readError(error, "学习队列没有加载成功，请稍后重试。"));
    }
  }

  function showCurrentWord() {
    const word = state.queue[state.queueIndex];
    if (!word) {
      finishSession();
      return;
    }

    if (word.__isRetry) state.pendingRetryKeys.delete(sessionWordKey(word));
    animateCardSwitch(() => renderWordContent(word));
  }

  function renderWordContent(word) {
    elements.flashcard.classList.remove("is-revealed");
    elements.flashcardBack.scrollTop = 0;
    elements.ratingArea.classList.remove("is-visible");
    elements.ratingArea.setAttribute("aria-hidden", "true");
    elements.ratingArea.setAttribute("inert", "");
    toggleCardAccessibility(false);
    setRatingDisabled(false);

    const phonetic = formatPhonetic(word.phonetic);
    const partOfSpeech = word.partOfSpeech || "";
    const koreanMeaning = preferredKoreanMeaning(word);
    const rankPrefix = word.__isRetry ? "本组再巩固" : "IELTS CORE";
    const rank = word.priorityRank ? `${rankPrefix} · #${word.priorityRank}` : rankPrefix;

    setText(elements.studyRank, rank);
    setText(elements.studyWord, word.word || "…");
    setText(elements.studyPhonetic, phonetic);
    setText(elements.studyPartOfSpeech, partOfSpeech);
    elements.studyPhonetic.hidden = !phonetic;
    elements.studyPartOfSpeech.hidden = !partOfSpeech;
    setText(elements.backWord, word.word || "…");
    setText(elements.backMeta, [phonetic, partOfSpeech].filter(Boolean).join(" · "));
    setText(elements.chineseMeaning, word.chineseMeaning || "中文释义待补充");
    setText(elements.koreanMeaning, koreanMeaning || "한국어 뜻 준비 중");
    setText(elements.englishExample, word.englishExample || "Example coming soon.");
    setText(elements.koreanExample, word.koreanExample || "예문 준비 중입니다.");

    updateStudyProgress();
    state.wordShownAt = performance.now();
  }

  async function animateCardSwitch(render) {
    if (prefersReducedMotion()) {
      render();
      return;
    }
    const card = elements.flashcard;
    card.classList.add("card-leaving");
    await wait(160);
    render();
    card.classList.remove("card-leaving");
    card.classList.add("card-entering");
    await wait(240);
    card.classList.remove("card-entering");
  }

  function revealCurrentCard() {
    if (state.currentRoute !== "study" || elements.studyStage.hidden) return;
    if (elements.flashcard.classList.contains("is-revealed")) return;
    elements.flashcard.classList.add("is-revealed");
    toggleCardAccessibility(true);
    window.setTimeout(() => {
      if (state.currentRoute !== "study" || elements.studyStage.hidden
          || !elements.flashcard.classList.contains("is-revealed")) return;
      elements.ratingArea.classList.add("is-visible");
      elements.ratingArea.setAttribute("aria-hidden", "false");
      elements.ratingArea.removeAttribute("inert");
      if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
    }, prefersReducedMotion() ? 0 : 260);
  }

  async function submitRating(rating) {
    if (state.submittingReview || !elements.flashcard.classList.contains("is-revealed")) return;
    const word = state.queue[state.queueIndex];
    if (!word) return;

    const normalizedRating = String(rating || "").toUpperCase();
    if (!["AGAIN", "HARD", "GOOD", "EASY"].includes(normalizedRating)) return;

    state.submittingReview = true;
    setRatingDisabled(true);
    const responseMs = Math.max(0, Math.round(performance.now() - state.wordShownAt));
    const clientReviewId = word.__clientReviewId || createReviewId();
    word.__clientReviewId = clientReviewId;

    try {
      const reviewResponse = await apiRequest("/study/reviews", {
        method: "POST",
        body: { clientReviewId, wordId: word.id ?? word.wordId, rating: normalizedRating, responseMs }
      });

      const key = sessionWordKey(word);
      const result = state.sessionResults.get(key) || {
        word,
        attempts: 0,
        finalRating: null,
        review: null
      };
      result.attempts += 1;
      result.lastRating = normalizedRating;
      result.review = reviewResponse || {};
      state.sessionResults.set(key, result);
      state.session.attempts += 1;

      if (normalizedRating === "AGAIN" || normalizedRating === "HARD") {
        result.finalRating = null;
        scheduleRetry(word, reviewResponse?.repeatAfterCards);
        showToast(normalizedRating === "AGAIN"
          ? "这个词已加入本组稍后重学"
          : "这个词已加入本组稍后巩固");
      } else {
        result.finalRating = normalizedRating;
        state.completedWordKeys.add(key);
        if (result.attempts === 1) state.session.firstPass += 1;
        removeFutureRetries(key);
      }

      state.refreshAfterStudy = true;
      state.queueIndex += 1;
      showCurrentWord();
    } catch (error) {
      setRatingDisabled(false);
      showToast(readError(error, "复习结果未能保存，请重试"), "error");
    } finally {
      state.submittingReview = false;
    }
  }

  function finishSession({ empty = false } = {}) {
    elements.studyLoading.hidden = true;
    elements.studyStage.hidden = true;
    elements.studyError.hidden = true;
    elements.studyComplete.hidden = false;
    updateStudyProgress();
    const completedCount = state.completedWordKeys.size;
    const firstPassRate = completedCount > 0
      ? Math.round((state.session.firstPass / completedCount) * 100)
      : 0;
    setText(elements.sessionReviewed, formatNumber(completedCount));
    setText(elements.sessionAttempts, formatNumber(state.session.attempts));
    setText(elements.sessionGoodRate, `${firstPassRate}%`);
    setText(
      elements.studyCompleteEyebrow,
      empty ? "学习状态" : (state.currentBatchIsExtra ? "额外学习完成" : "本组完成")
    );
    setText(
      elements.studyCompleteTitle,
      empty
        ? "当前没有待学习的单词。"
        : (state.currentBatchIsExtra ? "额外一组单词完成了。" : "今天的一组单词完成了。")
    );
    setText(
      elements.studyCompleteDescription,
      empty
        ? "今天没有到期词或未学新词，可以返回首页。"
        : (state.currentBatchIsExtra
          ? "这是一组超出今日目标的额外学习，以下是复习安排。"
          : "以下是根据本组学习结果生成的复习安排。")
    );
    elements.sessionSummary.hidden = empty;
    elements.sessionWordReview.hidden = empty;
    elements.continueSessionButton.hidden = empty;
    elements.continueSessionButton.textContent = `额外再学 ${state.dailyGoal} 个词`;
    elements.continueSessionButton.setAttribute(
      "aria-label",
      `额外再学一组，最多 ${state.dailyGoal} 个词`
    );
    if (empty) {
      elements.sessionWordList.innerHTML = "";
    } else {
      renderSessionWordList();
    }
    if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
  }

  function showStudyError(message) {
    elements.studyStage.hidden = true;
    elements.studyComplete.hidden = true;
    elements.studyError.hidden = false;
    setText(elements.studyErrorMessage, message);
    syncStudyEntryAvailability();
  }

  function closeStudy() {
    const destination = ["vocabulary", "messages"].includes(state.previousRoute) ? state.previousRoute : "dashboard";
    navigate(destination);
  }

  function updateStudyProgress() {
    const total = state.initialWords.length;
    const completedCount = Math.min(state.completedWordKeys.size, total);
    const completed = total > 0 ? (completedCount / total) * 100 : 0;
    setText(elements.studyProgressLabel, `${completedCount} / ${total}`);
    setText(elements.studyAttemptLabel, `尝试 ${state.session.attempts} 次`);
    elements.studyProgressBar.style.width = `${completed}%`;
    updateProgressAccessibility(
      elements.studyProgressTrack,
      completed,
      `已结束 ${completedCount} / ${total} 个唯一单词，共尝试 ${state.session.attempts} 次`
    );
  }

  function uniqueSessionWords(words) {
    const seen = new Set();
    return words.reduce((unique, word, index) => {
      const key = sessionWordKey(word, index);
      if (seen.has(key)) return unique;
      seen.add(key);
      unique.push({ ...word, __sessionKey: key });
      return unique;
    }, []);
  }

  function createQueueItem(word, isRetry = false) {
    return {
      ...word,
      __sessionKey: sessionWordKey(word),
      __isRetry: isRetry,
      __clientReviewId: null
    };
  }

  function sessionWordKey(word = {}, fallbackIndex = 0) {
    if (word.__sessionKey) return word.__sessionKey;
    const id = word.id ?? word.wordId;
    if (id !== null && id !== undefined && id !== "") return `id:${id}`;
    const spelling = cleanText(word.word).toLocaleLowerCase();
    if (spelling) return `word:${spelling}`;
    return `word-at:${fallbackIndex}`;
  }

  function scheduleRetry(word, requestedGap) {
    const key = sessionWordKey(word);
    const alreadyQueued = state.pendingRetryKeys.has(key)
      || state.queue.some((item, index) => index > state.queueIndex && sessionWordKey(item) === key);
    if (alreadyQueued) return;

    const firstRemainingIndex = state.queueIndex + 1;
    const remainingCount = Math.max(0, state.queue.length - firstRemainingIndex);
    const responseGap = optionalNumber(requestedGap);
    const retryGap = responseGap === null ? RETRY_GAP_SIZE : clamp(Math.round(responseGap), 0, state.dailyGoal);
    const insertionIndex = firstRemainingIndex + Math.min(retryGap, remainingCount);
    state.queue.splice(insertionIndex, 0, createQueueItem(word, true));
    state.pendingRetryKeys.add(key);
  }

  function removeFutureRetries(key) {
    for (let index = state.queue.length - 1; index > state.queueIndex; index -= 1) {
      if (sessionWordKey(state.queue[index]) === key) state.queue.splice(index, 1);
    }
    state.pendingRetryKeys.delete(key);
  }

  function renderSessionWordList() {
    if (!elements.sessionWordList) return;
    const ratingOrder = { GOOD: 0, EASY: 1 };
    const sortedWords = state.initialWords
      .map((word, originalIndex) => ({
        word,
        originalIndex,
        result: state.sessionResults.get(sessionWordKey(word)) || {}
      }))
      .sort((left, right) => {
        const leftRating = String(left.result.finalRating || left.result.lastRating || "").toUpperCase();
        const rightRating = String(right.result.finalRating || right.result.lastRating || "").toUpperCase();
        const ratingDifference = (ratingOrder[leftRating] ?? 2) - (ratingOrder[rightRating] ?? 2);
        return ratingDifference || left.originalIndex - right.originalIndex;
      });

    elements.sessionWordList.innerHTML = sortedWords.map(({ word, result }) => {
      const schedule = sessionReviewSchedule(word, result);
      return `
        <li class="session-word-item">
          <div class="session-word-copy">
            <strong lang="en">${escapeHtml(word.word || "…")}</strong>
          </div>
          <div class="review-schedule${schedule.noReview ? " is-mastered" : ""}">
            <strong>${escapeHtml(schedule.label)}</strong>
          </div>
        </li>`;
    }).join("");
  }

  function sessionReviewSchedule(word, result = {}) {
    if (String(result.finalRating || "").toUpperCase() === "EASY") {
      return {
        label: "无需复习",
        detail: "不再安排复习",
        date: null,
        noReview: true
      };
    }

    const review = result.review || {};
    const intervalCandidate = optionalNumber(
      review.intervalDays,
      review.reviewIntervalDays,
      review.interval,
      word.intervalDays
    );
    const parsedNextReviewDate = firstValidDate(
      review.nextReviewAt,
      review.nextReviewTime,
      review.nextReviewDate,
      word.nextReviewAt
    );
    const nextReviewDate = parsedNextReviewDate && parsedNextReviewDate.getTime() > Date.now()
      ? parsedNextReviewDate
      : null;
    let intervalDays = intervalCandidate !== null && intervalCandidate > 0
      ? Math.max(1, Math.round(intervalCandidate))
      : null;

    if (intervalDays === null && nextReviewDate) {
      const millisecondsPerDay = 24 * 60 * 60 * 1000;
      intervalDays = Math.max(1, Math.ceil((nextReviewDate.getTime() - Date.now()) / millisecondsPerDay));
    }
    if (intervalDays === null) intervalDays = 1;

    const date = nextReviewDate || dateAfterDays(intervalDays);
    return {
      label: `${intervalDays}天后复习`,
      dateLabel: `预计 ${formatReviewDate(date)}`,
      detail: "根据本次记忆结果安排",
      date,
      noReview: false
    };
  }

  function optionalNumber(...values) {
    for (const value of values) {
      if (value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value))) {
        return Number(value);
      }
    }
    return null;
  }

  function firstValidDate(...values) {
    for (const value of values) {
      if (!value) continue;
      const date = new Date(value);
      if (!Number.isNaN(date.getTime())) return date;
    }
    return null;
  }

  function dateAfterDays(days) {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return date;
  }

  function formatReviewDate(date) {
    return new Intl.DateTimeFormat("zh-CN", {
      month: "long",
      day: "numeric",
      weekday: "short"
    }).format(date);
  }

  function setRatingDisabled(disabled) {
    elements.ratingButtons.forEach((button) => { button.disabled = disabled; });
  }

  function navigate(route, options = {}) {
    const { updateHash = true } = options;
    if (!elements.views.some((view) => view.dataset.view === route)) return;

    if (route !== "study" && state.currentRoute === "study" && state.refreshAfterStudy) {
      state.refreshAfterStudy = false;
      refreshDashboardSilently();
    }

    state.currentRoute = route;
    document.body.dataset.route = route;
    elements.views.forEach((view) => {
      const active = view.dataset.view === route;
      view.hidden = !active;
      view.classList.toggle("is-active", active);
    });

    elements.routeButtons.forEach((button) => {
      const active = button.dataset.route === route;
      button.classList.toggle("is-active", active);
      if (active) button.setAttribute("aria-current", "page");
      else button.removeAttribute("aria-current");
    });

    if (updateHash && route !== "study") {
      history.replaceState(null, "", `#${route}`);
    }

    if (route === "vocabulary" && state.words.length === 0) loadWords(0, false);
    if (route === "messages" && !state.messagesLoaded) loadMessages();
    if (route === "me") {
      renderMeView();
      loadMeData();
    }
    window.scrollTo({ top: 0, behavior: prefersReducedMotion() ? "auto" : "smooth" });
  }

  async function refreshDashboardSilently() {
    state.dashboardReady = false;
    syncStudyEntryAvailability();
    try {
      const dashboard = await apiRequest(`/dashboard?bookId=${encodeURIComponent(state.bookId)}`);
      state.dashboard = dashboard || {};
      if (dashboard?.bookId) state.bookId = dashboard.bookId;
      state.dashboardReady = true;
      renderDashboard(state.dashboard);
      renderMeView();
      if (state.currentRoute === "vocabulary") loadWords(0, false);
    } catch (error) {
      state.dashboardReady = false;
      syncStudyEntryAvailability();
      showToast(readError(error, "学习已保存，但进度刷新失败"), "error");
    }
  }

  async function refreshBookSelectionSilently() {
    try {
      const selection = await apiRequest("/profile/book-selection");
      state.selection = selection || null;
      renderBookSelector(state.selection);
    } catch (error) {
      // 静默失败：下次切换词书或刷新时会重新同步。
    }
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
        button.textContent = button.id === "logoutButton" ? "退出登录" : "退出";
      });
    }
  }

  function handleKeyboard(event) {
    const target = event.target;
    const typing = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target?.isContentEditable;
    const interactive = target instanceof HTMLButtonElement || target instanceof HTMLAnchorElement || target instanceof HTMLSelectElement;

    if (event.key === "/" && !typing && state.currentRoute !== "study") {
      event.preventDefault();
      navigate("vocabulary");
      window.setTimeout(() => elements.wordSearch.focus(), 80);
      return;
    }

    if (typing || state.currentRoute !== "study") return;
    if (event.key === "Escape") {
      closeStudy();
      return;
    }
    if (event.code === "Space" && target === elements.revealButton) {
      event.preventDefault();
      revealCurrentCard();
      return;
    }
    if (event.code === "Space" && target instanceof HTMLButtonElement && target.dataset.rating) {
      event.preventDefault();
      submitRating(target.dataset.rating);
      return;
    }
    if (event.code === "Space" && !interactive) {
      event.preventDefault();
      revealCurrentCard();
      return;
    }
    if (elements.flashcard.classList.contains("is-revealed")) {
      const ratings = { "1": "AGAIN", "2": "HARD", "3": "GOOD", "4": "EASY" };
      if (ratings[event.key]) submitRating(ratings[event.key]);
    }
  }

  async function apiRequest(path, options = {}) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 10000);
    const method = String(options.method || "GET").toUpperCase();
    const requestOptions = {
      method,
      headers: { Accept: "application/json" },
      credentials: "same-origin",
      signal: controller.signal
    };

    if (options.body !== undefined) {
      requestOptions.headers["Content-Type"] = "application/json";
      requestOptions.body = JSON.stringify(options.body);
    }
    if (method !== "GET" && method !== "HEAD") {
      requestOptions.headers["X-CSRF-Token"] = state.csrfToken || readStoredCsrf();
    }

    try {
      const response = await fetch(`${API_BASE}${path}`, requestOptions);
      const contentType = response.headers.get("content-type") || "";
      const payload = contentType.includes("application/json") ? await response.json() : null;
      if (!response.ok) {
        const error = new Error(payload?.message || `请求失败（${response.status}）`);
        error.status = response.status;
        throw error;
      }
      if (payload && payload.success === false) {
        const error = new Error(payload.message || "请求未成功");
        error.status = response.status;
        throw error;
      }
      return payload && Object.prototype.hasOwnProperty.call(payload, "data") ? payload.data : payload;
    } catch (error) {
      if (error.name === "AbortError") throw new Error("请求超时，请稍后重试");
      if (error.status === 401 && options.authRedirect !== false) redirectToLogin();
      throw error;
    } finally {
      window.clearTimeout(timeout);
    }
  }

  function asArray(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.items)) return data.items;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.words)) return data.words;
    return [];
  }

  function preferredKoreanMeaning(word = {}) {
    const direct = cleanText(word.koreanMeaning);
    const equivalents = cleanText(word.koreanEquivalents);
    const definition = cleanText(word.koreanDefinition);
    if (direct) return direct;
    if (equivalents && definition && equivalents !== definition) return `${equivalents} · ${definition}`;
    return equivalents || definition;
  }

  function normalizeStatus(value) {
    const status = String(value || "NEW").toUpperCase();
    const statuses = {
      NEW: { css: "new", label: "不认识" },
      AGAIN: { css: "new", label: "不认识" },
      UNKNOWN: { css: "new", label: "不认识" },
      LEARNING: { css: "learning", label: "模糊" },
      HARD: { css: "learning", label: "模糊" },
      FUZZY: { css: "learning", label: "模糊" },
      REVIEWING: { css: "reviewing", label: "记得" },
      GOOD: { css: "reviewing", label: "记得" },
      REMEMBERED: { css: "reviewing", label: "记得" },
      MASTERED: { css: "mastered", label: "熟练" },
      EASY: { css: "mastered", label: "熟练" }
    };
    return statuses[status] || statuses.NEW;
  }

  function renderDate() {
    const now = new Date();
    const hour = now.getHours();
    const greeting = hour < 11 ? "早上好，用一组单词开启今天" : hour < 18 ? "下午好，今天也要稳稳地进步" : "晚上好，来做一次轻松的复习";
    const fullDate = new Intl.DateTimeFormat("zh-CN", {
      year: "numeric", month: "long", day: "numeric", weekday: "long"
    }).format(now);
    const shortDate = new Intl.DateTimeFormat("zh-CN", { month: "short", day: "numeric" }).format(now);
    setText(elements.greeting, greeting);
    setText(elements.todayLabel, fullDate);
    elements.todayLabel.dateTime = now.toISOString().slice(0, 10);
    setText(elements.planDate, shortDate);
  }

  function toggleTheme() {
    const nextTheme = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nextTheme;
    try {
      localStorage.setItem("trivocab-theme", nextTheme);
    } catch (error) {
      // The selected theme still applies for this session when storage is unavailable.
    }
    syncThemeControl();
  }

  function syncThemeControl() {
    const dark = document.documentElement.dataset.theme === "dark";
    elements.themeToggle.setAttribute("aria-pressed", String(dark));
    elements.themeToggle.setAttribute("aria-label", dark ? "切换为浅色模式" : "切换为深色模式");
    setText(elements.themeToggleLabel, dark ? "深色" : "浅色");
    if (elements.themeColorMeta) {
      elements.themeColorMeta.setAttribute("content", dark ? "#111713" : "#edf0eb");
    }
  }

  function redirectToLogin() {
    clearStoredCsrf();
    const next = `${window.location.pathname}${window.location.hash}`;
    window.location.replace(`/login.html?next=${encodeURIComponent(next)}`);
  }

  function storeCsrf(token) {
    if (!token) return;
    try {
      sessionStorage.setItem(CSRF_STORAGE_KEY, token);
    } catch (error) {
      // The token remains available in memory for this page.
    }
  }

  function readStoredCsrf() {
    try {
      return sessionStorage.getItem(CSRF_STORAGE_KEY) || "";
    } catch (error) {
      return "";
    }
  }

  function clearStoredCsrf() {
    state.csrfToken = "";
    try {
      sessionStorage.removeItem(CSRF_STORAGE_KEY);
    } catch (error) {
      // No stored token needs to be cleared when storage is unavailable.
    }
  }

  function toggleCardAccessibility(revealed) {
    elements.flashcardFront.setAttribute("aria-hidden", String(revealed));
    elements.flashcardBack.setAttribute("aria-hidden", String(!revealed));
    elements.flashcardFront.toggleAttribute("inert", revealed);
    elements.flashcardBack.toggleAttribute("inert", !revealed);
  }

  function updateProgressAccessibility(element, value, valueText) {
    if (!element) return;
    element.setAttribute("aria-valuenow", String(Math.round(clamp(numberValue(value, 0), 0, 100))));
    element.setAttribute("aria-valuetext", valueText);
  }

  function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }

  function setRefreshState(loading) {
    elements.refreshButton.disabled = loading;
    elements.refreshButton.classList.toggle("is-spinning", loading);
  }

  function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast${type === "error" ? " is-error" : ""}`;
    toast.setAttribute("role", type === "error" ? "alert" : "status");
    toast.textContent = message;
    elements.toastRegion.append(toast);
    window.setTimeout(() => {
      toast.classList.add("is-leaving");
      window.setTimeout(() => toast.remove(), 260);
    }, 3600);
  }

  function setText(element, value) {
    if (element) element.textContent = value ?? "";
  }

  function numberValue(...values) {
    for (const value of values) {
      if (value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value))) {
        return Number(value);
      }
    }
    return 0;
  }

  function formatNumber(value) {
    return new Intl.NumberFormat("zh-CN").format(numberValue(value, 0));
  }

  function formatPercent(value) {
    return Math.round(numberValue(value, 0) * 10) / 10;
  }

  function formatPhonetic(value) {
    const phonetic = cleanText(value);
    if (!phonetic) return "";
    if (/^[/\[].*[/\]]$/.test(phonetic)) return phonetic;
    return `/${phonetic}/`;
  }

  function cleanText(value) {
    return String(value || "").trim();
  }

  function createReviewId() {
    if (window.crypto?.randomUUID) return window.crypto.randomUUID();
    return `review-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
  }

  function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
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

  function readError(error, fallback) {
    return error instanceof Error && error.message ? error.message : fallback;
  }

  function debounce(callback, delay) {
    let timeout;
    return (...args) => {
      window.clearTimeout(timeout);
      timeout = window.setTimeout(() => callback(...args), delay);
    };
  }

  function wait(milliseconds) {
    return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
  }
})();
