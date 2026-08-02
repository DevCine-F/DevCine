const { Client } = require('pg');
const xlsx = require('xlsx');

const DB_URL = 'postgresql://postgres.uetuvfdemxlptcfniwti:DevCine%40123@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres';

async function exportMovies() {
    const client = new Client({ connectionString: DB_URL });
    
    try {
        await client.connect();
        
        // Query to get movies and their genres
        const query = `
            SELECT 
                m.id,
                m.title AS name,
                m.duration_mins,
                m.director,
                m.cast_members,
                m.country,
                m.production_year,
                m.language,
                m.original_language,
                m.version_type,
                m.age_rating,
                m.status,
                TO_CHAR(m.start_date, 'YYYY-MM-DD') AS start_date,
                TO_CHAR(m.release_date, 'YYYY-MM-DD') AS release_date,
                TO_CHAR(m.end_date, 'YYYY-MM-DD') AS end_date,
                m.trailer_url,
                m.poster_base64 AS poster_url,
                m.banner_base64 AS banner_url,
                m.description,
                COALESCE(
                    (SELECT string_agg(c.name, ', ')
                     FROM movie_genre_mapping mgm
                     JOIN categories c ON mgm.category_id = c.id
                     WHERE mgm.movie_id = m.id), 
                    '') AS genres
            FROM movies m
            ORDER BY m.id ASC
        `;
        
        const res = await client.query(query);
        const data = res.rows;
        
        // Create a new workbook and add the data
        const wb = xlsx.utils.book_new();
        const ws = xlsx.utils.json_to_sheet(data);
        
        // Add the worksheet to the workbook
        xlsx.utils.book_append_sheet(wb, ws, 'Movies');
        
        // Write to file
        const outputFile = 'E:\\DATN\\DevCine\\datamovies\\movies_data.xlsx';
        xlsx.writeFile(wb, outputFile);
        
        console.log(`Successfully exported ${data.length} movies to ${outputFile}`);
    } catch (err) {
        console.error('Error exporting movies:', err);
    } finally {
        await client.end();
    }
}

exportMovies();
