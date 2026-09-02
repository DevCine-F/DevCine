import os
import sys
from datetime import datetime
import psycopg2
from psycopg2.extensions import adapt

# Ensure UTF-8 output
sys.stdout.reconfigure(encoding='utf-8')

# 1. Parse .env
env_file = os.path.join(os.path.dirname(__file__), '..', 'devcine-backend', '.env')
env_vars = {}
if os.path.exists(env_file):
    with open(env_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                k, v = line.split('=', 1)
                env_vars[k.strip()] = v.strip()

host = 'aws-1-ap-southeast-1.pooler.supabase.com'
port = 5432
dbname = 'postgres'
user = env_vars.get('DB_USERNAME', 'postgres.uetuvfdemxlptcfniwti')
password = env_vars.get('DB_PASSWORD', 'DevCine@123')

backup_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'backup'))
os.makedirs(backup_dir, exist_ok=True)

now_str = datetime.now().strftime('%Y%m%d_%H%M%S')
timestamp_readable = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
backup_filename = f'devcine_backup_{now_str}.sql'
latest_filename = 'devcine_backup_latest.sql'
backup_filepath = os.path.join(backup_dir, backup_filename)
latest_filepath = os.path.join(backup_dir, latest_filename)

print(f"Connecting to database {host}:{port}/{dbname} as {user}...")
conn = psycopg2.connect(
    host=host,
    port=port,
    dbname=dbname,
    user=user,
    password=password
)
cur = conn.cursor()

def format_val(v, conn):
    if v is None:
        return 'NULL'
    if isinstance(v, bool):
        return 'TRUE' if v else 'FALSE'
    a = adapt(v)
    if hasattr(a, 'prepare'):
        a.prepare(conn)
    q = a.getquoted()
    return q.decode('utf-8') if isinstance(q, bytes) else str(q)

# Get all tables
cur.execute("""
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    ORDER BY table_name;
""")
tables = [r[0] for r in cur.fetchall()]

# Get primary keys
cur.execute("""
    SELECT
        tc.table_name, 
        kcu.column_name
    FROM 
        information_schema.table_constraints AS tc 
        JOIN information_schema.key_column_usage AS kcu
          ON tc.constraint_name = kcu.constraint_name
          AND tc.table_schema = kcu.table_schema
    WHERE tc.constraint_type = 'PRIMARY KEY' 
      AND tc.table_schema = 'public'
    ORDER BY tc.table_name, kcu.ordinal_position;
""")
pks_map = {}
for t, c in cur.fetchall():
    pks_map.setdefault(t, []).append(c)

# Get sequences mapping
cur.execute("""
    SELECT 
        d.refobjid::regclass::text AS table_name,
        a.attname AS column_name,
        c.relname AS sequence_name
    FROM pg_depend d
    JOIN pg_attribute a ON a.attrelid = d.refobjid AND a.attnum = d.refobjsubid
    JOIN pg_class c ON c.oid = d.objid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE d.deptype IN ('a', 'i')
      AND c.relkind = 'S'
      AND n.nspname = 'public';
""")
seq_mappings = cur.fetchall()

print(f"Found {len(tables)} tables. Generating full SQL dump...")

table_stats = []
total_rows_dumped = 0

