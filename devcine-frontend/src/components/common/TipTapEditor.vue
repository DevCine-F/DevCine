<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import api from '@/api/axios'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: 'Nhập nội dung chi tiết bài viết tại đây...'
  },
  maxlength: {
    type: Number,
    default: 10000
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'blur', 'change'])
const toastStore = useToastStore()

// State cho Upload / Modal
const isUploadingImage = ref(false)
const fileInputRef = ref(null)
const isLinkModalOpen = ref(false)
const linkForm = ref({ text: '', url: 'https://' })
const isImageModalOpen = ref(false)
const imageForm = ref({ alt: '', url: 'https://' })

const editor = useEditor({
  content: props.modelValue || '',
  editable: !props.disabled,
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [2, 3]
      }
    }),
    Underline,
    Image.configure({
      inline: false,
      allowBase64: true,
      HTMLAttributes: {
        class: 'rounded-xl max-w-full max-h-[420px] mx-auto object-cover my-4 shadow-xl border border-white/10'
      }
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        class: 'text-primary underline font-bold hover:text-primary/80 transition-colors',
        target: '_blank',
        rel: 'noopener noreferrer'
      }
    }),
    Placeholder.configure({
      placeholder: props.placeholder
    })
  ],
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    // Nếu rỗng chỉ có tag rỗng <p></p>
    const cleanHtml = editor.isEmpty ? '' : html
    emit('update:modelValue', cleanHtml)
    emit('change', cleanHtml)
  },
  onBlur: () => {
    emit('blur')
  }
})

// Sync khi modelValue từ ngoài thay đổi (ví dụ khi load dữ liệu sửa bài)
watch(() => props.modelValue, (newVal) => {
  if (!editor.value) return
  const currentHtml = editor.value.getHTML()
  const isSame = (newVal === currentHtml) || (editor.value.isEmpty && !newVal)
  if (!isSame) {
    editor.value.commands.setContent(newVal || '', false)
  }
})

watch(() => props.disabled, (newVal) => {
  if (editor.value) {
    editor.value.setEditable(!newVal)
  }
})

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.destroy()
  }
})

// Các hàm xử lý Toolbar
const triggerImageUpload = () => {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
    fileInputRef.value.click()
  }
}

const handleFileUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  let prepared
  try {
    prepared = await prepareImageForUpload(file)
  } catch (err) {
    toastStore.error(friendlyError(err, 'Ảnh không hợp lệ.'))
    e.target.value = ''
    return
  }

  isUploadingImage.value = true
  try {
    const formData = new FormData()
    formData.append('file', prepared)
    const { data } = await api.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const uploadedUrl = data?.data?.url || data?.url || (typeof data?.data === 'string' ? data.data : '')
    if (!uploadedUrl) throw new Error('Không nhận được đường dẫn ảnh từ máy chủ')

    const altText = file.name ? file.name.replace(/\.[^/.]+$/, '') : 'Hình ảnh'
    editor.value?.chain().focus().setImage({ src: uploadedUrl, alt: altText }).run()
    toastStore.success('Đã tải ảnh lên và chèn vào bài viết!')
  } catch (err) {
    toastStore.error(friendlyError(err, 'Tải ảnh thất bại.'))
  } finally {
    isUploadingImage.value = false
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

const openLinkDialog = () => {
  if (!editor.value) return
  const previousUrl = editor.value.getAttributes('link').href || 'https://'
  const { from, to } = editor.value.state.selection
  const selectedText = editor.value.state.doc.textBetween(from, to, ' ')

  linkForm.value = {
    text: selectedText || '',
    url: previousUrl
  }
  isLinkModalOpen.value = true
}

const submitInsertLink = () => {
  if (!editor.value) return
  const url = (linkForm.value.url || '').trim()
  if (!url || url === 'https://') {
    // Nếu để trống thì xóa link
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    isLinkModalOpen.value = false
    return
  }

  if (linkForm.value.text && editor.value.state.selection.empty) {
    editor.value.chain().focus().insertContent(`<a href="${url}">${linkForm.value.text}</a>`).run()
  } else {
    editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  }
  isLinkModalOpen.value = false
}

const openImageDialog = () => {
  imageForm.value = { alt: '', url: 'https://' }
  isImageModalOpen.value = true
}

const submitInsertImageUrl = () => {
  if (!editor.value) return
  const url = (imageForm.value.url || '').trim()
  if (!url || url === 'https://') {
    toastStore.warning('Vui lòng nhập đường dẫn URL ảnh hợp lệ.')
    return
  }
  editor.value.chain().focus().setImage({ src: url, alt: imageForm.value.alt || 'Hình ảnh' }).run()
  isImageModalOpen.value = false
}
</script>

<template>
  <div class="tiptap-wrapper bg-surface-container-highest border rounded-xl overflow-hidden flex flex-col transition-colors border-outline-variant/20 focus-within:border-primary">
    <!-- Input file ẩn -->
    <input ref="fileInputRef" type="file" accept="image/*" class="hidden" @change="handleFileUpload" :disabled="isUploadingImage" />

    <!-- Thanh Toolbar chuyên nghiệp của TipTap -->
    <div v-if="editor" class="tiptap-toolbar bg-surface-container-lowest border-b border-outline-variant/10 p-2 flex flex-wrap gap-1 items-center select-none">
      <!-- In đậm -->
      <button type="button" @click="editor.chain().focus().toggleBold().run()"
        :class="editor.isActive('bold') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="In đậm (Ctrl+B)" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm font-bold">format_bold</span>
      </button>

      <!-- In nghiêng -->
      <button type="button" @click="editor.chain().focus().toggleItalic().run()"
        :class="editor.isActive('italic') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="In nghiêng (Ctrl+I)" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">format_italic</span>
      </button>

      <!-- Gạch chân -->
      <button type="button" @click="editor.chain().focus().toggleUnderline().run()"
        :class="editor.isActive('underline') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Gạch chân (Ctrl+U)" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">format_underlined</span>
      </button>

      <!-- Gạch ngang chữ -->
      <button type="button" @click="editor.chain().focus().toggleStrike().run()"
        :class="editor.isActive('strike') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Gạch ngang chữ" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">strikethrough_s</span>
      </button>

      <div class="w-px h-5 bg-outline-variant/20 mx-1"></div>

      <!-- Tiêu đề H2 -->
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        :class="editor.isActive('heading', { level: 2 }) ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Tiêu đề lớn (H2)" class="px-2 h-8 rounded-sm border flex items-center justify-center font-black text-xs transition-all">
        H2
      </button>

      <!-- Tiêu đề H3 -->
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
        :class="editor.isActive('heading', { level: 3 }) ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Tiêu đề phụ (H3)" class="px-2 h-8 rounded-sm border flex items-center justify-center font-black text-xs transition-all">
        H3
      </button>

      <!-- Đoạn văn bình thường -->
      <button type="button" @click="editor.chain().focus().setParagraph().run()"
        :class="editor.isActive('paragraph') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Đoạn văn (P)" class="px-2 h-8 rounded-sm border flex items-center justify-center font-bold text-xs transition-all">
        P
      </button>

      <div class="w-px h-5 bg-outline-variant/20 mx-1"></div>

      <!-- Danh sách chấm tròn -->
      <button type="button" @click="editor.chain().focus().toggleBulletList().run()"
        :class="editor.isActive('bulletList') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Danh sách gạch đầu dòng" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">format_list_bulleted</span>
      </button>

      <!-- Danh sách số thứ tự -->
      <button type="button" @click="editor.chain().focus().toggleOrderedList().run()"
        :class="editor.isActive('orderedList') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Danh sách đánh số" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">format_list_numbered</span>
      </button>

      <!-- Khối trích dẫn -->
      <button type="button" @click="editor.chain().focus().toggleBlockquote().run()"
        :class="editor.isActive('blockquote') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Khối trích dẫn" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">format_quote</span>
      </button>

      <!-- Đường kẻ ngang phân cách -->
      <button type="button" @click="editor.chain().focus().setHorizontalRule().run()"
        title="Đường kẻ ngang phân cách" class="w-8 h-8 rounded-sm text-on-surface-variant hover:text-white hover:bg-white/10 flex items-center justify-center transition-colors">
        <span class="material-symbols-outlined text-sm">horizontal_rule</span>
      </button>

      <div class="w-px h-5 bg-outline-variant/20 mx-1"></div>

      <!-- Tải ảnh trực tiếp từ máy tính lên Cloudinary -->
      <button type="button" @click="triggerImageUpload" :disabled="isUploadingImage"
        title="Tải ảnh từ máy tính lên và chèn vào bài viết" class="px-2.5 h-8 rounded-sm hover:bg-white/10 flex items-center gap-1.5 text-on-surface-variant hover:text-primary transition-colors text-xs font-bold bg-white/5 border border-white/10">
        <span v-if="isUploadingImage" class="material-symbols-outlined text-sm animate-spin text-primary">progress_activity</span>
        <span v-else class="material-symbols-outlined text-sm">add_photo_alternate</span>
        <span class="text-[10px] uppercase font-bold">{{ isUploadingImage ? 'Đang tải...' : 'Tải ảnh lên' }}</span>
      </button>

      <!-- Chèn ảnh từ URL -->
      <button type="button" @click="openImageDialog" title="Chèn ảnh từ link URL" class="w-8 h-8 rounded-sm hover:bg-white/10 flex items-center justify-center text-on-surface-variant hover:text-primary transition-colors">
        <span class="material-symbols-outlined text-sm">image</span>
      </button>

      <!-- Chèn liên kết Link -->
      <button type="button" @click="openLinkDialog"
        :class="editor.isActive('link') ? 'bg-primary/20 text-primary border-primary/40' : 'text-on-surface-variant hover:text-white hover:bg-white/10 border-transparent'"
        title="Chèn liên kết (Link)" class="w-8 h-8 rounded-sm border flex items-center justify-center transition-all">
        <span class="material-symbols-outlined text-sm">link</span>
      </button>

      <div class="w-px h-5 bg-outline-variant/20 mx-1"></div>

      <!-- Undo / Redo -->
      <button type="button" @click="editor.chain().focus().undo().run()" :disabled="!editor.can().undo()" title="Hoàn tác (Ctrl+Z)" class="w-8 h-8 rounded-sm hover:bg-white/10 flex items-center justify-center text-on-surface-variant hover:text-white transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-sm">undo</span>
      </button>
      <button type="button" @click="editor.chain().focus().redo().run()" :disabled="!editor.can().redo()" title="Làm lại (Ctrl+Y)" class="w-8 h-8 rounded-sm hover:bg-white/10 flex items-center justify-center text-on-surface-variant hover:text-white transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-sm">redo</span>
      </button>

      <!-- Xóa định dạng -->
      <button type="button" @click="editor.chain().focus().unsetAllMarks().clearNodes().run()" title="Xóa toàn bộ định dạng đang chọn" class="w-8 h-8 rounded-sm hover:bg-white/10 flex items-center justify-center text-on-surface-variant hover:text-red-400 transition-colors ml-auto">
        <span class="material-symbols-outlined text-sm">format_clear</span>
      </button>
    </div>

    <!-- Khung Editor TipTap Content -->
    <EditorContent :editor="editor" class="tiptap-content-area flex-1 w-full bg-surface-container-highest p-4 text-sm text-on-surface outline-none overflow-y-auto scrollbar-custom min-h-[280px] max-h-[460px] font-sans leading-relaxed select-text" />

    <!-- Modal Chèn Link -->
    <div v-if="isLinkModalOpen" class="fixed inset-0 z-[1200] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isLinkModalOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low border border-outline-variant/20 rounded-2xl shadow-2xl p-6 space-y-4 animate-in zoom-in-95 duration-200">
        <div class="flex justify-between items-center pb-3 border-b border-outline-variant/10">
          <h4 class="font-headline font-black uppercase italic text-primary text-base flex items-center gap-2">
            <span class="material-symbols-outlined text-lg">link</span> Chèn liên kết
          </h4>
          <button @click="isLinkModalOpen = false" class="text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="space-y-3">
          <div>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5 block">Văn bản hiển thị</label>
            <input v-model="linkForm.text" type="text" placeholder="VD: Bấm vào đây để xem chi tiết" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
          </div>
          <div>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5 block">Đường dẫn liên kết (URL)</label>
            <input v-model="linkForm.url" type="url" placeholder="https://..." class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-lg text-sm text-on-surface focus:border-primary outline-none font-mono" />
          </div>
        </div>
        <div class="flex gap-3 pt-2">
          <button @click="isLinkModalOpen = false" class="flex-1 py-2.5 rounded-lg border border-outline-variant/20 text-xs font-bold uppercase tracking-wider hover:bg-white/5 transition-colors">Hủy</button>
          <button @click="submitInsertLink" class="flex-1 py-2.5 rounded-lg bg-primary text-on-primary text-xs font-bold uppercase tracking-wider hover:brightness-110 transition-all">Lưu liên kết</button>
        </div>
      </div>
    </div>

    <!-- Modal Chèn Ảnh bằng URL -->
    <div v-if="isImageModalOpen" class="fixed inset-0 z-[1200] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isImageModalOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low border border-outline-variant/20 rounded-2xl shadow-2xl p-6 space-y-4 animate-in zoom-in-95 duration-200">
        <div class="flex justify-between items-center pb-3 border-b border-outline-variant/10">
          <h4 class="font-headline font-black uppercase italic text-primary text-base flex items-center gap-2">
            <span class="material-symbols-outlined text-lg">image</span> Chèn ảnh từ URL
          </h4>
          <button @click="isImageModalOpen = false" class="text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="space-y-3">
          <div>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5 block">Mô tả ảnh (Alt text)</label>
            <input v-model="imageForm.alt" type="text" placeholder="VD: Banner khuyến mãi hè" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
          </div>
          <div>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5 block">Đường dẫn ảnh (URL)</label>
            <input v-model="imageForm.url" type="url" placeholder="https://.../anh.jpg" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-lg text-sm text-on-surface focus:border-primary outline-none font-mono" />
          </div>
        </div>
        <div class="flex gap-3 pt-2">
          <button @click="isImageModalOpen = false" class="flex-1 py-2.5 rounded-lg border border-outline-variant/20 text-xs font-bold uppercase tracking-wider hover:bg-white/5 transition-colors">Hủy</button>
          <button @click="submitInsertImageUrl" class="flex-1 py-2.5 rounded-lg bg-primary text-on-primary text-xs font-bold uppercase tracking-wider hover:brightness-110 transition-all">Chèn Ảnh</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* CSS Styling cho vùng soạn thảo TipTap */
.tiptap-content-area .tiptap {
  outline: none;
  min-height: 240px;
}

.tiptap-content-area .tiptap p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  float: left;
  color: rgba(148, 163, 184, 0.4);
  pointer-events: none;
  height: 0;
}

