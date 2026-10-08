const API_BASE = "/api";
const TOKEN_KEY = "realestate.accessToken";
const USER_KEY = "realestate.user";

const state = {
    token: localStorage.getItem(TOKEN_KEY),
    user: readUser(),
    properties: [],
    favorites: new Set(),
    dashboard: { inquiries: [], visits: [] },
    dashboardRendered: false,
    authMode: "login",
    toastTimer: null
};

function readUser() {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY) || "null");
    } catch {
        localStorage.removeItem(USER_KEY);
        return null;
    }
}

async function removePropertyImage(propertyId, imageId) {
    try {
        await api(`/properties/${propertyId}/images/${imageId}`, { method: "DELETE" });
        showToast("Property photo removed.");
        await showProperty(propertyId);
    } catch (error) {
        showToast(error.message);
    }
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function money(value) {
    return new Intl.NumberFormat(undefined, {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    }).format(Number(value));
}

function formatDate(value) {
    if (!value) return "Date unavailable";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.token) headers.set("Authorization", `Bearer ${state.token}`);
    if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try {
            const body = await response.json();
            message = body.detail || body.message || body.title || message;
        } catch {
            if (response.status === 401) message = "Your session has expired. Please log in again.";
        }
        if (response.status === 401 && state.token) signOut(false);
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
}

function showToast(message) {
    const toast = document.querySelector("#toast");
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => toast.classList.remove("show"), 3600);
}

function setSignedIn(token, user) {
    state.token = token;
    state.user = user;
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    updateAuthUi();
    loadProperties().catch(error => showToast(error.message));
    if (user.role === "CUSTOMER") loadFavorites().catch(error => showToast(error.message));
    loadDashboard()
        .then(() => redirectToRoleDashboard(user.role))
        .catch(error => showToast(error.message));
}

function redirectToRoleDashboard(role) {
    const route = `${String(role).toLowerCase()}-dashboard`;
    history.replaceState(null, "", `#${route}`);
    navigateToCurrentHash();
}

function signOut(showMessage = true) {
    state.token = null;
    state.user = null;
    state.favorites.clear();
    state.dashboard = { inquiries: [], visits: [] };
    state.dashboardRendered = false;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    updateAuthUi();
    renderProperties();
    document.querySelector("#dashboard").classList.add("hidden");
    showPublicPage();
    if (showMessage) showToast("You have been logged out.");
}

const dashboardRoutes = {
    "customer-dashboard": { role: "CUSTOMER", target: "#customer-dashboard" },
    "customer-favorites": { role: "CUSTOMER", target: "#customer-favorites" },
    "customer-inquiries": { role: "CUSTOMER", target: "#customer-inquiries" },
    "customer-visits": { role: "CUSTOMER", target: "#customer-visits" },
    "customer-profile": { role: "CUSTOMER", target: "#customer-profile" },
    "agent-dashboard": { role: "AGENT", target: "#agent-dashboard" },
    "agent-properties": { role: "AGENT", target: "#agent-properties" },
    "agent-inquiries": { role: "AGENT", target: "#agent-inquiries" },
    "agent-visits": { role: "AGENT", target: "#agent-visits" },
    "admin-dashboard": { role: "ADMIN", target: "#admin-dashboard" },
    "admin-properties": { role: "ADMIN", target: "#admin-properties" },
    "admin-users": { role: "ADMIN", target: "#admin-users" },
    "admin-agents": { role: "ADMIN", target: "#admin-agents" },
    "admin-inquiries": { role: "ADMIN", target: "#admin-inquiries" },
    "admin-visits": { role: "ADMIN", target: "#admin-visits" },
    "admin-statistics": { role: "ADMIN", target: "#admin-statistics" }
};

function showPublicPage() {
    document.querySelector("#homes").classList.remove("hidden");
    document.querySelector(".search-section").classList.remove("hidden");
    document.querySelector("#property-results").classList.remove("hidden");
    document.querySelector(".closing-banner").classList.remove("hidden");
    document.querySelector("#dashboard").classList.add("hidden");
}

function focusDashboardRoute(route) {
    const content = document.querySelector("#dashboard-content");
    const focusedTarget = document.querySelector(dashboardRoutes[route].target);
    if (!focusedTarget || route.endsWith("-dashboard")) {
        Array.from(content.children).forEach(element => element.classList.remove("hidden"));
        return focusedTarget;
    }
    const section = focusedTarget.closest("#dashboard-content > *") || focusedTarget;
    Array.from(content.children).forEach(element => {
        element.classList.toggle("hidden", element !== section);
    });
    return focusedTarget;
}

async function navigateToCurrentHash(scroll = true) {
    const route = location.hash.slice(1).toLowerCase();
    const page = dashboardRoutes[route];
    if (!page) {
        showPublicPage();
        if (route === "homes" || route === "property-results") {
            document.querySelector(route === "homes" ? "#homes" : "#property-results")
                .scrollIntoView({ behavior: scroll ? "smooth" : "auto" });
        }
        return;
    }

    if (!state.token || !state.user) {
        showPublicPage();
        return;
    }
    if (state.user.role !== page.role) {
        showToast(`That page is only available to ${page.role} accounts.`);
        history.replaceState(null, "", `#${String(state.user.role).toLowerCase()}-dashboard`);
        return navigateToCurrentHash(scroll);
    }
    if (!state.dashboardRendered) {
        await loadDashboard();
    }

    document.querySelector("#homes").classList.add("hidden");
    document.querySelector(".search-section").classList.add("hidden");
    document.querySelector("#property-results").classList.add("hidden");
    document.querySelector(".closing-banner").classList.add("hidden");
    const dashboard = document.querySelector("#dashboard");
    dashboard.classList.remove("hidden");
    const target = focusDashboardRoute(route);
    target?.scrollIntoView({ behavior: scroll ? "smooth" : "auto", block: "start" });
}

