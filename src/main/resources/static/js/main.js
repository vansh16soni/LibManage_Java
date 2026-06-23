// ============================================================
// LibManage — main.js
// Toast helper, Chart.js leaderboard, AJAX interactions
// ============================================================

/* ---------- Toasts ---------- */
function lmToast(message, type = "success") {
    const container = document.getElementById("toastContainer");
    if (!container) return;
    const id = "toast-" + Date.now();
    const cls = type === "error" ? "lm-toast-error" : "lm-toast-success";
    const icon = type === "error" ? "bi-exclamation-circle-fill text-danger" : "bi-check-circle-fill text-success";
    const el = document.createElement("div");
    el.id = id;
    el.className = `toast align-items-center border-0 shadow-sm ${cls}`;
    el.setAttribute("role", "alert");
    el.innerHTML = `
        <div class="d-flex">
            <div class="toast-body d-flex align-items-center gap-2">
                <i class="bi ${icon}"></i> <span>${message}</span>
            </div>
            <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>`;
    container.appendChild(el);
    const toast = new bootstrap.Toast(el, { delay: 4000 });
    toast.show();
    el.addEventListener("hidden.bs.toast", () => el.remove());
}

/* ---------- Helper: parse JSON error from fetch response ---------- */
async function lmHandleResponse(res) {
    if (res.ok) return res.json().catch(() => ({}));
    let message = "Something went wrong.";
    try {
        const data = await res.json();
        message = data.error || message;
    } catch (e) { /* ignore */ }
    throw new Error(message);
}

/* ---------- CSRF helper (Disabled) ---------- */
function lmCsrfHeaders() {
    return {};
}

/* ============================================================
   Leaderboard Chart (Admin dashboard)
   ============================================================ */
function lmInitLeaderboardChart(canvasId, labels, counts) {
    const el = document.getElementById(canvasId);
    if (!el || typeof Chart === "undefined") return;
    new Chart(el, {
        type: "bar",
        data: {
            labels: labels,
            datasets: [{
                label: "Times Borrowed",
                data: counts,
                backgroundColor: "#38B2AC",
                borderRadius: 6,
                maxBarThickness: 42
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, ticks: { precision: 0 } },
                x: { grid: { display: false } }
            }
        }
    });
}

/* ============================================================
   Book Search (Member dashboard) — AJAX, debounced
   ============================================================ */
function lmInitBookSearch(inputId, resultsId, opts = {}) {
    const input = document.getElementById(inputId);
    const resultsEl = document.getElementById(resultsId);
    if (!input || !resultsEl) return;

    let debounceTimer;
    const renderResults = (books) => {
        if (!books.length) {
            resultsEl.innerHTML = `<div class="col-12 text-center text-muted py-5">
                <i class="bi bi-search fs-2 d-block mb-2"></i> No books matched your search.
            </div>`;
            return;
        }
        resultsEl.innerHTML = books.map(b => `
            <div class="lm-book-card">
                ${b.coverImageUrl
                    ? `<img src="${b.coverImageUrl}" class="lm-book-cover" alt="${b.title}">`
                    : `<div class="lm-book-cover-placeholder"><i class="bi bi-book"></i></div>`}
                <div class="lm-book-info">
                    <div class="lm-book-title">${b.title}</div>
                    <div class="lm-book-author">${b.author}</div>
                    <span class="badge ${b.availableQuantity > 0 ? 'badge-in-stock' : 'badge-out-stock'} mb-2">
                        ${b.availableQuantity > 0 ? 'In Stock' : 'Out of Stock'}
                    </span>
                    ${opts.allowBorrow !== false ? `
                    <button class="btn btn-lm-cyan btn-sm w-100" ${b.availableQuantity > 0 ? '' : 'disabled'}
                        onclick="lmBorrowBook('${b.id}', this)">
                        <i class="bi bi-bookmark-plus"></i> Borrow
                    </button>` : ''}
                </div>
            </div>
        `).join("");
    };

    const doSearch = (q) => {
        fetch(`/api/books/search?q=${encodeURIComponent(q)}`)
            .then(lmHandleResponse)
            .then(renderResults)
            .catch(err => lmToast(err.message, "error"));
    };

    input.addEventListener("input", () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => doSearch(input.value.trim()), 300);
    });

    window.lmDoBookSearch = doSearch;
    window.lmRenderBookResults = renderResults;

    // Initial load
    doSearch("");
}

function lmBorrowBook(bookId, btn) {
    if (btn) { btn.disabled = true; btn.innerHTML = `<span class="spinner-border spinner-border-sm"></span>`; }
    fetch("/api/transactions/borrow", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...lmCsrfHeaders() },
        body: JSON.stringify({ userId: window.LM_CURRENT_USER_ID, bookId })
    })
        .then(lmHandleResponse)
        .then(() => {
            lmToast("Book borrowed successfully! Due in 14 days.");
            setTimeout(() => window.location.reload(), 900);
        })
        .catch(err => {
            lmToast(err.message, "error");
            if (btn) { btn.disabled = false; btn.innerHTML = `<i class="bi bi-bookmark-plus"></i> Borrow`; }
        });
}

/* ============================================================
   Admin: Book CRUD (gallery/table page)
   ============================================================ */
