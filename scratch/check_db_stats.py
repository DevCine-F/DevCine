import sys
import psycopg2

sys.stdout.reconfigure(encoding='utf-8')

conn = psycopg2.connect(
    host='aws-1-ap-southeast-1.pooler.supabase.com',
    port=5432,
    dbname='postgres',
    user='postgres.uetuvfdemxlptcfniwti',
    password='DevCine@123'
)
cur = conn.cursor()
cur.execute("""
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    ORDER BY table_name;
""")
tables = [r[0] for r in cur.fetchall()]

total_rows = 0
stats = []
for t in tables:
    cur.execute(f'SELECT COUNT(*) FROM "{t}";')
    cnt = cur.fetchone()[0]
    total_rows += cnt
    stats.append((t, cnt))

print(f"Total tables: {len(tables)}, Total rows: {total_rows}")
for t, cnt in stats:
    if cnt > 0:
        print(f"  - {t}: {cnt} rows")

cur.close()
conn.close()