function updateAuthUi() {
    const signedIn = Boolean(state.token && state.user);
    document.querySelector("#login-open").classList.toggle("hidden", signedIn);
    document.querySelector("#register-open").classList.toggle("hidden", signedIn);
    document.querySelector("#logout").classList.toggle("hidden", !signedIn);
    document.querySelectorAll(".signed-in-only").forEach(element => element.classList.toggle("hidden", !signedIn));
    const label = document.querySelector("#user-label");
    label.textContent = signedIn ? `${state.user.firstName} · ${state.user.role}` : "";
    label.classList.toggle("hidden", !signedIn);
    const navigation = {
        CUSTOMER: [
            ["Dashboard", "#customer-dashboard"],
            ["Properties", "#property-results"],
            ["Favorites", "#customer-favorites"],
            ["My inquiries", "#customer-inquiries"],
            ["My visits", "#customer-visits"],
            ["Profile", "#customer-profile"]
        ],
        AGENT: [
            ["Agent dashboard", "#agent-dashboard"],
            ["My properties", "#agent-properties"],
            ["Inquiries", "#agent-inquiries"],
            ["Visits", "#agent-visits"]
        ],
        ADMIN: [
            ["Admin dashboard", "#admin-dashboard"],
            ["All properties", "#admin-properties"],
            ["Agents", "#admin-agents"],
            ["Customers", "#admin-users"],
            ["All inquiries", "#admin-inquiries"],
            ["Visits", "#admin-visits"],
            ["System statistics", "#admin-statistics"]
        ]
    };
    const links = signedIn
        ? navigation[state.user.role]
        : [["Find a home", "#homes"]];
    document.querySelector(".main-nav").innerHTML = links.map(([text, target]) =>
        `<a href="${target}">${text}</a>`
    ).join("");
}

function openAuth(mode = "login") {
    setAuthMode(mode);
    document.querySelector("#auth-dialog").showModal();
}

function setAuthMode(mode) {
    state.authMode = mode;
    const register = mode === "register";
    document.querySelector("#auth-title").innerHTML = register ? "Create your <em>account.</em>" : "Log in to your <em>account.</em>";
    document.querySelector("#register-fields").classList.toggle("hidden", !register);
    document.querySelector("#phone-field").classList.toggle("hidden", !register);
    document.querySelector("#auth-form [name=password]").autocomplete = register ? "new-password" : "current-password";
    document.querySelector("#auth-form [name=firstName]").required = register;
    document.querySelector("#auth-form [name=lastName]").required = register;
    document.querySelector("#auth-form [name=phoneNumber]").required = false;
    document.querySelector("#auth-notice").textContent = "";
    document.querySelectorAll("[data-auth-mode]").forEach(button => {
        button.classList.toggle("active", button.dataset.authMode === mode);
    });
    document.querySelector("#auth-form button[type=submit]").innerHTML = register
        ? "Create account <span aria-hidden=\"true\">↗</span>"
        : "Log in <span aria-hidden=\"true\">↗</span>";
}

function searchQuery(form) {
    const values = new FormData(form);
    const query = new URLSearchParams();
    for (const key of ["city", "propertyType", "minPrice", "maxPrice", "minBedrooms", "status"]) {
        const value = String(values.get(key) || "").trim();
        if (value) query.set(key, value);
    }
    return query.toString();
}

async function loadProperties(query = searchQuery(document.querySelector("#search-form"))) {
    // Public listings are available without logging in. The backend filters
    // anonymous/customer requests to APPROVED + AVAILABLE properties.
    const properties = await api(`/properties${query ? `?${query}` : ""}`);
    state.properties = properties;
    renderProperties();
}

async function checkBackendConnection() {
    const status = document.querySelector("#backend-status");
    try {
        const health = await api("/health");
        status.classList.add("connected");
        status.textContent = health.status === "UP" ? "Backend connected" : "Backend responded";
        const indicator = document.createElement("i");
        status.prepend(indicator);
    } catch {
        status.classList.add("failed");
        status.textContent = "Backend unavailable";
        const indicator = document.createElement("i");
        status.prepend(indicator);
    }
}

async function loadFavorites() {
    if (!state.user || state.user.role !== "CUSTOMER") {
        state.favorites.clear();
        return;
    }
    const favorites = await api("/favorites");
    state.favorites = new Set(favorites.map(property => property.id));
    renderProperties();
}

function visual(property, index, showFavorite = true) {
    const tones = ["", "tone-1", "tone-2", "tone-3", "tone-4"];
    const favoriteButton = showFavorite && state.user?.role === "CUSTOMER"
        ? `<button class="favorite-button ${state.favorites.has(property.id) ? "is-favorite" : ""}" type="button" data-action="favorite" data-id="${property.id}" aria-label="${state.favorites.has(property.id) ? "Remove from" : "Add to"} favorites">${state.favorites.has(property.id) ? "♥" : "♡"}</button>`
        : "";
    return `<div class="property-visual ${tones[index % tones.length]} property-detail-visual">
        <span class="visual-sun"></span>
        <span class="property-type-pill">${escapeHtml(property.propertyType)}</span>
        ${favoriteButton}
        <span class="status-pill">${escapeHtml(property.status)}</span>
        <div class="mini-house" aria-hidden="true"><div class="mini-roof"></div><div class="mini-body"><span class="mini-window"></span><span class="mini-door"></span><span class="mini-window"></span></div></div>
    </div>`;
}

