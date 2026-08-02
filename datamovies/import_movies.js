const fs = require('fs');
const xlsx = require('xlsx');
const { Client } = require('pg');

const filePath = 'E:\\DATN\\DevCine\\datamovies\\movies_data.xlsx';
const DB_URL = 'postgresql://postgres.uetuvfdemxlptcfniwti:DevCine%40123@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres';

async function importMovies() {
    if (!fs.existsSync(filePath)) {
        console.log("Không tìm thấy file Excel!");
        process.exit(1);
    }

    const wb = xlsx.readFile(filePath);
    const ws = wb.Sheets[wb.SheetNames[0]];
    const data = xlsx.utils.sheet_to_json(ws);

    const client = new Client({ connectionString: DB_URL });
    await client.connect();

    console.log(`Tiến hành import ${data.length} phim...`);
    let successCount = 0;

    try {
        await client.query('BEGIN'); // Start transaction

        for (const row of data) {
            const id = row.id;
            if (!id) continue;

            const title = row.name;
            const duration_mins = row.duration;
            const director = row.director;
            const cast_members = row.actors;
            const country = row.country;
            const production_year = row.release_year;
            const language = row.language;
            const age_rating = row.age_rating;
            const status = row.status;
            const release_date = row.release_date || null;
            const end_date = row.end_date || null;
            const start_date = row.start_date || row.release_date || null;
            const original_language = row.original_language || null;
            const version_type = row.version_type || null;
            const trailer_url = row.trailer_url;
            const poster_url = row.poster_url;
            const banner_url = row.banner_url;
            const description = row.description;
            
            // 1. Update Movie
            const updateMovieSql = `
                UPDATE movies 
                SET title = $1, duration_mins = $2, director = $3, cast_members = $4, country = $5, 
                    production_year = $6, language = $7, age_rating = $8, status = $9, release_date = $10, 
                    end_date = $11, trailer_url = $12, poster_base64 = $13, banner_base64 = $14, description = $15,
                    start_date = $17, original_language = $18, version_type = $19
                WHERE id = $16
            `;
            const values = [
                title, duration_mins, director, cast_members, country, 
                production_year, language, age_rating, status, release_date, 
                end_date, trailer_url, poster_url, banner_url, description, 
                id, start_date, original_language, version_type
            ];
            await client.query(updateMovieSql, values);

            // 2. Handle Genres
            const genresStr = row.genres;
            if (genresStr && typeof genresStr === 'string') {
                const genreNames = genresStr.split(',').map(s => s.trim()).filter(s => s);
                for (const gName of genreNames) {
                    // Check if category exists
                    let catRes = await client.query('SELECT id FROM categories WHERE lower(name) = lower($1)', [gName]);
                    let categoryId;
                    
                    if (catRes.rows.length === 0) {
                        // Insert new category
                        const insertCat = await client.query(
                            'INSERT INTO categories (name, description) VALUES ($1, $2) RETURNING id', 
                            [gName, gName]
                        );
                        categoryId = insertCat.rows[0].id;
                    } else {
                        categoryId = catRes.rows[0].id;
                    }

                    // Map category to movie safely
                    const mapCheck = await client.query(
                        'SELECT 1 FROM movie_genre_mapping WHERE movie_id = $1 AND category_id = $2',
                        [id, categoryId]
                    );
                    if (mapCheck.rows.length === 0) {
                        await client.query(
                            'INSERT INTO movie_genre_mapping (movie_id, category_id) VALUES ($1, $2)',
                            [id, categoryId]
                        );
                    }
                }
            }
            
            successCount++;
        }

        await client.query('COMMIT');
        console.log(`Đã UPDATE thành công ${successCount} phim vào CSDL!`);
        
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Lỗi trong quá trình import, đã rollback toàn bộ giao dịch:', err);
    } finally {
        await client.end();
    }
}

importMovies();
