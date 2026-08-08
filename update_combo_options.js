const { Client } = require('pg');
const fs = require('fs');
const dbUrl = 'postgres://postgres.uetuvfdemxlptcfniwti:DevCine@123@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres';

const sql = `
DO $$ 
DECLARE
    g_vap_lon INT;
    g_mix INT;
    g_nuoc INT;
    f_id INT;
BEGIN
    SELECT id INTO g_vap_lon FROM fnb_option_groups WHERE name = 'Chọn Vị Bắp Lớn' LIMIT 1;
    SELECT id INTO g_mix FROM fnb_option_groups WHERE name = 'Chế Độ Mix Vị (Bắp Lớn)' LIMIT 1;
    SELECT id INTO g_nuoc FROM fnb_option_groups WHERE name = 'Chọn Loại Nước Lớn' LIMIT 1;

    IF g_vap_lon IS NOT NULL THEN
        FOR f_id IN SELECT id FROM fnb_items WHERE name IN ('DEVCINE COMBO', 'SOLO COMBO', 'COUPLE COMBO') LOOP
            DELETE FROM fnb_item_option_groups WHERE fnb_item_id = f_id;
            INSERT INTO fnb_item_option_groups (fnb_item_id, option_group_id) VALUES (f_id, g_vap_lon);
            INSERT INTO fnb_item_option_groups (fnb_item_id, option_group_id) VALUES (f_id, g_mix);
            INSERT INTO fnb_item_option_groups (fnb_item_id, option_group_id) VALUES (f_id, g_nuoc);
        END LOOP;
    END IF;
END $$;
`;

async function runTest() {
  const client = new Client({ connectionString: dbUrl });
  try {
    await client.connect();
    console.log('Connected to DB');
    await client.query(sql);
    console.log('Combo Option Groups successfully updated!');
  } catch (err) {
    console.error(err);
  } finally {
    await client.end();
  }
}

runTest();