function renderProperties() {
    const grid = document.querySelector("#property-grid");
    const visible = state.user?.role === "AGENT"
        ? state.properties.filter(property => property.agentId === state.user.id)
        : state.properties;
    document.querySelector("#results-count").textContent = `${visible.length} ${visible.length === 1 ? "home" : "homes"} found`;
    if (!visible.length) {
        grid.innerHTML = `<p class="no-results">${state.user?.role === "AGENT" ? "You don't have any matching listings yet." : "No homes match those filters. Try broadening your search."}</p>`;
        return;
    }
    grid.innerHTML = visible.map((property, index) => `
        <article class="property-card">
            ${visual(property, index)}
            <div class="property-card-body">
                <div class="property-price">${money(property.price)}</div>
                <h3 class="property-title">${escapeHtml(property.title)}</h3>
                <p class="property-location">${escapeHtml(property.address)}, ${escapeHtml(property.city)}</p>
                <div class="property-meta">
                    ${property.bedrooms == null ? "" : `<span>${property.bedrooms} beds</span>`}
                    ${property.bathrooms == null ? "" : `<span>${property.bathrooms} baths</span>`}
                    ${property.areaSqft == null ? "" : `<span>${property.areaSqft.toLocaleString()} sq ft</span>`}
                </div>
                <div class="property-card-footer">
                    <span class="muted">Listed by an H&amp;H agent</span>
                    <button class="button button-dark" type="button" data-action="details" data-id="${property.id}">View details <span aria-hidden="true">↗</span></button>
                </div>
            </div>
        </article>`).join("");
}

async function showProperty(id) {
    const [property, images] = await Promise.all([
        api(`/properties/${id}`),
        api(`/properties/${id}/images`)
    ]);
    const canManage = ["ADMIN", "AGENT"].includes(state.user?.role)
        && (state.user.role === "ADMIN" || property.agentId === state.user.id);
    const gallery = images.length
        ? `<div class="property-image-gallery">${images.map(image => `
            <figure>
                <img src="${escapeHtml(image.imageUrl)}" alt="${escapeHtml(property.title)}" loading="lazy">
                <figcaption>${image.primary ? "Main photo" : "Property photo"}
                    ${canManage ? `<button class="small-button danger" type="button" data-action="delete-property-image" data-property-id="${property.id}" data-image-id="${image.id}">Remove</button>` : ""}
                </figcaption>
            </figure>`).join("")}</div>`
        : visual(property, Number(property.id) || 0);
    const imageUpload = canManage ? `
        <form class="stack-form inline-action-form" data-form="property-images" data-property-id="${property.id}">
            <label>Add property photos<input name="images" type="file" accept="image/jpeg,image/png" multiple required></label>
            <p class="form-notice" aria-live="polite"></p>
            <button class="button button-dark" type="submit">Upload photos</button>
        </form>` : "";
    document.querySelector("#property-detail-content").innerHTML = `
        ${gallery}
        <div class="property-detail-body">
            <p class="eyebrow">${escapeHtml(property.propertyType)} · ${escapeHtml(property.status)}</p>
            ${["ADMIN", "AGENT"].includes(state.user?.role) ? `<p class="muted">Approval: ${escapeHtml(property.approvalStatus)}</p>` : ""}
            <div class="detail-price">${money(property.price)}</div>
            <h2>${escapeHtml(property.title)}</h2>
            <p>${escapeHtml(property.address)}, ${escapeHtml(property.city)}</p>
            <p>${escapeHtml(property.description || "A lovely place ready for its next chapter.")}</p>
            <div class="property-meta">
                ${property.bedrooms == null ? "" : `<span>${property.bedrooms} bedrooms</span>`}
                ${property.bathrooms == null ? "" : `<span>${property.bathrooms} bathrooms</span>`}
                ${property.areaSqft == null ? "" : `<span>${property.areaSqft.toLocaleString()} sq ft</span>`}
            </div>
            ${imageUpload}
            <div class="detail-actions">
                ${state.user?.role === "CUSTOMER" ? `<button class="button button-dark" data-action="inquiry-form" data-id="${property.id}">Ask about this home</button><button class="button button-dark" data-action="visit-form" data-id="${property.id}">Schedule a visit</button>` : ""}
                ${state.user?.role === "CUSTOMER" ? `<button class="small-button" data-action="favorite" data-id="${property.id}">${state.favorites.has(property.id) ? "♥ Saved" : "♡ Save home"}</button>` : ""}
                ${["ADMIN", "AGENT"].includes(state.user?.role) && (state.user.role === "ADMIN" || property.agentId === state.user.id) ? `<button class="button button-dark" data-action="edit-property" data-id="${property.id}">Edit listing</button><button class="small-button danger" data-action="delete-property" data-id="${property.id}">Delete</button>` : ""}
                ${state.user?.role === "ADMIN" && property.approvalStatus !== "APPROVED" ? `<button class="small-button" data-action="approve-property" data-id="${property.id}">Approve listing</button>` : ""}
                ${state.user?.role === "ADMIN" && property.approvalStatus !== "REJECTED" ? `<button class="small-button danger" data-action="reject-property" data-id="${property.id}">Reject listing</button>` : ""}
            </div>
            ${state.user?.role === "CUSTOMER" ? `
                <form class="stack-form inline-action-form hidden" data-form="inquiry" data-property-id="${property.id}">
                    <label>Your message<textarea name="message" rows="3" required maxlength="5000" placeholder="What would you like to know?"></textarea></label>
                    <p class="form-notice" aria-live="polite"></p>
                    <button class="button button-dark" type="submit">Send inquiry</button>
                </form>
                <form class="stack-form inline-action-form hidden" data-form="visit" data-property-id="${property.id}">
                    <label>Choose a visit date and time<input name="scheduledAt" type="datetime-local" min="${minimumLocalDateTime()}" required></label>
                    <p class="form-notice" aria-live="polite"></p>
                    <button class="button button-dark" type="submit">Request visit</button>
                </form>` : ""}
        </div>`;
    document.querySelector("#property-dialog").showModal();
}

