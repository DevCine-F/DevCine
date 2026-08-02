const { Client } = require('pg');

const client = new Client({
  connectionString: 'postgresql://postgres.uetuvfdemxlptcfniwti:DevCine@123@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres'
});

async function run() {
  await client.connect();
  console.log('Connected to DB');

  const stRes = await client.query('SELECT id, name FROM seat_types');
  const seatTypes = {};
  for (const r of stRes.rows) {
      seatTypes[r.name.toUpperCase()] = r.id;
  }
  
  for (const type of ['NORMAL', 'VIP', 'SWEETBOX']) {
      if (!seatTypes[type]) {
          const res = await client.query('INSERT INTO seat_types (name) VALUES ($1) RETURNING id', [type]);
          seatTypes[type] = res.rows[0].id;
      }
  }

  const cRes = await client.query('SELECT id FROM cinemas');
  for (const c of cRes.rows) {
      const cinemaId = c.id;
      const rRes = await client.query('SELECT id FROM rooms WHERE cinema_id = $1 ORDER BY id ASC', [cinemaId]);
      let rooms = rRes.rows;
      
      while (rooms.length < 3) {
          const res = await client.query(`INSERT INTO rooms (cinema_id, name, type, status, turnaround_time_mins) VALUES ($1, 'Temp', 'STANDARD', 'Active', 15) RETURNING id`, [cinemaId]);
          rooms.push({ id: res.rows[0].id });
      }
      
      const r1 = rooms[0].id;
      const r2 = rooms[1].id;
      const r3 = rooms[2].id;
      
      await client.query(`UPDATE rooms SET name = 'Phòng 01 - Standard', type = 'STANDARD', status = 'Active', matrix_row = 10, matrix_col = 16 WHERE id = $1`, [r1]);
      await client.query(`UPDATE rooms SET name = 'Phòng 02 - Superplex', type = 'SUPERPLEX', status = 'Active', matrix_row = 10, matrix_col = 16 WHERE id = $1`, [r2]);
      await client.query(`UPDATE rooms SET name = 'Phòng 03 - Cine Comfort', type = 'CINE_COMFORT', status = 'Active', matrix_row = 8, matrix_col = 10 WHERE id = $1`, [r3]);
      
      for (let i = 3; i < rooms.length; i++) {
          await client.query(`UPDATE rooms SET status = 'Maintenance' WHERE id = $1`, [rooms[i].id]);
      }
      
      async function reseedSeats(roomId, rows, cols, normalRows, vipRows) {
          const sRes = await client.query('SELECT id, row_char, col_num FROM seats WHERE room_id = $1', [roomId]);
          const existing = {};
          for (const s of sRes.rows) existing[`${s.row_char}-${s.col_num}`] = s;
          
          for (let r = 1; r <= rows; r++) {
              let type = seatTypes['SWEETBOX'];
              if (r <= normalRows) type = seatTypes['NORMAL'];
              else if (r <= vipRows) type = seatTypes['VIP'];
              
              const rowChar = String.fromCharCode(64 + r);
              for (let c = 1; c <= cols; c++) {
                  const label = rowChar + c.toString().padStart(2, '0');
                  const ex = existing[`${rowChar}-${c}`];
                  if (ex) {
                      await client.query('UPDATE seats SET seat_type_id = $1, is_active = true, label = $2 WHERE id = $3', [type, label, ex.id]);
                      delete existing[`${rowChar}-${c}`];
                  } else {
                      await client.query('INSERT INTO seats (room_id, row_char, col_num, seat_type_id, is_active, label) VALUES ($1, $2, $3, $4, true, $5)', [roomId, rowChar, c, type, label]);
                  }
              }
          }
          
          for (const key in existing) {
              await client.query('UPDATE seats SET is_active = false WHERE id = $1', [existing[key].id]);
          }
      }
      
      await reseedSeats(r1, 10, 16, 4, 9);
      await reseedSeats(r2, 10, 16, 3, 9);
      await reseedSeats(r3, 8, 10, 2, 8);
  }
  
  console.log('Migration successful');
  await client.end();
}

run().catch(console.error);