.tiptap-content-area .tiptap h2 {
  font-size: 1.25rem;
  font-weight: 800;
  text-transform: uppercase;
  color: #fff;
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tiptap-content-area .tiptap h3 {
  font-size: 1.05rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #f5c518;
  margin-top: 1rem;
  margin-bottom: 0.35rem;
}

.tiptap-content-area .tiptap p {
  margin-bottom: 0.6rem;
  line-height: 1.65;
}

.tiptap-content-area .tiptap ul {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin: 0.6rem 0;
}

.tiptap-content-area .tiptap ol {
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin: 0.6rem 0;
}

.tiptap-content-area .tiptap li {
  margin-bottom: 0.25rem;
}

.tiptap-content-area .tiptap blockquote {
  border-left: 4px solid #f5c518;
  padding: 0.5rem 0.85rem;
  margin: 0.75rem 0;
  background: rgba(245, 197, 24, 0.06);
  font-style: italic;
  color: #cbd5e1;
  border-radius: 0 0.5rem 0.5rem 0;
}

.tiptap-content-area .tiptap img {
  border-radius: 0.75rem;
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1rem auto;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tiptap-content-area .tiptap hr {
  margin: 1.25rem 0;
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.tiptap-content-area .tiptap a {
  color: #f5c518;
  text-decoration: underline;
  font-weight: 600;
  cursor: pointer;
}
</style>