async function toggleFavorite(id) {
    if (!state.user) return openAuth("login");
    if (state.user.role !== "CUSTOMER") return showToast("Favorites are available to customer accounts.");
    try {
        if (state.favorites.has(id)) {
            await api(`/favorites/${id}`, { method: "DELETE" });
            state.favorites.delete(id);
            showToast("Removed from your favorites.");
        } else {
            await api(`/favorites/${id}`, { method: "POST" });
            state.favorites.add(id);
            showToast("Saved to your favorites.");
        }
        renderProperties();
        if (document.querySelector("#property-dialog").open) await showProperty(id);
        if (!document.querySelector("#dashboard").classList.contains("hidden")) await loadDashboard();
    } catch (error) {
        showToast(error.message);
    }
}

async function loadDashboard() {
    if (!state.token || !state.user) return;
    const dashboard = document.querySelector("#dashboard");
    const content = document.querySelector("#dashboard-content");
    const role = state.user.role;
    const isCustomer = role === "CUSTOMER";
    const isAgent = role === "AGENT";
    const isAdmin = role === "ADMIN";
    dashboard.classList.remove("hidden");
    document.querySelector("#dashboard-role").textContent = role;
    document.querySelector("#dashboard-title").innerHTML = isCustomer
        ? "Your home-finding <em>journey.</em>"
        : isAgent ? "Your listings, <em>at a glance.</em>" : "The whole picture, <em>at a glance.</em>";
    document.querySelector("#dashboard-eyebrow").textContent = `Welcome back, ${state.user.firstName}`;
    const requests = [
        api("/properties"),
        api("/inquiries"),
        api("/visits")
    ];
    if (isCustomer) requests.push(api("/favorites"), api("/customers/me"));
    if (isAdmin) requests.push(api("/admin/statistics"), api("/admin/users"), api("/admin/agents"));
    const data = await Promise.all(requests);
    const properties = data[0];
    const inquiries = data[1];
    const visits = data[2];
    const customerFavorites = isCustomer ? data[3] : [];
    const customerProfile = isCustomer ? data[4] : null;
    const adminStatistics = isAdmin ? data[3] : null;
    const users = isAdmin ? data[4] : [];
    const agents = isAdmin ? data[5] : [];
    state.dashboard = { inquiries, visits };
    if (customerProfile) {
        state.user = customerProfile;
        localStorage.setItem(USER_KEY, JSON.stringify(customerProfile));
        updateAuthUi();
    }
    const metrics = isCustomer
        ? [["Saved homes", customerFavorites.length], ["My inquiries", inquiries.length], ["Upcoming visits", visits.filter(visit => ["PENDING", "ACCEPTED"].includes(visit.status)).length]]
        : isAgent
            ? [["My properties", properties.length], ["Customer inquiries", inquiries.length], ["Scheduled visits", visits.filter(visit => ["PENDING", "ACCEPTED"].includes(visit.status)).length]]
            : [
                ["Total users", adminStatistics.totalUsers],
                ["Total agents", adminStatistics.totalAgents],
                ["Total customers", adminStatistics.totalCustomers],
                ["Total properties", adminStatistics.totalProperties],
                ["Available properties", adminStatistics.availableProperties],
                ["Sold / rented properties", adminStatistics.soldOrRentedProperties],
                ["Pending inquiries", adminStatistics.pendingInquiries],
                ["Pending visit requests", adminStatistics.pendingVisitRequests]
            ];
    content.innerHTML = `
        <div id="${isCustomer ? "customer-statistics" : isAgent ? "agent-statistics" : "admin-statistics"}" class="metric-grid">${metrics.map(([label, value]) => `<div class="metric-card"><span>${label}</span><strong>${value}</strong></div>`).join("")}</div>
        ${isAdmin ? renderAdminQuickActions() : ""}
        ${isAgent ? renderAgentProfile(state.user) : ""}
        ${isCustomer ? renderDashboardProperties("Your favorites", customerFavorites, "No saved homes yet. Tap the heart on a listing to keep it here.", "customer-favorites") : ""}
        ${isAgent || isAdmin ? renderManagedProperties(properties, isAdmin) : ""}
        ${isAdmin ? renderAdminUsers(users, agents) : ""}
        ${renderInquiries(inquiries, isCustomer)}
        ${renderVisits(visits, isCustomer)}
        ${isCustomer ? renderCustomerProfile(customerProfile) : ""}`;
    state.dashboardRendered = true;
}

function renderAdminQuickActions() {
    return `<section class="dashboard-panel admin-quick-actions" aria-label="Admin quick actions">
        <div class="panel-heading"><h3>Quick actions</h3></div>
        <div class="quick-action-grid">
            <button class="button button-dark" data-action="add-agent">+ Add Agent</button>
            <button class="button button-dark" data-action="new-property">+ Add Property</button>
            <a class="button button-quiet" href="#admin-agents">Manage Agents</a>
            <a class="button button-quiet" href="#admin-properties">Manage Properties</a>
            <a class="button button-quiet" href="#admin-users">Manage Customers</a>
            <a class="button button-quiet" href="#admin-inquiries">View Inquiries</a>
            <a class="button button-quiet" href="#admin-visits">View Visits</a>
        </div>
    </section>`;
}

function renderAgentProfile(user) {
    return `<section id="agent-profile" class="dashboard-panel">
        <div class="panel-heading"><h3>Agent profile</h3><span class="role-pill">AGENT</span></div>
        <div class="dashboard-row"><div><strong>${escapeHtml(user.firstName)} ${escapeHtml(user.lastName)}</strong><small>${escapeHtml(user.email)}</small>${user.phoneNumber ? `<small>${escapeHtml(user.phoneNumber)}</small>` : ""}</div><span class="muted">Signed in as AGENT</span></div>
    </section>`;
}

