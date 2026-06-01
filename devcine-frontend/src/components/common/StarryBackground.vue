<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const { ambientColor, isWarping } = storeToRefs(themeStore)

const props = defineProps({
  density: {
    type: String,
    default: 'dense'
  },
  theme: {
    type: String,
    default: 'customer' // 'customer' or 'admin'
  }
})

const starColors = [
  'rgba(255, 255, 255, ',
  'rgba(220, 235, 255, ',
  'rgba(255, 240, 210, ',
  'rgba(255, 255, 255, '
]

const generateStars = (count, withGlow = false) => {
  let shadow = []
  for (let i = 0; i < count; i++) {
    const x = Math.floor(Math.random() * 3500)
    const y = Math.floor(Math.random() * 3500)
    const opacity = (Math.random() * 0.7 + 0.3).toFixed(2)
    const colorBase = starColors[Math.floor(Math.random() * starColors.length)]
    
    let shadowString = `${x}px ${y}px ${colorBase}${opacity})`
    if (withGlow) {
      shadowString += `, ${x}px ${y}px 6px ${colorBase}${(opacity * 0.5).toFixed(2)})`
    }
    shadow.push(shadowString)
  }
  return shadow.join(', ')
}

const counts = computed(() => {
  if (props.density === 'dense') return { small: 800, medium: 250, large: 60 }
  return { small: 300, medium: 100, large: 20 }
})

const shadowsSmall = computed(() => generateStars(counts.value.small))
const shadowsMedium = computed(() => generateStars(counts.value.medium))
const shadowsLarge = computed(() => generateStars(counts.value.large, true))

const scrollY = ref(0)
const mouseX = ref(window.innerWidth / 2)
const mouseY = ref(window.innerHeight / 2)

const onScroll = () => {
  scrollY.value = window.scrollY
}

const onMouseMove = (e) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('mousemove', onMouseMove, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('mousemove', onMouseMove)
})

const transformS = computed(() => `translateY(${-scrollY.value * 0.15}px)`)
const transformM = computed(() => `translateY(${-scrollY.value * 0.35}px)`)
const transformL = computed(() => `translateY(${-scrollY.value * 0.8}px)`)
const transformShooting = computed(() => `rotate(-35deg) translateY(${-scrollY.value * 1.5}px)`)
const transformMouseGlow = computed(() => `translate(${mouseX.value}px, ${mouseY.value}px) translate(-50%, -50%)`)

// Ánh sáng tỏa ra từ toàn bộ nền khi có ambientColor
const ambientGlowStyle = computed(() => {
  if (ambientColor.value) {
    return {
      backgroundColor: ambientColor.value,
      opacity: 0.15 // Giữ độ trong suốt thấp để không làm mất vẻ đẹp của nền đen
    }
  }
  return {
    backgroundColor: 'transparent',
    opacity: 0
  }
})

</script>

<template>
  <div class="galaxy-wrapper pointer-events-none">
    <div :class="['nebula-bg', props.theme === 'admin' ? 'nebula-admin' : 'nebula-customer']"></div>
    <div class="ambient-glow" :style="ambientGlowStyle"></div>
    
    <div class="stars-container" :class="{ 'warp-active': isWarping }">
      <div class="parallax-layer" :style="{ transform: transformS }">
        <div class="stars stars-s"></div>
      </div>
      <div class="parallax-layer" :style="{ transform: transformM }">
        <div class="stars stars-m"></div>
      </div>
      <div class="parallax-layer" :style="{ transform: transformL }">
        <div class="stars stars-l"></div>
      </div>
    </div>
    
    <!-- Lớp sao băng (Shooting Stars) -->
    <div class="shooting-stars-layer" :style="{ transform: transformShooting }">
      <div class="shooting-star" style="top: 15%; left: 85%; animation-delay: 0s;"></div>
      <div class="shooting-star" style="top: 40%; left: 95%; animation-delay: 4.5s;"></div>
      <div class="shooting-star" style="top: 5%; left: 60%; animation-delay: 8.2s;"></div>
      <div class="shooting-star" style="top: 60%; left: 80%; animation-delay: 11s;"></div>
      <div class="shooting-star" style="top: 80%; left: 90%; animation-delay: 17s;"></div>
    </div>

    <!-- Ánh sáng tương tác theo chuột -->
    <div class="mouse-glow" :style="{ transform: transformMouseGlow }"></div>
  </div>
