// src/utils/markdown.js
import { marked } from 'marked';
import hljs from 'highlight.js';
import DOMPurify from 'dompurify';
import 'highlight.js/styles/atom-one-dark.css'; // 引入极客暗黑代码高亮主题

// 1. 初始化配置 marked 解析器
marked.setOptions({
    highlight: function(code, lang) {
        const language = hljs.getLanguage(lang) ? lang : 'plaintext';
        return hljs.highlight(code, { language }).value;
    },
    breaks: true // 允许将回车换行符真正解析为 <br>
});

/**
 * 安全的将 AI 文本渲染成带高亮和角标的 HTML 字符串
 * @param {string} text AI 传回的纯文本
 * @returns {string} 安全的富文本 HTML
 */
export const renderAiMessage = (text) => {
    if (!text) return '';

    // 步骤 A: 将文本中可能的引用角标 [1], [2] 动态转化为带特征的 span 标签
    const preProcessed = text.replace(/\[(\d+)\]/g, '<span class="citation-badge" data-id="$1">$1</span>');

    // 步骤 B: 使用 marked 将 Markdown 语法转换成标准 HTML 树
    const rawHtml = marked(preProcessed);

    // 步骤 C: XSS 净化。极其关键：必须通过 ADD_ATTR 允许保留 data-id 属性，否则点击跳转功能会被杀掉！
    return DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['data-id'] });
};