function renderDashboardProperties(title, properties, emptyText, sectionId = "") {
    return `<section${sectionId ? ` id="${sectionId}"` : ""} class="dashboard-panel"><div class="panel-heading"><h3>${title}</h3><a class="text-button" href="#property-results" data-action="browse">Browse homes ↗</a></div>
        ${properties.length ? `<div class="property-grid">${properties.map((property, index) => `<article class="property-card">${visual(property, index, false)}<div class="property-card-body"><div class="property-price">${money(property.price)}</div><h3 class="property-title">${escapeHtml(property.title)}</h3><p class="property-location">${escapeHtml(property.city)}</p><div class="property-card-footer"><button class="button button-dark" data-action="details" data-id="${property.id}">View details</button><button class="small-button danger" data-action="favorite" data-id="${property.id}">Remove</button></div></div></article>`).join("")}</div>` : `<p class="empty-note">${emptyText}</p>`}
    </section>`;
}

function renderCustomerProfile(user) {
    return `<section id="customer-profile" class="dashboard-panel">
        <div class="panel-heading"><h3>My profile</h3><span class="role-pill">CUSTOMER</span></div>
        <form class="stack-form customer-profile-form" data-form="customer-profile">
            <div class="form-pair">
                <label>First name<input name="firstName" value="${escapeHtml(user.firstName)}" required maxlength="100"></label>
                <label>Last name<input name="lastName" value="${escapeHtml(user.lastName)}" required maxlength="100"></label>
            </div>
            <label>Email address<input type="email" value="${escapeHtml(user.email)}" readonly aria-describedby="profile-email-note"></label>
            <small id="profile-email-note" class="muted">Email addresses are managed by your account administrator.</small>
            <label>Phone number<input name="phoneNumber" type="tel" value="${escapeHtml(user.phoneNumber || "")}" maxlength="30"></label>
            <p class="form-notice" aria-live="polite"></p>
            <button class="button button-dark" type="submit">Save profile</button>
        </form>
    </section>`;
}