</template>

<style scoped>
.galaxy-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -10;
  background-color: #030304;
  overflow: hidden;
}

.nebula-bg {
  position: absolute;
  inset: 0;
  filter: blur(50px);
  animation: pulseNebula 25s ease-in-out infinite alternate;
}

.nebula-customer {
  background: 
    radial-gradient(circle at 15% 25%, rgba(20, 30, 45, 0.15) 0%, transparent 40%),
    radial-gradient(circle at 85% 65%, rgba(25, 15, 30, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(30, 35, 45, 0.08) 0%, transparent 70%);
}

.nebula-admin {
  background: transparent;
}

/* LỚP HẮT SÁNG AMBIENT TỪ POSTER (CÓ TRANSITION MƯỢT) */
.ambient-glow {
  position: absolute;
  inset: 0;
  transition: background-color 1s ease-in-out, opacity 1s ease-in-out;
  mix-blend-mode: screen;
}

/* ÁNH SÁNG TƯƠNG TÁC THEO CHUỘT */
.mouse-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(100, 150, 255, 0.015) 0%, transparent 60%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 20; /* Nằm trên cùng của nền vũ trụ */
  will-change: transform;
}

@keyframes pulseNebula {
  0% { transform: scale(1) translate(0, 0); opacity: 0.7; }
  100% { transform: scale(1.15) translate(-2%, 2%); opacity: 1; }
}

.stars-container {
  position: absolute;
  width: 100%;
  height: 200%;
  top: -50%;
  left: 0;
  transform: rotate(-8deg); 
  transition: transform 0.8s cubic-bezier(0.5, 0, 0.1, 1), filter 0.8s;
  z-index: 5;
}

/* WARP SPEED EFFECT (BƯỚC NHẢY KHÔNG GIAN) */
.warp-active {
  transform: rotate(-8deg) scaleY(40) !important;
  filter: brightness(2.5) drop-shadow(0 0 10px rgba(255, 255, 255, 0.8));
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  background: transparent;
}

.stars-s {
  width: 1px;
  height: 1px;
  box-shadow: v-bind(shadowsSmall);
  animation: animStar 160s linear infinite, twinkle 4s ease-in-out infinite alternate;
}

.stars-m {
  width: 2px;
  height: 2px;
  border-radius: 50%;
  box-shadow: v-bind(shadowsMedium);
  animation: animStar 120s linear infinite, twinkle 6s ease-in-out infinite alternate;
}

.stars-l {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  box-shadow: v-bind(shadowsLarge);
  animation: animStar 80s linear infinite, twinkle 7s ease-in-out infinite alternate;
}

.shooting-stars-layer {
  position: absolute;
  inset: 0;
  transform: rotate(-35deg);
  z-index: 10;
}

.shooting-star {
  position: absolute;
  height: 1px;
  background: linear-gradient(90deg, transparent, #ffffff);
  border-radius: 999px;
  animation: shootingStarAnim 20s linear infinite;
  opacity: 0;
}

.shooting-star::before {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 3px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 
    0 0 10px 2px rgba(255, 255, 255, 0.9), 
    0 0 20px 5px rgba(220, 235, 255, 0.4);
}

@keyframes shootingStarAnim {
  0% { transform: translateX(0); width: 0; opacity: 0; }
  2% { width: 250px; opacity: 1; }
  8% { transform: translateX(-2000px); width: 0; opacity: 0; }
  100% { transform: translateX(-2000px); width: 0; opacity: 0; }
}

@keyframes animStar {
  from { transform: translateY(0); }
  to { transform: translateY(-1500px); }
}

@keyframes twinkle {
  0% { opacity: 0.5; }
  30% { opacity: 1; }
  70% { opacity: 0.2; }
  100% { opacity: 0.8; }
}
</style>
