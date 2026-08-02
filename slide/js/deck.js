/* =============================================================
   DevCine Deck — điều hướng & hiệu ứng
   ============================================================= */
(function () {
  "use strict";

  /* ---------- Sinh nền sao (tái hiện StarryBackground.vue) ---------- */
  function buildStars() {
    var layers = [
      { cls: "s", count: 260 },
      { cls: "m", count: 90 },
      { cls: "l", count: 26 }
    ];
    var colors = [
      "rgba(255,255,255,",
      "rgba(220,235,255,",
      "rgba(255,240,210,"
    ];
    var frag = document.createDocumentFragment();
    layers.forEach(function (layer) {
      for (var i = 0; i < layer.count; i++) {
        var span = document.createElement("span");
        span.className = layer.cls;
        var op = (Math.random() * 0.7 + 0.3).toFixed(2);
        var color = colors[(Math.random() * colors.length) | 0] + op + ")";
        span.style.left = (Math.random() * 100) + "%";
        span.style.top = (Math.random() * 100) + "%";
        span.style.background = color;
        span.style.boxShadow = "0 0 6px " + color;
        span.style.animationDelay = (Math.random() * -160) + "s, " + (Math.random() * -7) + "s";
        frag.appendChild(span);
      }
    });
    var host = document.getElementById("stars");
    if (host) host.appendChild(frag);

    // Shooting stars
    var shooting = document.getElementById("shooting");
    if (shooting) {
      var positions = [
        { top: "15%", left: "85%", delay: "0s" },
        { top: "40%", left: "95%", delay: "4.5s" },
        { top: "5%",  left: "60%", delay: "8.2s" },
        { top: "60%", left: "80%", delay: "11s" },
        { top: "80%", left: "90%", delay: "17s" }
      ];
      positions.forEach(function (p) {
        var i = document.createElement("i");
        i.style.top = p.top; i.style.left = p.left; i.style.animationDelay = p.delay;
        shooting.appendChild(i);
      });
    }
  }

  /* ---------- Điều hướng slide ---------- */
  var slides = Array.prototype.slice.call(document.querySelectorAll(".slide"));
  var total = slides.length;
  var current = 0;
  var animating = false;

  var progress = document.getElementById("progress");
  var counterNow = document.getElementById("counter-now");
  var counterTotal = document.getElementById("counter-total");
  var dotsWrap = document.getElementById("dots");
  var prevBtn = document.getElementById("prev");
  var nextBtn = document.getElementById("next");
  var hint = document.getElementById("hint");

  if (counterTotal) counterTotal.textContent = String(total).padStart(2, "0");

  // Build dots
  slides.forEach(function (_, idx) {
    var b = document.createElement("button");
    b.className = "dot";
    b.setAttribute("aria-label", "Slide " + (idx + 1));
    b.addEventListener("click", function () { goTo(idx); });
    dotsWrap.appendChild(b);
  });
  var dots = Array.prototype.slice.call(dotsWrap.children);

  function applyStaggerIndex(slide) {
    var items = slide.querySelectorAll("[data-stagger]");
    items.forEach(function (el, i) { el.style.setProperty("--i", i); });
  }
  slides.forEach(applyStaggerIndex);

  function render(prevIndex) {
    slides.forEach(function (s, idx) {
      s.classList.remove("active", "leaving");
      if (idx === current) s.classList.add("active");
      else if (idx === prevIndex) s.classList.add("leaving");
    });
    dots.forEach(function (d, idx) { d.classList.toggle("active", idx === current); });

    if (progress) progress.style.width = ((current) / (total - 1) * 100) + "%";
    if (counterNow) counterNow.textContent = String(current + 1).padStart(2, "0");
    if (prevBtn) prevBtn.classList.toggle("hide", current === 0);
    if (nextBtn) nextBtn.classList.toggle("hide", current === total - 1);
  }

  function goTo(index) {
    if (animating || index === current) return;
    index = Math.max(0, Math.min(total - 1, index));
    if (index === current) return;
    animating = true;
    var prevIndex = current;
    current = index;
    render(prevIndex);
    window.setTimeout(function () { animating = false; }, 650);
    fadeHint();
  }

  function next() { goTo(current + 1); }
  function prev() { goTo(current - 1); }

  /* ---------- Keyboard ---------- */
  document.addEventListener("keydown", function (e) {
    switch (e.key) {
      case "ArrowRight":
      case "ArrowDown":
      case " ":
      case "PageDown":
        e.preventDefault(); next(); break;
      case "ArrowLeft":
      case "ArrowUp":
      case "PageUp":
        e.preventDefault(); prev(); break;
      case "Home": e.preventDefault(); goTo(0); break;
      case "End": e.preventDefault(); goTo(total - 1); break;
      case "f":
      case "F":
        toggleFullscreen(); break;
    }
  });

  /* ---------- Click hai mép màn hình ---------- */
  if (prevBtn) prevBtn.addEventListener("click", prev);
  if (nextBtn) nextBtn.addEventListener("click", next);

  /* ---------- Wheel (cuộn chuột nhẹ) ---------- */
  var wheelLock = false;
  window.addEventListener("wheel", function (e) {
    if (wheelLock || Math.abs(e.deltaY) < 24) return;
    wheelLock = true;
    if (e.deltaY > 0) next(); else prev();
    window.setTimeout(function () { wheelLock = false; }, 800);
  }, { passive: true });

  /* ---------- Swipe (touch) ---------- */
  var touchX = 0, touchY = 0;
  window.addEventListener("touchstart", function (e) {
    touchX = e.changedTouches[0].clientX;
    touchY = e.changedTouches[0].clientY;
  }, { passive: true });
  window.addEventListener("touchend", function (e) {
    var dx = e.changedTouches[0].clientX - touchX;
    var dy = e.changedTouches[0].clientY - touchY;
    if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy)) {
      if (dx < 0) next(); else prev();
    }
  }, { passive: true });

  /* ---------- Fullscreen ---------- */
  function toggleFullscreen() {
    if (!document.fullscreenElement) {
      (document.documentElement.requestFullscreen || function () {}).call(document.documentElement);
    } else {
      (document.exitFullscreen || function () {}).call(document);
    }
  }

  /* ---------- Ẩn hint sau tương tác ---------- */
  var hintFaded = false;
  function fadeHint() {
    if (hintFaded || !hint) return;
    hintFaded = true;
    hint.classList.add("fade");
  }
  window.setTimeout(fadeHint, 6000);

  /* ---------- Parallax nhẹ cho lớp sao theo chuột ---------- */
  var starsEl = document.getElementById("stars");
  window.addEventListener("mousemove", function (e) {
    if (!starsEl) return;
    var x = (e.clientX / window.innerWidth - 0.5) * 12;
    var y = (e.clientY / window.innerHeight - 0.5) * 12;
    starsEl.style.transform = "rotate(-8deg) translate(" + x + "px," + y + "px)";
  }, { passive: true });

  /* ---------- Init ---------- */
  buildStars();
  render(-1);
})();
