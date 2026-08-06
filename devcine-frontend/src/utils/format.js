/**
 * Định dạng tiền tệ VND
 * @param {number} amount 
 */
export const formatVND = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
};

/**
 * Định dạng ngày tháng DD/MM/YYYY
 * @param {string|Date} date 
 */
export const formatDate = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return d.toLocaleDateString('vi-VN');
};

/**
 * Định dạng ngày tháng DD.MM.YYYY (Phong cách điện ảnh)
 * @param {string|Date} date 
 */
export const formatDateDot = (date) => {
  if (!date) return '';
  const d = new Date(date);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}.${month}.${year}`;
};

/**
 * Định dạng thời gian HH:mm
 * @param {string|Date} date 
 */
export const formatTime = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

/**
 * Tách Tên và Mô tả của Combo Bắp Nước
 * VD: "Combo Party (2 Bắp + 4 Nước)" -> { title: "Combo Party", desc: "(2 Bắp + 4 Nước)" }
 * @param {string} name 
 */
export const formatComboTitle = (name) => {
  if (!name) return { title: '', desc: '' };
  const idx = name.indexOf('(');
  if (idx !== -1) {
    return {
      title: name.substring(0, idx).trim(),
      desc: name.substring(idx).trim()
    };
  }
  return { title: name, desc: '' };
};

