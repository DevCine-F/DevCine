const { Client } = require('pg');
const fs = require('fs');
const dbUrl = 'postgres://postgres.uetuvfdemxlptcfniwti:DevCine@123@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres';

const sql = `
CREATE TABLE IF NOT EXISTS fnb_option_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    min_choices INT NOT NULL DEFAULT 0,
    max_choices INT NOT NULL DEFAULT 1,
    is_required BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS fnb_option_items (
    id SERIAL PRIMARY KEY,
    group_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    surcharge_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_group FOREIGN KEY (group_id) REFERENCES fnb_option_groups(id) ON DELETE CASCADE,
    UNIQUE (group_id, name)
);

CREATE TABLE IF NOT EXISTS fnb_item_option_groups (
    fnb_item_id INT NOT NULL,
    option_group_id INT NOT NULL,
    PRIMARY KEY (fnb_item_id, option_group_id)
);

DO $$ 
DECLARE
    bap_lon_id INT;
    bap_vua_id INT;
    nuoc_lon_id INT;
    combo_solo_id INT;
    combo_couple_id INT;
    g_vap_lon INT;
    g_mix INT;
    g_nuoc INT;
BEGIN
    -- Delete from referencing tables first
    DELETE FROM booking_fnbs WHERE fnb_item_id IN (SELECT id FROM fnb_items WHERE name IN ('Bắp Rang Lớn', 'Bắp Rang Vừa', 'Nước Ngọt Lớn', 'Combo Solo', 'Combo Couple'));
    DELETE FROM concession_sale_items WHERE fnb_item_id IN (SELECT id FROM fnb_items WHERE name IN ('Bắp Rang Lớn', 'Bắp Rang Vừa', 'Nước Ngọt Lớn', 'Combo Solo', 'Combo Couple'));

    DELETE FROM fnb_items WHERE name IN ('Bắp Rang Lớn', 'Bắp Rang Vừa', 'Nước Ngọt Lớn', 'Combo Solo', 'Combo Couple');
    
    INSERT INTO fnb_items (name, type, price, is_active) VALUES ('Bắp Rang Lớn', 'BẮP RANG', 55000, true) RETURNING id INTO bap_lon_id;
    INSERT INTO fnb_items (name, type, price, is_active) VALUES ('Bắp Rang Vừa', 'BẮP RANG', 45000, true) RETURNING id INTO bap_vua_id;
    INSERT INTO fnb_items (name, type, price, is_active) VALUES ('Nước Ngọt Lớn', 'NƯỚC UỐNG', 35000, true) RETURNING id INTO nuoc_lon_id;
    INSERT INTO fnb_items (name, type, price, is_active) VALUES ('Combo Solo', 'COMBO', 85000, true) RETURNING id INTO combo_solo_id;
    INSERT INTO fnb_items (name, type, price, is_active) VALUES ('Combo Couple', 'COMBO', 119000, true) RETURNING id INTO combo_couple_id;

    DELETE FROM fnb_option_groups WHERE name IN ('Chọn Vị Bắp Lớn', 'Chế Độ Mix Vị (Bắp Lớn)', 'Chọn Loại Nước Lớn');

    INSERT INTO fnb_option_groups (name, min_choices, max_choices, is_required) VALUES ('Chọn Vị Bắp Lớn', 1, 1, true) RETURNING id INTO g_vap_lon;
    INSERT INTO fnb_option_groups (name, min_choices, max_choices, is_required) VALUES ('Chế Độ Mix Vị (Bắp Lớn)', 0, 1, false) RETURNING id INTO g_mix;
    INSERT INTO fnb_option_groups (name, min_choices, max_choices, is_required) VALUES ('Chọn Loại Nước Lớn', 1, 1, true) RETURNING id INTO g_nuoc;

    INSERT INTO fnb_option_items (group_id, name, surcharge_price) VALUES 
    (g_vap_lon, 'Bắp Nguyên Bản', 0),
    (g_vap_lon, 'Vị Phô Mai', 10000),
    (g_vap_lon, 'Vị Caramel', 10000),
    (g_vap_lon, 'Vị Bơ Sữa', 10000),
    (g_vap_lon, 'Vị Mala CAY', 10000),
    (g_vap_lon, 'Vị Cà Phê Sữa', 10000);

    INSERT INTO fnb_option_items (group_id, name, surcharge_price) VALUES 
    (g_mix, 'Mix 2 Vị Tự Chọn', 20000);

    INSERT INTO fnb_option_items (group_id, name, surcharge_price) VALUES 
    (g_nuoc, 'Pepsi', 0),
    (g_nuoc, '7Up', 0),
    (g_nuoc, 'Mirinda Cam', 0),
    (g_nuoc, 'Lipton Trà Chanh', 0),
    (g_nuoc, 'Trà Vải Nestea', 10000),
    (g_nuoc, 'Soda ADE Xoài', 10000);

    DELETE FROM fnb_item_option_groups WHERE fnb_item_id IN (bap_lon_id, nuoc_lon_id, combo_solo_id, combo_couple_id);

    INSERT INTO fnb_item_option_groups (fnb_item_id, option_group_id) VALUES 
    (bap_lon_id, g_vap_lon),
    (bap_lon_id, g_mix),
    (nuoc_lon_id, g_nuoc),
    (combo_solo_id, g_vap_lon),
    (combo_solo_id, g_mix),
    (combo_solo_id, g_nuoc),
    (combo_couple_id, g_vap_lon),
    (combo_couple_id, g_mix),
    (combo_couple_id, g_nuoc);

END $$;
`;

async function runTest() {
  const client = new Client({ connectionString: dbUrl });
  try {
    await client.connect();
    console.log('Connected to DB');
    
    fs.writeFileSync('V2__seed_fnb_data.sql', sql);
    console.log('SQL file written to V2__seed_fnb_data.sql');

    await client.query(sql);
    console.log('Seed data successfully executed!');
  } catch (err) {
    console.error(err);
  } finally {
    await client.end();
  }
}

runTest();
