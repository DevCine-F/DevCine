import axios from 'axios';

async function test() {
  console.log('Bắt đầu gọi API tới Backend...');
  const start = Date.now();
  try {
    const res = await axios.get('http://localhost:8080/api/movies', {
      maxContentLength: Infinity,
      maxBodyLength: Infinity
    });
    const time = Date.now() - start;
    const dataString = JSON.stringify(res.data);
    const sizeMB = (Buffer.byteLength(dataString, 'utf8') / (1024 * 1024)).toFixed(2);
    
    console.log(`\n================================`);
    console.log(`✅ Gọi API thành công!`);
    console.log(`⏱ Thời gian Backend phản hồi: ${time} ms`);
    console.log(`📦 Kích thước cục dữ liệu: ${sizeMB} MB`);
    if (res.data.length > 0) {
       console.log(`🖼 Độ dài của chuỗi Base64 ảnh phim đầu tiên: ${res.data[0].posterUrl?.length || 0} ký tự!`);
    }
    console.log(`================================\n`);
  } catch (e) {
    console.error('Lỗi khi gọi API:', e.message);
  }
}

test();