function renderManagedProperties(properties, isAdmin) {
    return `<section id="${isAdmin ? "admin-properties" : "agent-properties"}" class="dashboard-panel"><div class="panel-heading"><h3>${isAdmin ? "All properties" : "My properties"}</h3><button class="button button-dark" data-action="new-property">Add property <span aria-hidden="true">+</span></button></div>
        ${properties.length ? `<div class="dashboard-list">${properties.map(property => `<div class="dashboard-row"><div><strong>${escapeHtml(property.title)}</strong><small>${money(property.price)} · ${escapeHtml(property.city)} · ${escapeHtml(property.status)}${isAdmin ? ` · Agent #${property.agentId} · Approval: ${property.approvalStatus}` : ` · ${property.approvalStatus === "PENDING" ? "Awaiting admin approval" : property.approvalStatus}`}</small></div><div class="dashboard-row-actions"><button class="small-button" data-action="details" data-id="${property.id}">Images</button>${isAdmin && property.approvalStatus !== "APPROVED" ? `<button class="small-button" data-action="approve-property" data-id="${property.id}">Approve</button>` : ""}${isAdmin && property.approvalStatus !== "REJECTED" ? `<button class="small-button danger" data-action="reject-property" data-id="${property.id}">Reject</button>` : ""}<button class="small-button" data-action="edit-property" data-id="${property.id}">Edit</button><button class="small-button danger" data-action="delete-property" data-id="${property.id}">Delete</button></div></div>`).join("")}</div>` : `<p class="empty-note">No properties to display.</p>`}
    </section>`;
}

function renderAdminUsers(users, agents) {
    const rows = (list, emptyText, editable) => list.length
        ? `<div class="dashboard-list">${list.map(user => `<div class="dashboard-row"><div><strong>${escapeHtml(user.firstName)} ${escapeHtml(user.lastName)}</strong><small>${escapeHtml(user.email)} · ${user.role}${user.role === "AGENT" && !user.active ? " · Deactivated" : ""}</small></div>${editable ? `<form class="dashboard-row-actions" data-form="role" data-user-id="${user.id}"><label class="muted">Role <select name="role">${["CUSTOMER", "AGENT", "ADMIN"].map(role => `<option value="${role}" ${role === user.role ? "selected" : ""}>${role}</option>`).join("")}</select></label><button class="small-button" type="submit">Update</button></form>` : `<div class="dashboard-row-actions"><button class="small-button" data-action="edit-agent" data-id="${user.id}" data-first-name="${escapeHtml(user.firstName)}" data-last-name="${escapeHtml(user.lastName)}" data-email="${escapeHtml(user.email)}" data-phone="${escapeHtml(user.phoneNumber || "")}">Edit</button><button class="small-button ${user.active ? "danger" : ""}" data-action="${user.active ? "deactivate-agent" : "activate-agent"}" data-id="${user.id}">${user.active ? "Deactivate" : "Activate"}</button><button class="small-button danger" data-action="delete-agent" data-id="${user.id}">Delete</button></div>`}</div>`).join("")}</div>`
        : `<p class="empty-note">${emptyText}</p>`;
    return `<section id="admin-users" class="dashboard-panel"><div class="panel-heading"><h3>Manage users</h3><button class="button button-dark" data-action="add-agent">Add Agent <span aria-hidden="true">+</span></button><span class="muted">${users.length} accounts</span></div>${rows(users, "No users to display.", true)}</section>
        <section id="admin-agents" class="dashboard-panel"><div class="panel-heading"><h3>Manage agents</h3><button class="button button-dark" data-action="add-agent">Add Agent <span aria-hidden="true">+</span></button><span class="muted">${agents.length} agents</span></div>${rows(agents, "No agents to display.", false)}</section>`;
}

function renderInquiries(inquiries, isCustomer) {
    const canReply = state.user?.role === "AGENT" || state.user?.role === "ADMIN";
    const title = isCustomer ? "Your inquiries" : state.user?.role === "ADMIN" ? "All inquiries" : "Customer inquiries for my properties";
    const sectionId = state.user?.role === "ADMIN" ? "admin-inquiries" : state.user?.role === "AGENT" ? "agent-inquiries" : "customer-inquiries";
    return `<section id="${sectionId}" class="dashboard-panel"><div class="panel-heading"><h3>${title}</h3></div>
        ${inquiries.length ? `<div class="dashboard-list">${inquiries.map(item => `<div class="dashboard-row"><div><strong>${escapeHtml(item.propertyTitle)}</strong><small>${isCustomer ? "Your message" : `From ${escapeHtml(item.customerEmail)}`} · ${formatDate(item.createdAt)}</small><small>${escapeHtml(item.message)}</small>${item.responseMessage ? `<small><strong>${isCustomer ? "Agent reply" : "Reply"}:</strong> ${escapeHtml(item.responseMessage)} · ${formatDate(item.respondedAt)}</small>` : ""}${canReply && !item.responseMessage ? `<form class="stack-form inline-action-form" data-form="reply" data-inquiry-id="${item.id}"><label>Reply to customer<textarea name="message" rows="2" required maxlength="5000"></textarea></label><p class="form-notice" aria-live="polite"></p><button class="small-button" type="submit">Send reply</button></form>` : ""}</div><span class="status-pill">${escapeHtml(item.status)}</span></div>`).join("")}</div>` : `<p class="empty-note">No inquiries yet.</p>`}
    </section>`;
}

function renderVisits(visits, isCustomer) {
    const isAgent = state.user?.role === "AGENT";
    const title = isCustomer ? "Your scheduled visits" : "Property visits";
    const sectionId = isCustomer ? "customer-visits" : state.user?.role === "AGENT" ? "agent-visits" : "admin-visits";
    return `<section id="${sectionId}" class="dashboard-panel"><div class="panel-heading"><h3>${title}</h3></div>
        ${visits.length ? `<div class="dashboard-list">${visits.map(visit => `<div class="dashboard-row"><div><strong>${escapeHtml(visit.propertyTitle)}</strong><small>${isCustomer ? "" : `Customer: ${escapeHtml(visit.customerEmail)} · `}${formatDate(visit.scheduledAt)} · ${escapeHtml(visit.status)}</small></div><div class="dashboard-row-actions">${isCustomer && visit.status === "PENDING" ? `<button class="small-button danger" data-action="cancel-visit" data-id="${visit.id}">Cancel</button>` : ""}${isAgent && visit.status === "PENDING" ? `<button class="small-button" data-action="accept-visit" data-id="${visit.id}">Accept</button><button class="small-button danger" data-action="reject-visit" data-id="${visit.id}">Reject</button>` : ""}</div></div>`).join("")}</div>` : `<p class="empty-note">${isAgent ? "No visit requests for your properties yet." : "No scheduled visits yet."}</p>`}
    </section>`;
}

function openPropertyForm(property = null) {
    if (!state.user || !["ADMIN", "AGENT"].includes(state.user.role)) return;
    const form = document.querySelector("#property-form");
    form.reset();
    form.elements.id.value = property?.id || "";
    document.querySelector("#property-form-title").innerHTML = property ? "Edit a <em>listing.</em>" : "Add a <em>property.</em>";
    document.querySelector("#agent-id-field").classList.toggle("hidden", state.user.role !== "ADMIN");
    form.elements.agentId.required = state.user.role === "ADMIN" && !property;
    if (property) {
        for (const key of ["title", "description", "propertyType", "price", "bedrooms", "bathrooms", "areaSqft", "city", "address", "status", "agentId"]) {
            if (property[key] != null) form.elements[key].value = property[key];
        }
    }
    document.querySelector("#property-form-notice").textContent = "";
    document.querySelector("#property-form-dialog").showModal();
}

async function fetchPropertyForEdit(id) {
    try {
        const property = await api(`/properties/${id}`);
        openPropertyForm(property);
    } catch (error) {
        showToast(error.message);
    }
}

async function removeProperty(id) {
    if (!confirm("Delete this property listing?")) return;
    try {
        await api(`/properties/${id}`, { method: "DELETE" });
        document.querySelector("#property-dialog").close();
        showToast("Property listing deleted.");
        await loadProperties();
        await loadDashboard();
    } catch (error) {
        showToast(error.message);
    }
}

function minimumLocalDateTime() {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
}

async function submitInquiry(propertyId, message) {
    try {
        await api("/inquiries", {
            method: "POST",
            body: JSON.stringify({ propertyId, message })
        });
        showToast("Your inquiry has been sent.");
        document.querySelector("#property-dialog").close();
        await loadDashboard();
    } catch (error) {
        showToast(error.message);
    }
}

async function scheduleVisit(propertyId, scheduledAt) {
    try {
        await api("/visits", {
            method: "POST",
            body: JSON.stringify({ propertyId, scheduledAt })
        });
        showToast("Your visit has been scheduled.");
        document.querySelector("#property-dialog").close();
        await loadDashboard();
    } catch (error) {
        showToast(error.message);
    }
}

async function cancelVisit(id) {
    if (!confirm("Cancel this scheduled visit?")) return;
    try {
        await api(`/visits/${id}`, { method: "DELETE" });
        showToast("Visit cancelled.");
        await loadDashboard();
    } catch (error) {
        showToast(error.message);
    }
}

async function decideVisit(id, status) {
    const action = status === "ACCEPTED" ? "accept" : "reject";
    if (!confirm(`Are you sure you want to ${action} this visit request?`)) return;
    try {
        await api(`/visits/${id}/status`, {
            method: "PATCH",
            body: JSON.stringify({ status })
        });
        showToast(`Visit request ${action}ed.`);
        await loadDashboard();
    } catch (error) {
        showToast(error.message);
    }
}

async function replyToInquiry(inquiryId, message) {
    await api(`/inquiries/${inquiryId}/reply`, {
        method: "PUT",
        body: JSON.stringify({ message })
    });
    showToast("Your reply has been sent.");
    await loadDashboard();
}

async function uploadPropertyImages(propertyId, files, notice) {
    const selectedImages = Array.from(files);
    for (const [index, file] of selectedImages.entries()) {
        const upload = new FormData();
        upload.append("file", file);
        try {
            await api(`/properties/${propertyId}/images`, { method: "POST", body: upload });
        } catch (error) {
            notice.textContent = `${file.name} could not be uploaded: ${error.message}`;
            return;
        }
    }
    showToast("Property photos uploaded.");
    await showProperty(propertyId);
}

document.querySelector("#auth-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    const notice = document.querySelector("#auth-notice");
    const login = state.authMode === "login";
    const body = login
        ? { email: values.get("email"), password: values.get("password") }
        : {
            firstName: values.get("firstName"),
            lastName: values.get("lastName"),
            email: values.get("email"),
            password: values.get("password"),
            phoneNumber: values.get("phoneNumber") || null
        };
    try {
        const response = await api(login ? "/auth/login" : "/auth/register", {
            method: "POST",
            body: JSON.stringify(body)
        });
        if (login) {
            setSignedIn(response.accessToken, response.user);
            document.querySelector("#auth-dialog").close();
            showToast(`Welcome back, ${response.user.firstName}.`);
        } else {
            notice.classList.add("success");
            notice.textContent = "Account created. Please log in with your new credentials.";
            form.reset();
            setAuthMode("login");
        }
    } catch (error) {
        notice.classList.remove("success");
        notice.textContent = error.message;
    }
});

document.querySelector("#search-form").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        await loadProperties(searchQuery(event.currentTarget));
        document.querySelector("#property-results").scrollIntoView({ behavior: "smooth" });
    } catch (error) {
        showToast(error.message);
    }
});

document.querySelector("#clear-search").addEventListener("click", async () => {
    document.querySelector("#search-form").reset();
    try {
        await loadProperties("");
    } catch (error) {
        showToast(error.message);
    }
});

document.querySelector("#property-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    const id = values.get("id");
    const optionalNumber = key => values.get(key) === "" ? null : Number(values.get(key));
    const body = {
        title: values.get("title"),
        description: values.get("description") || null,
        propertyType: values.get("propertyType"),
        price: Number(values.get("price")),
        bedrooms: optionalNumber("bedrooms"),
        bathrooms: optionalNumber("bathrooms"),
        areaSqft: optionalNumber("areaSqft"),
        city: values.get("city"),
        address: values.get("address"),
        status: values.get("status"),
        agentId: state.user.role === "ADMIN" ? optionalNumber("agentId") : null
    };
    let savedProperty = null;
    try {
        savedProperty = await api(id ? `/properties/${id}` : "/properties", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(body)
        });
        form.elements.id.value = savedProperty.id;
        const imageInput = form.elements.images;
        const selectedImages = Array.from(imageInput.files);
        for (const [index, file] of selectedImages.entries()) {
            const upload = new FormData();
            upload.append("file", file);
            try {
                await api(`/properties/${savedProperty.id}/images`, { method: "POST", body: upload });
            } catch (error) {
                const remainingImages = new DataTransfer();
                selectedImages.slice(index).forEach(remaining => remainingImages.items.add(remaining));
                imageInput.files = remainingImages.files;
                throw error;
            }
        }
        imageInput.value = "";
        document.querySelector("#property-form-dialog").close();
        showToast(id ? "Property updated." : "Property created.");
        await loadProperties();
        await loadDashboard();
    } catch (error) {
        const notice = document.querySelector("#property-form-notice");
        notice.textContent = savedProperty
            ? `The property was saved, but an image could not be uploaded: ${error.message}`
            : error.message;
    }
});

document.addEventListener("submit", async event => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || !form.matches("[data-form]")) return;
    event.preventDefault();
    const propertyId = Number(form.dataset.propertyId);
    const values = new FormData(form);
    const notice = form.querySelector(".form-notice");
    notice.textContent = "";
    try {
        if (form.dataset.form === "inquiry") {
            await submitInquiry(propertyId, String(values.get("message")).trim());
        } else if (form.dataset.form === "visit") {
            await scheduleVisit(propertyId, values.get("scheduledAt"));
        } else if (form.dataset.form === "reply") {
            await replyToInquiry(Number(form.dataset.inquiryId), String(values.get("message")).trim());
        } else if (form.dataset.form === "property-images") {
            await uploadPropertyImages(propertyId, values.getAll("images"), notice);
        } else if (form.dataset.form === "role") {
            await api(`/admin/users/${form.dataset.userId}/role`, {
                method: "PUT",
                body: JSON.stringify({ role: values.get("role") })
            });
            showToast("User role updated.");
            await loadDashboard();
        } else if (form.dataset.form === "add-agent") {
            const created = await api("/admin/agents", {
                method: "POST",
                body: JSON.stringify({
                    firstName: values.get("firstName"),
                    lastName: values.get("lastName"),
                    email: values.get("email"),
                    password: values.get("password"),
                    phoneNumber: values.get("phoneNumber") || null
                })
            });
            form.reset();
            document.querySelector("#agent-form-dialog").close();
            showToast(`Agent ${created.firstName} ${created.lastName} created.`);
            await loadDashboard();
        } else if (form.dataset.form === "edit-agent") {
            const agentId = form.dataset.agentId;
            const updated = await api(`/admin/agents/${agentId}`, {
                method: "PUT",
                body: JSON.stringify({
                    firstName: values.get("firstName"),
                    lastName: values.get("lastName"),
                    email: values.get("email"),
                    phoneNumber: values.get("phoneNumber") || null
                })
            });
            document.querySelector("#edit-agent-dialog").close();
            showToast(`Agent ${updated.firstName} ${updated.lastName} updated.`);
            await loadDashboard();
        } else if (form.dataset.form === "customer-profile") {
            const updated = await api("/customers/me", {
                method: "PUT",
                body: JSON.stringify({
                    firstName: values.get("firstName"),
                    lastName: values.get("lastName"),
                    phoneNumber: values.get("phoneNumber") || null
                })
            });
            state.user = updated;
            localStorage.setItem(USER_KEY, JSON.stringify(updated));
            updateAuthUi();
            showToast("Your profile has been updated.");
            await loadDashboard();
        }
    } catch (error) {
        if (notice) notice.textContent = error.message;
        else showToast(error.message);
    }
});

document.querySelector("#login-open").addEventListener("click", () => openAuth("login"));
document.querySelector("#register-open").addEventListener("click", () => openAuth("register"));
document.querySelector("#logout").addEventListener("click", () => signOut());
document.querySelectorAll("[data-auth-mode]").forEach(button => {
    button.addEventListener("click", () => setAuthMode(button.dataset.authMode));
});
document.querySelectorAll("[data-close-dialog]").forEach(button => {
    button.addEventListener("click", () => button.closest("dialog").close());
});
document.querySelector("#mobile-menu").addEventListener("click", () => {
    document.querySelector(".main-nav").classList.toggle("open");
});
document.querySelectorAll("[data-action=home], [data-action=browse]").forEach(link => {
    link.addEventListener("click", () => {
        showPublicPage();
        document.querySelector(".main-nav").classList.remove("open");
        document.querySelector("#homes").scrollIntoView({ behavior: "smooth" });
    });
});
document.querySelector(".main-nav").addEventListener("click", event => {
    const link = event.target.closest("a[href^='#']");
    if (!link) return;
    event.preventDefault();
    document.querySelector(".main-nav").classList.remove("open");
    const href = link.getAttribute("href");
    if (location.hash === href) {
        navigateToCurrentHash();
    } else {
        location.hash = href;
    }
});
window.addEventListener("hashchange", () => {
    navigateToCurrentHash().catch(error => showToast(error.message));
});

document.addEventListener("click", async event => {
    const actionElement = event.target.closest("[data-action]");
    if (!actionElement) return;
    const { action, id } = actionElement.dataset;
    if (action === "details") return showProperty(id).catch(error => showToast(error.message));
    if (action === "favorite") return toggleFavorite(Number(id));
    if (action === "new-property") return openPropertyForm();
    if (action === "add-agent") return document.querySelector("#agent-form-dialog").showModal();
    if (action === "edit-agent") {
        const form = document.querySelector("#edit-agent-form");
        form.dataset.agentId = id;
        const agentFields = { firstName: "firstName", lastName: "lastName", email: "email", phoneNumber: "phone" };
        for (const [field, dataAttribute] of Object.entries(agentFields)) {
            form.elements[field].value = actionElement.dataset[dataAttribute] || "";
        }
        form.querySelector(".form-notice").textContent = "";
        document.querySelector("#edit-agent-dialog").showModal();
        return;
    }
    if (action === "deactivate-agent") {
        if (!window.confirm("Deactivate this agent? They will lose access immediately, but their records will be preserved.")) return;
        try {
            await api(`/admin/agents/${id}/deactivate`, { method: "PATCH" });
            showToast("Agent deactivated. Existing access tokens are now invalid.");
            await loadDashboard();
        } catch (error) {
            showToast(error.message);
        }
        return;
    }
    if (action === "activate-agent") {
        try {
            await api(`/admin/agents/${id}/activate`, { method: "PATCH" });
            showToast("Agent activated.");
            await loadDashboard();
        } catch (error) {
            showToast(error.message);
        }
        return;
    }
    if (action === "delete-agent") {
        if (!window.confirm("Delete this agent? Accounts with related properties, visits, inquiries, or favorites cannot be deleted; deactivate the agent instead.")) return;
        try {
            await api(`/admin/agents/${id}`, { method: "DELETE" });
            showToast("Agent deleted.");
            await loadDashboard();
        } catch (error) {
            showToast(error.message);
        }
    }
    if (action === "edit-property") return fetchPropertyForEdit(id);
    if (action === "delete-property") return removeProperty(id);
    if (action === "approve-property" || action === "reject-property") {
        const approvalStatus = action === "approve-property" ? "APPROVED" : "REJECTED";
        try {
            await api(`/properties/${id}/approval`, {
                method: "PATCH",
                body: JSON.stringify({ approvalStatus })
            });
            showToast(`Property ${approvalStatus.toLowerCase()}.`);
            await loadProperties();
            await loadDashboard();
            if (document.querySelector("#property-dialog").open) await showProperty(id);
        } catch (error) {
            showToast(error.message);
        }
        return;
    }
    if (action === "delete-property-image") {
        return removePropertyImage(actionElement.dataset.propertyId, actionElement.dataset.imageId);
    }
    if (action === "inquiry-form" || action === "visit-form") {
        const type = action === "inquiry-form" ? "inquiry" : "visit";
        const form = document.querySelector(`#property-detail-content [data-form="${type}"]`);
        form.classList.toggle("hidden");
        form.scrollIntoView({ behavior: "smooth", block: "nearest" });
        return;
    }
    if (action === "cancel-visit") return cancelVisit(id);
    if (action === "accept-visit") return decideVisit(id, "ACCEPTED");
    if (action === "reject-visit") return decideVisit(id, "REJECTED");
});

document.querySelector("#year").textContent = new Date().getFullYear();
updateAuthUi();
checkBackendConnection();
loadProperties("").catch(error => {
    document.querySelector("#property-grid").innerHTML = `<p class="no-results">Could not load property listings: ${escapeHtml(error.message)}</p>`;
});
if (state.token && state.user) {
    loadFavorites().catch(error => showToast(error.message));
    loadDashboard()
        .then(() => navigateToCurrentHash(false))
        .catch(error => showToast(error.message));
} else {
    navigateToCurrentHash(false);
}