function lmSubmitBookForm(formEl, isEdit, bookId) {
    const formData = new FormData(formEl);
    const url = isEdit ? `/api/books/${bookId}` : "/api/books";
    const method = isEdit ? "PUT" : "POST";

    fetch(url, { method, headers: { ...lmCsrfHeaders() }, body: formData })
        .then(lmHandleResponse)
        .then(() => {
            lmToast(isEdit ? "Book updated successfully!" : "Book added successfully!");
            setTimeout(() => window.location.reload(), 800);
        })
        .catch(err => lmToast(err.message, "error"));
}

function lmDeleteBook(bookId, title) {
    console.log("[DEBUG] lmDeleteBook called for:", bookId, title);
    if (!confirm(`Delete "${title}"? This cannot be undone.`)) {
        console.log("[DEBUG] lmDeleteBook confirmation cancelled.");
        return;
    }
    console.log("[DEBUG] Redirecting to delete endpoint...");
    window.location.href = `/admin/books/delete/${bookId}`;
}

/* ============================================================
   Admin: Member block/unblock
   ============================================================ */
function lmToggleMemberBlock(userId, makeActive, btn) {
    fetch(`/api/members/${userId}/block?active=${makeActive}`, {
        method: "POST",
        headers: { ...lmCsrfHeaders() }
    })
        .then(lmHandleResponse)
        .then(() => {
            lmToast(makeActive ? "Member unblocked." : "Member blocked.");
            setTimeout(() => window.location.reload(), 700);
        })
        .catch(err => lmToast(err.message, "error"));
}

/* ============================================================
   Admin: Transactions — issue & return
   ============================================================ */
function lmIssueBook(formEl) {
    const userId = formEl.querySelector('[name="userId"]').value;
    const bookId = formEl.querySelector('[name="bookId"]').value;
    if (!userId || !bookId) { lmToast("Select both a member and a book.", "error"); return; }

    const loanPeriodDaysEl = formEl.querySelector('[name="loanPeriodDays"]');
    const loanPeriodDays = loanPeriodDaysEl ? loanPeriodDaysEl.value : null;

    fetch("/api/transactions/borrow", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...lmCsrfHeaders() },
        body: JSON.stringify({ userId, bookId, loanPeriodDays })
    })
        .then(lmHandleResponse)
        .then(() => {
            lmToast("Book issued successfully!");
            setTimeout(() => window.location.reload(), 800);
        })
        .catch(err => lmToast(err.message, "error"));
}

function lmReturnBook(recordId, btn) {
    if (btn) { btn.disabled = true; }
    fetch(`/api/transactions/return/${recordId}`, { method: "POST", headers: { ...lmCsrfHeaders() } })
        .then(lmHandleResponse)
        .then((record) => {
            const fineMsg = record.fineAmount > 0 ? ` Fine: ₹${record.fineAmount.toFixed(2)}` : "";
            lmToast("Book returned." + fineMsg);
            setTimeout(() => window.location.reload(), 900);
        })
        .catch(err => {
            lmToast(err.message, "error");
            if (btn) btn.disabled = false;
        });
}

/* ============================================================
   Member search for Transactions page (admin)
   ============================================================ */
function lmInitMemberSearch(inputId, resultsId, hiddenInputId, labelId) {
    const input = document.getElementById(inputId);
    const resultsEl = document.getElementById(resultsId);
    const hidden = document.getElementById(hiddenInputId);
    const label = document.getElementById(labelId);
    if (!input || !resultsEl) return;

    let debounceTimer;
    input.addEventListener("input", () => {
        clearTimeout(debounceTimer);
        const q = input.value.trim();
        debounceTimer = setTimeout(() => {
            fetch(`/api/members/search?q=${encodeURIComponent(q)}`)
                .then(lmHandleResponse)
                .then(members => {
                    if (!members.length) {
                        resultsEl.innerHTML = `<div class="list-group-item text-muted">No members found.</div>`;
                        resultsEl.classList.remove("d-none");
                        return;
                    }
                    resultsEl.innerHTML = members.map(m => `
                        <button type="button" class="list-group-item list-group-item-action"
                            onclick="lmSelectMember('${m.id}', '${m.fullName.replace(/'/g, "\\'")}', '${m.email}')">
                            <strong>${m.fullName}</strong> <span class="text-muted">${m.email}</span>
                            ${!m.active ? '<span class="badge badge-blocked ms-2">Blocked</span>' : ''}
                        </button>
                    `).join("");
                    resultsEl.classList.remove("d-none");
                })
                .catch(err => lmToast(err.message, "error"));
        }, 300);
    });

    window.lmSelectMember = (id, name, email) => {
        hidden.value = id;
        if (label) label.textContent = `${name} (${email})`;
        input.value = "";
        resultsEl.classList.add("d-none");
        resultsEl.innerHTML = "";
    };
}

/* ============================================================
   Admin: Member Create
   ============================================================ */
function lmSubmitMemberForm(formEl) {
    const formData = new FormData(formEl);
    const data = {};
    formData.forEach((value, key) => data[key] = value);

    const submitBtn = formEl.querySelector('[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Creating...';

    fetch("/api/members", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...lmCsrfHeaders() },
        body: JSON.stringify(data)
    })
        .then(lmHandleResponse)
        .then(() => {
            lmToast("Member created successfully!");
            const modalEl = document.getElementById("addMemberModal");
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            setTimeout(() => window.location.reload(), 800);
        })
        .catch(err => {
            lmToast(err.message, "error");
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        });
}

/* ---------- Bootstrap auto-init for components that need JS hooks ---------- */
document.addEventListener("DOMContentLoaded", () => {
    // Enable Bootstrap tooltips if any are present
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));
});
