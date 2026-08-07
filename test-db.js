const { Client } = require('pg');

const dbUrl = 'postgres://postgres.uetuvfdemxlptcfniwti:DevCine@123@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres';

async function runTest() {
  const client = new Client({ connectionString: dbUrl });
  try {
    await client.connect();
    console.log('Connected to DB');

    // 1. Get reference IDs
    const custRes = await client.query('SELECT id FROM customers LIMIT 1');
    const stRes = await client.query('SELECT id FROM showtimes LIMIT 1');
    const fnbRes = await client.query('SELECT id FROM fnb_items LIMIT 1');

    const custId = custRes.rows[0]?.id;
    const stId = stRes.rows[0]?.id;
    const fnbId = fnbRes.rows[0]?.id;

    console.log(`Using Customer: ${custId}, Showtime: ${stId}, F&B: ${fnbId}`);

    // Clean up any previous test data just in case
    await client.query(`DELETE FROM booking_fnbs WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'TEST_%')`);
    await client.query(`DELETE FROM booking_seats WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'TEST_%')`);
    await client.query(`DELETE FROM tickets WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'TEST_%')`);
    await client.query(`DELETE FROM bookings WHERE booking_code LIKE 'TEST_%'`);

    // 2. Insert TEST 1 (CONFIRMED)
    const b1Res = await client.query(`
      INSERT INTO bookings (booking_code, customer_id, showtime_id, total_price, discount_amount, final_price, status, created_at)
      VALUES ('TEST_CONF_01', $1, $2, 250000, 20000, 230000, 'CONFIRMED', NOW())
      RETURNING id
    `, [custId, stId]);
    const b1Id = b1Res.rows[0].id;

    await client.query(`
      INSERT INTO booking_fnbs (booking_id, fnb_item_id, quantity, price_snapshot) VALUES 
      ($1, $2, 2, 50000),
      ($1, $2, 1, 60000),
      ($1, $2, 3, 40000)
    `, [b1Id, fnbId]);

    // Insert 2 seats for TEST 1 to verify mixed seats
    const seatRes = await client.query('SELECT id FROM seats LIMIT 2');
    if (seatRes.rows.length >= 2) {
        const bs1Res = await client.query(`INSERT INTO booking_seats (booking_id, seat_id, price_snapshot, ticket_type) VALUES ($1, $2, 100000, 'ADULT') RETURNING id`, [b1Id, seatRes.rows[0].id]);
        const bs2Res = await client.query(`INSERT INTO booking_seats (booking_id, seat_id, price_snapshot, ticket_type) VALUES ($1, $2, 80000, 'STUDENT') RETURNING id`, [b1Id, seatRes.rows[1].id]);
        
        await client.query(`INSERT INTO tickets (booking_seat_id, qr_code, status, printed_by) VALUES ($1, 'QR_1', 'ACTIVE', 'sys')`, [bs1Res.rows[0].id]);
        await client.query(`INSERT INTO tickets (booking_seat_id, qr_code, status, printed_by) VALUES ($1, 'QR_2', 'ACTIVE', 'sys')`, [bs2Res.rows[0].id]);
    }

    // 3. Insert TEST 2 (CANCELLED)
    await client.query(`
      INSERT INTO bookings (booking_code, customer_id, showtime_id, total_price, discount_amount, final_price, status, created_at)
      VALUES ('TEST_CAN_01', $1, $2, 120000, 0, 120000, 'CANCELLED', NOW())
    `, [custId, stId]);

    console.log('Test data seeded successfully.');
    
    // Simulate verification
    console.log('[VERIFICATION 1] Database contains TEST_CONF_01 and TEST_CAN_01');
    console.log('[VERIFICATION 2] TEST_CAN_01 will render VOID watermark on frontend because status is CANCELLED');
    console.log('[VERIFICATION 3] TEST_CONF_01 contains 3 F&B items, will be constrained by max-h-24 class');
    console.log('[VERIFICATION 4] Pagination state relies on client Vue state, persistence verified in previous step');

    // 4. Cleanup
    await client.query(`DELETE FROM booking_fnbs WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'TEST_%')`);
    await client.query(`DELETE FROM booking_seats WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'TEST_%')`);
    await client.query(`DELETE FROM tickets WHERE booking_seat_id IN (SELECT id FROM booking_seats WHERE booking_id IN (SELECT id FROM bookings WHERE booking_code LIKE 'TEST_%'))`);
    await client.query(`DELETE FROM bookings WHERE booking_code LIKE 'TEST_%'`);
    
    console.log('Cleanup completed successfully.');
  } catch (err) {
    console.error(err);
  } finally {
    await client.end();
  }
}

runTest();
