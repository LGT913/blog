<script setup>
import { useSiteStore } from '../store/site'

const siteStore = useSiteStore()
</script>

<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand">
          <span class="footer-logo">✦</span>
          <span class="footer-site-name">{{ siteStore.state.config.siteName }}</span>
        </div>
        <p class="footer-desc">{{ siteStore.state.config.siteDesc }}</p>
      </div>
      <div class="footer-bottom">
        <div class="footer-copyright">
          &copy; {{ new Date().getFullYear() }} {{ siteStore.state.config.siteName }}.
          {{ siteStore.state.config.copyright }}
        </div>
        <div class="footer-links">
          <span v-if="siteStore.state.config.recordNo" class="footer-link">
            {{ siteStore.state.config.recordNo }}
          </span>
          <span v-if="siteStore.state.config.email" class="footer-link">
            {{ siteStore.state.config.email }}
          </span>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: auto;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg);
}

.footer-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 32px var(--space-6) 24px;
}

.footer-top {
  text-align: center;
  margin-bottom: 24px;
}

.footer-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
}

.footer-logo {
  font-size: 18px;
  color: var(--color-primary);
}

.footer-site-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.footer-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  max-width: 400px;
  margin: 0 auto;
  line-height: 1.6;
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
}

.footer-copyright {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-link {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}
</style>
