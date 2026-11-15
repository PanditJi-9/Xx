/* MaxManager site — small, dependency-free enhancements.
   Everything here is optional: links open the full capture without JS,
   and the gallery is complete before this file runs. */
(function () {
  "use strict";

  var root = document.documentElement;
  var toggle = document.querySelector("[data-theme-toggle]");
  var stored = null;

  try {
    stored = localStorage.getItem("mm-theme");
  } catch (error) {
    stored = null;
  }

  function current() {
    if (root.getAttribute("data-theme")) return root.getAttribute("data-theme");
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches
      ? "light"
      : "dark";
  }

  function paint(theme) {
    root.setAttribute("data-theme", theme);
    if (!toggle) return;
    var toLight = theme === "dark";
    toggle.setAttribute("aria-label", toLight ? "Switch to light theme" : "Switch to dark theme");
    toggle.setAttribute("title", toLight ? "Light theme" : "Dark theme");
  }

  if (stored === "light" || stored === "dark") paint(stored);

  if (toggle) {
    toggle.addEventListener("click", function () {
      var next = current() === "dark" ? "light" : "dark";
      paint(next);
      try {
        localStorage.setItem("mm-theme", next);
      } catch (error) {
        /* private mode: the choice simply lasts for this page */
      }
    });
  }

  /* Gallery filter — buttons only exist to narrow a list that is fully
     visible without them, so a failed script leaves a usable page. */
  var bar = document.querySelector("[data-filters]");
  var shots = Array.prototype.slice.call(document.querySelectorAll("[data-shot-group]"));

  if (bar && shots.length) {
    var buttons = Array.prototype.slice.call(bar.querySelectorAll("button[data-filter]"));

    bar.hidden = false;

    buttons.forEach(function (button) {
      button.addEventListener("click", function () {
        var filter = button.getAttribute("data-filter");

        buttons.forEach(function (other) {
          other.setAttribute("aria-pressed", String(other === button));
        });

        shots.forEach(function (shot) {
          shot.hidden = filter !== "all" && shot.getAttribute("data-shot-group") !== filter;
        });
      });
    });
  }

  /* Lightbox — a real <dialog>, so Escape and focus handling are native. */
  var dialog = document.getElementById("shot-dialog");
  var dialogImg = document.getElementById("shot-dialog-img");
  var dialogCap = document.getElementById("shot-dialog-cap");
  var triggers = Array.prototype.slice.call(document.querySelectorAll("[data-lightbox]"));

  if (dialog && dialogImg && dialog.showModal) {
    triggers.forEach(function (link) {
      link.addEventListener("click", function (event) {
        var img = link.querySelector("img");
        var caption = link.getAttribute("data-caption") || "";

        event.preventDefault();
        dialogImg.src = link.getAttribute("href");
        dialogImg.alt = img ? img.alt : caption;
        if (dialogCap) dialogCap.textContent = caption;
        dialog.showModal();
      });
    });

    dialog.addEventListener("click", function (event) {
      if (event.target === dialog) dialog.close();
    });
  }
})();
