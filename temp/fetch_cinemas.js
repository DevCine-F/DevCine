
const http = require('http');

const fetchJson = (url) => {
  return new Promise((resolve, reject) => {
    http.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          resolve(null);
        }
      });
    }).on('error', reject);
  });
};

(async () => {
  try {
    const cinemasRes = await fetchJson('http://localhost:8080/api/cinemas');
    const cinemas = cinemasRes.data || cinemasRes;
    
    const result = [];
    
    for (const cinema of cinemas) {
      const roomsRes = await fetchJson('http://localhost:8080/api/rooms/cinema/' + cinema.id);
      const rooms = roomsRes.data || roomsRes || [];
      result.push({
        cinema: cinema,
        rooms: rooms
      });
    }
    
    console.log(JSON.stringify(result, null, 2));
  } catch (e) {
    console.error(e);
  }
})();

