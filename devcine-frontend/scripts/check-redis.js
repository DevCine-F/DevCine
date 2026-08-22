import net from 'net';

const REDIS_PORT = 6382;
const REDIS_HOST = '127.0.0.1';

const checkRedis = () => {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    socket.setTimeout(1200);

    socket.on('connect', () => {
      socket.destroy();
      resolve(true);
    });

    socket.on('error', () => {
      socket.destroy();
      resolve(false);
    });

    socket.on('timeout', () => {
      socket.destroy();
      resolve(false);
    });

    socket.connect(REDIS_PORT, REDIS_HOST);
  });
};

const isRunning = await checkRedis();

if (!isRunning) {
  console.log('\n\x1b[33m%s\x1b[0m', '================================================================');
  console.log('\x1b[31m%s\x1b[0m', ' ❌ [CẢNH BÁO]: REDIS CHƯA ĐƯỢC BẬT TRÊN CỔNG ' + REDIS_PORT + '!');
  console.log('\x1b[36m%s\x1b[0m', ' 👉 Vui lòng MỞ DOCKER DESKTOP hoặc khởi động Container Redis.');
  console.log('\x1b[36m%s\x1b[0m', ' 👉 Sau khi Docker/Redis sẵn sàng, hãy chạy lại: npm run dev:all');
  console.log('\x1b[33m%s\x1b[0m', '================================================================\n');
  process.exit(1);
}

console.log('\x1b[32m%s\x1b[0m', '✅ [Redis]: Đã kết nối thành công tới cổng ' + REDIS_PORT + '. Đang khởi động Backend & Frontend...\n');
