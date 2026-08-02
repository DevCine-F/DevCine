const http = require('http');

const request = (options, postData = null) => {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          resolve(data);
        }
      });
    });
    req.on('error', reject);
    if (postData) {
      req.write(postData);
    }
    req.end();
  });
};

(async () => {
  try {
    const loginData = JSON.stringify({ username: 'admin', password: '123' });
    const loginRes = await request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/auth/login',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(loginData)
      }
    }, loginData);

    const token = loginRes.data ? loginRes.data.token : loginRes.token;
    if (!token) {
      console.log('Login failed: ' + JSON.stringify(loginRes));
      return;
    }

    const authHeaders = { 'Authorization': 'Bearer ' + token };
    const cinemasRes = await request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/v1/cinemas',
      method: 'GET',
      headers: authHeaders
    });

    const cinemas = cinemasRes.data || cinemasRes;
    
    let md = '';
    const gaps = [];
    
    for (const c of cinemas) {
      md += '### ' + c.name + ' (ID: ' + c.id + ')\n';
      const roomsRes = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/api/rooms/cinema/' + c.id,
        method: 'GET',
        headers: authHeaders
      });
      const rooms = roomsRes.data || roomsRes || [];
      
      md += '| ID | Tên Phòng | Lo?i Phòng (Type) | Kích thu?c (Hàng x C?t) | T?ng Gh? | Tr?ng Thái |\n';
      md += '|---|---|---|---|---|---|\n';
      
      if (!Array.isArray(rooms) || rooms.length === 0) {
        md += '| - | (Chua có phòng) | - | - | - | - |\n';
      } else {
        for (const r of rooms) {
           const typeStr = r.type || 'N/A';
           const totalSeats = (r.matrixRow || 0) * (r.matrixCol || 0);
           const status = r.status || 'N/A';
           
           let isGap = false;
           let gapReasons = [];
           if (!['STANDARD', 'SUPERPLEX', 'CINE_COMFORT'].includes(typeStr)) {
               isGap = true;
               gapReasons.push('Lo?i phòng sai chu?n (' + typeStr + ')');
           }
           
           let statusStr = status;
           if (status !== 'ACTIVE' && status !== 'Active') {
               statusStr = '**' + status + '** ??';
               isGap = true;
               gapReasons.push('Tr?ng thái không ho?t d?ng (' + status + ')');
           }
           if (totalSeats === 0) {
               isGap = true;
               gapReasons.push('Kích thu?c/Gh? chua c?u hình');
           }
           
           if (isGap) {
               gaps.push('- **Phòng ' + r.name + ' (ID: ' + r.id + ') - ' + c.name + '**: ' + gapReasons.join(', '));
           }
           
           const typeDisplay = isGap && !['STANDARD', 'SUPERPLEX', 'CINE_COMFORT'].includes(typeStr) ? '**' + typeStr + '** ??' : typeStr;
           
           md += '| ' + r.id + ' | ' + r.name + ' | ' + typeDisplay + ' | ' + (r.matrixRow || 0) + ' x ' + (r.matrixCol || 0) + ' | ' + totalSeats + ' | ' + statusStr + ' |\n';
        }
      }
      md += '\n';
    }
    
    md += '\n## 2. ÐÁNH GIÁ Ð? L?CH CHU?N (Gap Analysis)\n';
    if (gaps.length === 0) {
        md += 'Không phát hi?n l?ch chu?n. T?t c? các phòng d?u h?p l?.\n';
    } else {
        md += gaps.join('\n') + '\n';
    }
    
    console.log(md);
  } catch (e) {
    console.error(e);
  }
})();
