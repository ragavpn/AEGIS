// Crucix Configuration — all settings with env var overrides

import "./apis/utils/env.mjs"; // Load .env first

export default {
  port: parseInt(process.env.PORT) || 3117,
  refreshIntervalMinutes: parseInt(process.env.REFRESH_INTERVAL_MINUTES) || 15,

  // LLM config — mirrors aegis-backend's approach:
  // Just set LLM_API_KEY + (optionally) LLM_BASE_URL and LLM_MODEL.
  // LLM_PROVIDER is optional — defaults to 'openai' (OpenAI-compatible) when LLM_API_KEY is present.
  // Set LLM_PROVIDER explicitly only for non-OpenAI-compatible providers (anthropic, gemini, codex, etc.)
  llm: {
    provider: process.env.LLM_PROVIDER
      || (process.env.LLM_API_KEY ? 'openai' : null), // auto-detect: API key present → OpenAI-compatible
    apiKey: process.env.LLM_API_KEY || null,
    model: process.env.LLM_MODEL || 'gpt-4o',
    // Defaults to GitHub Models if no base URL is set — same as aegis-backend
    baseUrl: process.env.LLM_BASE_URL
      || process.env.OLLAMA_BASE_URL
      || 'https://models.inference.ai.azure.com/chat/completions',
  },

  telegram: {
    botToken: process.env.TELEGRAM_BOT_TOKEN || null,
    chatId: process.env.TELEGRAM_CHAT_ID || null,
    botPollingInterval: parseInt(process.env.TELEGRAM_POLL_INTERVAL) || 5000,
    channels: process.env.TELEGRAM_CHANNELS || null, // Comma-separated extra channel IDs
  },

  discord: {
    botToken: process.env.DISCORD_BOT_TOKEN || null,
    channelId: process.env.DISCORD_CHANNEL_ID || null,
    guildId: process.env.DISCORD_GUILD_ID || null, // Server ID (for instant slash command registration)
    webhookUrl: process.env.DISCORD_WEBHOOK_URL || null, // Fallback: webhook-only alerts (no bot needed)
  },

  // Delta engine thresholds — override defaults from lib/delta/engine.mjs
  // Set to null to use built-in defaults
  delta: {
    thresholds: {
      numeric: {
        // Example overrides (uncomment to customize):
        // vix: 3,       // more sensitive to VIX moves
        // wti: 5,       // less sensitive to oil moves
      },
      count: {
        // urgent_posts: 3,     // need ±3 urgent posts to flag
        // thermal_total: 1000, // need ±1000 thermal detections
      },
    },
  },
};
