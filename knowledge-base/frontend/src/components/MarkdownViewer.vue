<template>
  <div class="markdown-body" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

const props = defineProps({ content: { type: String, default: '' } })

const md = new MarkdownIt({
  html: false, linkify: true, typographer: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try { return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>` } catch {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

/** 渲染 Markdown 并对输出做 HTML 消毒，防止 XSS */
const renderedHtml = computed(() => {
  const raw = md.render(props.content)
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: [
      'h1','h2','h3','h4','h5','h6','p','br','hr','blockquote','pre','code',
      'ul','ol','li','a','strong','em','b','i','s','del','ins','mark','sub','sup',
      'table','thead','tbody','tr','th','td','img','details','summary','span','div'
    ],
    ALLOWED_ATTR: ['src','alt','title','width','height','href','target','rel','class','align'],
    ALLOW_DATA_ATTR: false
  })
})
</script>

<style>
@import 'highlight.js/styles/atom-one-dark.css';

.markdown-body { font-size: 12px; line-height: 1.75; color: #292524; }

.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
  margin-top: 1.4em; margin-bottom: 0.4em; font-weight: 600; color: #1c1917;
}
.markdown-body h1 { font-size: 1.4em; border-bottom: 1px solid #ede7e0; padding-bottom: 10px; }
.markdown-body h2 { font-size: 1.2em; border-bottom: 1px solid #ede7e0; padding-bottom: 8px; }
.markdown-body h3 { font-size: 1.05em; }

.markdown-body p { margin: 0.8em 0; }

.markdown-body code {
  padding: 2px 6px; background: #faf8f5; border: 1px solid #e7e0d8;
  border-radius: 4px; font-size: 0.88em; color: #db2777;
}

.markdown-body pre { margin: 1em 0; border-radius: 6px; overflow-x: auto; }
.markdown-body pre code { display: block; padding: 16px; background: #fdf8f3; border: 1px solid #ede7e0; color: #292524; font-size: 13px; line-height: 1.6; border-radius: 6px; }

.markdown-body blockquote {
  margin: 1em 0; padding: 10px 16px;
  border-left: 3px solid #d97706; color: #8c847c; background: #fef7ed;
  border-radius: 0 4px 4px 0;
}

.markdown-body table { width: 100%; border-collapse: collapse; margin: 1em 0; }
.markdown-body th, .markdown-body td {
  border: 1px solid #e7e0d8; padding: 8px 12px; text-align: left; font-size: 14px;
}
.markdown-body th { background: #fdf8f3; font-weight: 600; color: #1c1917; }

.markdown-body img { max-width: 100%; border-radius: 6px; }
.markdown-body a { color: #d97706; text-decoration: none; }
.markdown-body a:hover { text-decoration: underline; }
.markdown-body ul, .markdown-body ol { padding-left: 1.6em; margin: 0.8em 0; }
.markdown-body hr { border: none; border-top: 1px solid #e7e0d8; margin: 1.5em 0; }
.markdown-body strong { color: #1c1917; }
</style>
