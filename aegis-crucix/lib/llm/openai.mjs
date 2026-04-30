// OpenAI Provider — raw fetch, no SDK
// When baseUrl points to GitHub Models (models.inference.ai.azure.com),
// automatically falls back through GITHUB_MODELS_FALLBACK_CHAIN on 429s.

import { LLMProvider } from './provider.mjs';

const GITHUB_MODELS_BASE_URL = 'https://models.inference.ai.azure.com/chat/completions';

// Curated fallback chain — ordered by capability & availability on GitHub Marketplace
const GITHUB_MODELS_FALLBACK_CHAIN = [
  'gpt-4o',
  'gpt-4o-mini',
  'o1-mini',
  'Meta-Llama-3.3-70B-Instruct',
  'Mistral-Large-2411',
  'Phi-4',
  'Cohere-command-r-plus-08-2024',
  'AI21-Jamba-1.5-Large',
  'Meta-Llama-3.1-405B-Instruct',
];

export class OpenAIProvider extends LLMProvider {
  constructor(config) {
    super(config);
    this.name = 'openai';
    this.apiKey = config.apiKey;
    this.model = config.model || 'gpt-4o';
    this.baseUrl = config.baseUrl || 'https://api.openai.com/v1/chat/completions';
    this._isGithubModels = this.baseUrl === GITHUB_MODELS_BASE_URL;
  }

  get isConfigured() { return !!this.apiKey; }

  async complete(systemPrompt, userMessage, opts = {}) {
    const messages = [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userMessage },
    ];
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.apiKey}`,
    };

    // Build ordered list of models to try
    const modelsToTry = this._isGithubModels
      ? [this.model, ...GITHUB_MODELS_FALLBACK_CHAIN.filter(m => m !== this.model)]
      : [this.model];

    let lastError = null;

    for (const model of modelsToTry) {
      console.log(`[LLM] Calling model: ${model}`);

      const res = await fetch(this.baseUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          model,
          max_completion_tokens: opts.maxTokens || 4096,
          messages,
        }),
        signal: AbortSignal.timeout(opts.timeout || 60000),
      });

      // Rate-limited — try next model in chain
      if (res.status === 429 && this._isGithubModels) {
        const retryAfter = res.headers.get('retry-after') || '?';
        console.warn(`[LLM] Rate limit hit on "${model}" (retry-after: ${retryAfter}s). Trying next model...`);
        lastError = new Error(`Rate limited on ${model}`);
        continue;
      }

      if (!res.ok) {
        const err = await res.text().catch(() => '');
        throw new Error(`OpenAI API ${res.status}: ${err.substring(0, 200)}`);
      }

      const data = await res.json();
      const text = data.choices?.[0]?.message?.content || '';
      console.log(`[LLM] Response received from "${model}" (${text.length} chars)`);

      return {
        text,
        usage: {
          inputTokens: data.usage?.prompt_tokens || 0,
          outputTokens: data.usage?.completion_tokens || 0,
        },
        model: data.model || model,
      };
    }

    throw lastError || new Error('All GitHub Models exhausted without a successful response');
  }
}