with open(backup_filepath, 'w', encoding='utf-8') as f:
    # Write Header
    f.write("-- ====================================================================\n")
    f.write(f"-- DEVCINE CINEMA MANAGEMENT SYSTEM - DATABASE BACKUP\n")
    f.write(f"-- Timestamp: {timestamp_readable}\n")
    f.write(f"-- Server Host: {host}\n")
    f.write(f"-- Database: {dbname}\n")
    f.write(f"-- Total Tables: {len(tables)}\n")
    f.write("-- ====================================================================\n\n")

    f.write("SET statement_timeout = 0;\n")
    f.write("SET lock_timeout = 0;\n")
    f.write("SET idle_in_transaction_session_timeout = 0;\n")
    f.write("SET client_encoding = 'UTF8';\n")
    f.write("SET standard_conforming_strings = on;\n")
    f.write("SET check_function_bodies = false;\n")
    f.write("SET client_min_messages = warning;\n")
    f.write("SET row_security = off;\n\n")

    f.write("BEGIN;\n\n")
    f.write("-- Disable triggers and foreign key constraint enforcement during data restoration\n")
    f.write("SET session_replication_role = 'replica';\n\n")

    for table in tables:
        f.write(f"-- --------------------------------------------------------------------\n")
        f.write(f"-- Table structure for: public.\"{table}\"\n")
        f.write(f"-- --------------------------------------------------------------------\n")

        # Get columns
        cur.execute("""
            SELECT 
                column_name, 
                data_type, 
                character_maximum_length,
                numeric_precision,
                numeric_scale,
                is_nullable,
                column_default,
                is_identity,
                identity_generation
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = %s
            ORDER BY ordinal_position;
        """, (table,))
        cols_info = cur.fetchall()

        col_defs = []
        col_names = []
        for col in cols_info:
            c_name, d_type, char_len, num_prec, num_scale, is_null, c_def, is_id, id_gen = col
            col_names.append(c_name)

            type_str = d_type.upper()
            if d_type == 'character varying':
                type_str = f'VARCHAR({char_len})' if char_len else 'VARCHAR'
            elif d_type == 'character':
                type_str = f'CHAR({char_len})' if char_len else 'CHAR'
            elif d_type == 'numeric':
                type_str = f'NUMERIC({num_prec},{num_scale})' if num_prec else 'NUMERIC'
            elif d_type == 'timestamp without time zone':
                type_str = 'TIMESTAMP'
            elif d_type == 'timestamp with time zone':
                type_str = 'TIMESTAMPTZ'
            elif d_type == 'time without time zone':
                type_str = 'TIME'
            elif d_type == 'double precision':
                type_str = 'DOUBLE PRECISION'

            c_str = f'    "{c_name}" {type_str}'
            if is_id == 'YES':
                c_str += f' GENERATED {id_gen} AS IDENTITY'
            elif c_def:
                c_str += f' DEFAULT {c_def}'

            if is_null == 'NO':
                c_str += ' NOT NULL'
            col_defs.append(c_str)

        pks = pks_map.get(table, [])
        if pks:
            pk_cols = ', '.join(f'"{p}"' for p in pks)
            col_defs.append(f'    CONSTRAINT "{table}_pkey" PRIMARY KEY ({pk_cols})')

        f.write(f'CREATE TABLE IF NOT EXISTS public."{table}" (\n')
        f.write(',\n'.join(col_defs))
        f.write('\n);\n\n')

        # Dump Data
        cols_quoted = ', '.join(f'"{c}"' for c in col_names)
        cur.execute(f'SELECT {cols_quoted} FROM public."{table}";')
        rows = cur.fetchall()
        row_count = len(rows)
        table_stats.append((table, row_count))
        total_rows_dumped += row_count

        if row_count > 0:
            f.write(f'-- Dumping data for table: public."{table}" ({row_count} rows)\n')
            # Batch inserts: 50 rows per statement
            batch_size = 50
            for i in range(0, row_count, batch_size):
                batch = rows[i:i + batch_size]
                values_list = []
                for row in batch:
                    formatted_vals = [format_val(v, conn) for v in row]
                    values_list.append(f"({', '.join(formatted_vals)})")
                
                f.write(f'INSERT INTO public."{table}" ({cols_quoted}) VALUES\n')
                f.write(',\n'.join(values_list))
                f.write(';\n')
            f.write('\n')
        else:
            f.write(f'-- Table public."{table}" is empty.\n\n')

    # Reset sequences
    f.write("-- --------------------------------------------------------------------\n")
    f.write("-- Synchronize auto-increment sequences\n")
    f.write("-- --------------------------------------------------------------------\n")
    for tbl_name, col_name, seq_name in seq_mappings:
        # Strip public prefix if present
        clean_tbl = tbl_name.replace('public.', '')
        f.write(f"SELECT setval('public.\"{seq_name}\"', COALESCE((SELECT MAX(\"{col_name}\") FROM public.\"{clean_tbl}\"), 1), true);\n")
    f.write("\n")

    # Restore session settings & commit
    f.write("-- --------------------------------------------------------------------\n")
    f.write("-- Re-enable foreign key constraints and triggers\n")
    f.write("-- --------------------------------------------------------------------\n")
    f.write("SET session_replication_role = 'default';\n\n")
    f.write("COMMIT;\n")
    f.write("-- ====================================================================\n")
    f.write(f"-- BACKUP COMPLETED: {total_rows_dumped} total rows exported across {len(tables)} tables\n")
    f.write("-- ====================================================================\n")

cur.close()
conn.close()

# Also make a copy as devcine_backup_latest.sql
import shutil
shutil.copyfile(backup_filepath, latest_filepath)

file_size_mb = os.path.getsize(backup_filepath) / (1024 * 1024)
print(f"\n========================================================")
print(f"BACKUP HOÀN TẤT THÀNH CÔNG!")
print(f"File sao lưu chính: {backup_filepath}")
print(f"File mới nhất:      {latest_filepath}")
print(f"Kích thước file:    {file_size_mb:.2f} MB")
print(f"Tổng số bảng:       {len(tables)}")
print(f"Tổng số bản ghi:    {total_rows_dumped}")
print(f"========================================================")
