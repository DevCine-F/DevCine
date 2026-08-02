const fs = require('fs');
const xlsx = require('xlsx');

const filePath = 'E:\\DATN\\DevCine\\datamovies\\movies_data.xlsx';

if (!fs.existsSync(filePath)) {
    console.log("Không tìm thấy file Excel!");
    process.exit(1);
}

const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const ws = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(ws);

const fix_posters = {
    28: "https://res.cloudinary.com/dnjtjbwnl/image/upload/v1781527761/lb886aeyjhps8grwqbpa.webp",
    29: "https://res.cloudinary.com/dnjtjbwnl/image/upload/v1781527761/lb886aeyjhps8grwqbpa.webp",
    30: "https://res.cloudinary.com/dnjtjbwnl/image/upload/v1781103765/qvlqxzexzsmdhuyrjr50.jpg",
    31: "https://res.cloudinary.com/dnjtjbwnl/image/upload/v1781527878/keta7nqw0ipc26id9sct.png",
    32: "https://res.cloudinary.com/dnjtjbwnl/image/upload/v1781527823/rmoezj6ehhdlnegaiya2.webp",
    33: "https://res.cloudinary.com/dnjtjbwnl/image/upload/v1781527865/bd2xfuzmjbymolsppl1u.jpg"
};

function isNa(val) {
    return val === undefined || val === null || (typeof val === 'number' && isNaN(val)) || String(val).toLowerCase() === 'nan';
}

for (let i = 0; i < data.length; i++) {
    const row = data[i];
    const m_id = row['id'];
    
    if (fix_posters[m_id]) {
        row['poster_url'] = fix_posters[m_id];
        row['banner_url'] = fix_posters[m_id];
    }
    
    if (isNa(row['start_date']) || String(row['start_date']).trim() === '') {
        row['start_date'] = row['release_date'];
    }
    
    if (isNa(row['original_language']) || String(row['original_language']).trim() === '') {
        row['original_language'] = row['language'];
    }
    
    if (isNa(row['version_type']) || String(row['version_type']).trim() === '') {
        const lang = String(row['language']);
        if (lang.includes('Lồng tiếng')) {
            row['version_type'] = '2D Lồng tiếng';
        } else {
            row['version_type'] = '2D Phụ đề';
        }
    }
}

const newWs = xlsx.utils.json_to_sheet(data);
const newWb = xlsx.utils.book_new();
xlsx.utils.book_append_sheet(newWb, newWs, 'Movies');
xlsx.writeFile(newWb, filePath);

console.log("ĐÃ FIX XONG DỮ LIỆU FILE EXCEL!");
