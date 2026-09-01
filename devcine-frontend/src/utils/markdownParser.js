/**
 * Parser & Sanitizer hỗ trợ cả HTML trực tiếp (WYSIWYG) và cú pháp Markdown cho tin khuyến mãi
 */
export function parseMarkdownToHtml(content = '') {
  if (!content) return ''

  // Nếu content đã là HTML (từ WYSIWYG editor)
  const isHtml = /<(?:p|div|h[1-6]|ul|ol|li|img|blockquote|strong|em|u|b|i|a|hr|br)[^>]*>/i.test(content)
  if (isHtml) {
    // Nếu là HTML, đảm bảo các thẻ hình ảnh và liên kết có class chuẩn đẹp mắt
    let html = content
      // Chống mã độc script/iframe
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/on\w+="[^"]*"/gi, '')
      .replace(/on\w+='[^']*'/gi, '')
    return html
  }

  // Nếu content là Markdown (tin cũ hoặc nhập text thô)
  let text = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

  // Escape HTML cơ bản để chống XSS (trừ tag <u>, </u>)
  text = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/&lt;u&gt;/gi, '<u>')
    .replace(/&lt;\/u&gt;/gi, '</u>')

  // 1. Hình ảnh: ![alt](url)
  text = text.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (match, alt, url) => {
    const cleanUrl = url.trim()
    const cleanAlt = alt.trim() || 'Hình ảnh'
    return `<div class="my-6 text-center">
      <img src="${cleanUrl}" alt="${cleanAlt}" class="rounded-2xl max-w-full max-h-[480px] mx-auto object-cover border border-outline-variant/15 shadow-xl hover:scale-[1.01] transition-transform" loading="lazy" />
      ${cleanAlt && cleanAlt !== 'Hình ảnh' ? `<p class="text-xs text-on-surface-variant/70 italic mt-2">${cleanAlt}</p>` : ''}
    </div>`
  })

  // 2. Liên kết: [text](url)
  text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (match, linkText, url) => {
    const cleanUrl = url.trim()
    const isExternal = cleanUrl.startsWith('http') || cleanUrl.startsWith('//')
    return `<a href="${cleanUrl}" ${isExternal ? 'target="_blank" rel="noopener noreferrer"' : ''} class="text-primary hover:text-primary/80 font-bold underline decoration-primary/40 underline-offset-4 transition-colors">${linkText}</a>`
  })

  // 3. Tiêu đề (Headings)
  text = text.replace(/^### (.*$)/gim, '<h3 class="text-lg sm:text-xl font-bold text-on-surface uppercase tracking-wide mt-6 mb-3 flex items-center gap-2"><span class="w-1.5 h-4 rounded-full bg-primary inline-block"></span>$1</h3>')
  text = text.replace(/^## (.*$)/gim, '<h2 class="text-xl sm:text-2xl font-black text-on-surface uppercase tracking-tight mt-8 mb-4 pb-2 border-b border-outline-variant/15">$1</h2>')
  text = text.replace(/^# (.*$)/gim, '<h1 class="text-2xl sm:text-3xl font-black text-primary uppercase tracking-tight mt-8 mb-4">$1</h1>')

  // 4. Định dạng chữ: In đậm, In nghiêng
  text = text.replace(/\*\*(.*?)\*\*/g, '<strong class="font-bold text-on-surface">$1</strong>')
  text = text.replace(/__(.*?)__/g, '<strong class="font-bold text-on-surface">$1</strong>')
  text = text.replace(/\*(.*?)\*/g, '<em class="italic text-on-surface/90">$1</em>')
  text = text.replace(/_(.*?)_/g, '<em class="italic text-on-surface/90">$1</em>')

  // 5. Đường kẻ phân cách
  text = text.replace(/^(?:---|\*\*\*|___)$/gim, '<hr class="my-6 border-outline-variant/20" />')

  // 6. Trích dẫn (Blockquote)
  text = text.replace(/^> (.*$)/gim, '<blockquote class="my-4 pl-4 border-l-4 border-primary/60 italic text-on-surface-variant bg-primary/5 py-2 px-3 rounded-r-lg">$1</blockquote>')

  // 7. Danh sách gạch đầu dòng & số thứ tự
  const lines = text.split('\n')
  let inList = false
  const processedLines = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const listMatch = line.match(/^(\s*)(?:[-*•]|\d+\.)\s+(.+)$/)

    if (listMatch) {
      if (!inList) {
        processedLines.push('<ul class="my-3 space-y-2 pl-4 list-disc marker:text-primary">')
        inList = true
      }
      processedLines.push(`<li class="text-on-surface-variant pl-1">${listMatch[2]}</li>`)
    } else {
      if (inList) {
        processedLines.push('</ul>')
        inList = false
      }
      processedLines.push(line)
    }
  }
  if (inList) {
    processedLines.push('</ul>')
  }

  // 8. Xử lý đoạn văn
  let result = processedLines.join('\n')
  result = result.replace(/\n\n+/g, '<div class="h-4"></div>')
  result = result.replace(/(?<!<\/h[1-3]>|<\/div>|<\/ul>|<\/li>|<\/blockquote>|<\/hr>)\n(?!<h[1-3]|<div|<ul|<li|<blockquote|<hr)/g, '<br/>')

  return result
}
