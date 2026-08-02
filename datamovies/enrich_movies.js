const fs = require('fs');
const xlsx = require('xlsx');

const filePath = 'E:\\DATN\\DevCine\\datamovies\\movies_data.xlsx';

if (!fs.existsSync(filePath)) {
    console.log("Không tìm thấy file Excel!");
    process.exit(1);
}

// Read the excel file
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const ws = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(ws);

const movie_enrichment = {
    22: {"name": "Tên Cậu Là Gì? (Your Name)", "duration": 107, "director": "Makoto Shinkai", "actors": "Ryunosuke Kamiki, Mone Kamishiraishi", "country": "Nhật Bản", "release_year": 2016, "language": "Phụ đề Tiếng Việt", "age_rating": "T13", "trailer_url": "https://www.youtube.com/watch?v=s0wTdCQoc2k", "description": "Mitsuha, một nữ sinh trung học ở nông thôn, và Taki, một nam sinh trung học ở Tokyo, bất ngờ bị hoán đổi thân xác cho nhau qua những giấc mơ. Cả hai cùng vượt qua không gian và thời gian để đi tìm sự thật và giải cứu thị trấn khỏi thảm họa thiên thạch."},
    23: {"name": "Mưu Đồ Ẩn Giấu (A Haunting in Venice)", "duration": 103, "director": "Kenneth Branagh", "actors": "Kenneth Branagh, Michelle Yeoh, Tina Fey", "country": "Mỹ", "release_year": 2023, "language": "Phụ đề Tiếng Việt", "age_rating": "T16", "trailer_url": "https://www.youtube.com/watch?v=y38RzCylCyg", "description": "Thám tử Hercule Poirot đã nghỉ hưu và sống ẩn dật tại Venice. Sau khi tham dự một buổi gọi hồn bí ẩn vào đêm Halloween tại một dinh thự cổ, một vị khách bất ngờ bị sát hại, buộc Poirot phải tái xuất để giải mã những hiện tượng siêu nhiên.", "genres": "Trinh thám, Kinh dị"},
    24: {"name": "Lật Mặt 7: Một Điều Ước", "duration": 138, "director": "Lý Hải", "actors": "Thanh Hiền, Trương Minh Cường, Đinh Y Nhung, Quách Ngọc Tuyên", "country": "Việt Nam", "release_year": 2024, "language": "Tiếng Việt", "age_rating": "K", "trailer_url": "https://www.youtube.com/watch?v=d_k-W44n4-s", "description": "Câu chuyện xoay quanh bà Hai 73 tuổi cùng 5 người con đã trưởng thành nhưng mỗi người một phương. Khi biến cố sức khỏe xảy ra, chuyến hành trình nuôi dưỡng và trách nhiệm của các con hé lộ những góc khuất sâu sắc về tình cảm gia đình.", "genres": "Gia đình, Tình cảm"},
    25: {"name": "Doraemon: Bản Giao Hưởng Địa Cầu", "duration": 115, "director": "Kazuaki Imai", "actors": "Wasabi Mizuta, Megumi Oohara, Yumi Kakazu", "country": "Nhật Bản", "release_year": 2024, "language": "Lồng tiếng Tiếng Việt", "age_rating": "P", "trailer_url": "https://www.youtube.com/watch?v=3gVw290T-F8", "description": "Nobita và nhóm bạn cùng nhau tham gia hành trình âm nhạc kỳ thú để giải cứu thế giới khỏi một sinh vật bí ẩn đe dọa xóa sổ âm thanh khỏi vũ trụ."},
    26: {"name": "Inside Out 2 (Những Mảnh Mảnh Cảm Xúc 2)", "duration": 96, "director": "Kelsey Mann", "actors": "Amy Poehler, Maya Hawke, Kensington Tallman", "country": "Mỹ", "release_year": 2024, "language": "Lồng tiếng Tiếng Việt", "age_rating": "P", "trailer_url": "https://www.youtube.com/watch?v=LEjhY15eCx0", "description": "Riley bước vào tuổi dậy thì với những cảm xúc mới xuất hiện tại trung tâm điều khiển, dẫn đầu là Lo Âu (Anxiety), khiến nhóm cảm xúc cũ rơi vào những thử thách bất ngờ.", "genres": "Hoạt hình, Gia đình"},
    27: {"name": "Grave of the Fireflies (Mộ Đom Đóm)", "duration": 89, "director": "Isao Takahata", "actors": "Tsutomu Tatsumi, Ayano Shiraishi", "country": "Nhật Bản", "release_year": 1988, "language": "Phụ đề Tiếng Việt", "age_rating": "T13", "trailer_url": "https://www.youtube.com/watch?v=4vPeTSRd580", "description": "Câu chuyện tình anh em đầy xúc động giữa hai đứa trẻ Seita và Setsuko trong bối cảnh Thế chiến thứ hai tại Nhật Bản, kiên cường sinh tồn giữa bom đạn bi thương."},
    28: {"name": "Mai", "duration": 131, "director": "Trấn Thành", "actors": "Phương Anh Đào, Tuấn Trần, Hồng Đào, Trấn Thành", "country": "Việt Nam", "release_year": 2024, "language": "Tiếng Việt", "age_rating": "T18", "trailer_url": "https://www.youtube.com/watch?v=e_e9rR3yqB8", "description": "Chuyện đời của Mai — một người phụ nữ làm nghề mát-xa với quá khứ nhiều biến cố, khi cô gặp Dương, một chàng trai trẻ kém tuổi tràn đầy nhiệt huyết quyết tâm chinh phục trái tim cô.", "genres": "Tình cảm, Tâm lý"},
    29: {"name": "Lật Mặt 7: Một Điều Ước (Bản Mở Rộng)", "duration": 132, "director": "Lý Hải", "actors": "Thanh Hiền, Trương Minh Cường, Đinh Y Nhung", "country": "Việt Nam", "release_year": 2024, "language": "Tiếng Việt", "age_rating": "T16", "trailer_url": "https://www.youtube.com/watch?v=d_k-W44n4-s", "genres": "Gia đình, Tình cảm"},
    30: {"name": "Đào, Phở Và Piano", "duration": 100, "director": "Phi Tiến Sơn", "actors": "Doãn Quốc Đam, Cao Thùy Linh, Trần Lực, Trung Hiếu", "country": "Việt Nam", "release_year": 2024, "language": "Tiếng Việt", "age_rating": "T13", "trailer_url": "https://www.youtube.com/watch?v=p1h2Vw7Z9wQ", "genres": "Lịch sử, Chiến tranh"},
    31: {"name": "Dune: Hành Tinh Cát - Phần Hai", "duration": 166, "director": "Denis Villeneuve", "actors": "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem", "country": "Mỹ", "release_year": 2024, "language": "Phụ đề Tiếng Việt", "age_rating": "T13", "trailer_url": "https://www.youtube.com/watch?v=Way9Dexny3w", "genres": "Viễn tưởng, Hành động"},
    32: {"name": "Kung Fu Panda 4", "duration": 94, "director": "Mike Mitchell", "actors": "Jack Black, Awkwafina, Viola Davis, Dustin Hoffman", "country": "Mỹ", "release_year": 2024, "language": "Lồng tiếng Tiếng Việt", "age_rating": "P", "trailer_url": "https://www.youtube.com/watch?v=_inKs4eeHiI", "genres": "Hoạt hình, Hài hước"},
    33: {"name": "Godzilla x Kong: Đế Chế Mới", "duration": 115, "director": "Adam Wingard", "actors": "Rebecca Hall, Brian Tyree Henry, Dan Stevens", "country": "Mỹ", "release_year": 2024, "language": "Phụ đề Tiếng Việt", "age_rating": "T13", "trailer_url": "https://www.youtube.com/watch?v=qqrpMRDuTEo"},
    36: {"name": "Cô Dâu 8 Tuổi (Bản Điện Ảnh)", "duration": 120, "director": "Sidharam Sharma", "actors": "Avika Gor, Avinash Mukherjee", "country": "Ấn Độ", "release_year": 2024, "language": "Lồng tiếng Tiếng Việt", "age_rating": "P", "trailer_url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "description": "Tác phẩm khắc họa câu chuyện đời sống xã hội và những biến cố bất ngờ của Anandi trong hành trình trưởng thành.", "genres": "Tâm lý, Gia đình"},
    37: {"name": "Biệt Đội Rất Ổn", "duration": 105, "director": "Tạ Nguyên Đức", "actors": "Lê Khánh, Hứa Vĩ Văn, Hoàng Oanh, Quang Tuấn", "country": "Việt Nam", "release_year": 2024, "language": "Tiếng Việt", "age_rating": "P", "trailer_url": "https://www.youtube.com/watch?v=abc12345678", "description": "Một phi vụ tráo chiếc vòng gia bảo đầy hài hước và kịch tính tại đám cưới xa hoa, quy tụ dàn diễn viên hài đình đám."},
    38: {"name": "Người Đẹp Và Quái Vật (Beauty and the Beast)", "duration": 129, "director": "Bill Condon", "actors": "Emma Watson, Dan Stevens, Luke Evans", "country": "Mỹ", "release_year": 2017, "language": "Lồng tiếng Tiếng Việt", "age_rating": "P", "trailer_url": "https://www.youtube.com/watch?v=e3Nl_TC4OI8", "description": "Belle - một cô gái trẻ thông minh và xinh đẹp - bị bắt làm tù nhân trong lâu đài của một con quái vật đáng sợ, nhưng qua thời gian cô nhận ra vẻ đẹp tâm hồn ẩn sâu bên trong hắn."}
};

const missing_country_map = {
    41: "Nhật Bản", 42: "Mỹ", 43: "Mỹ", 44: "Việt Nam", 45: "Nhật Bản",
    46: "Mỹ", 47: "Hàn Quốc", 48: "Việt Nam", 49: "Mỹ", 50: "Thái Lan",
    51: "Mỹ", 52: "Việt Nam", 54: "Việt Nam", 55: "Việt Nam", 56: "Việt Nam",
    57: "Việt Nam", 58: "Việt Nam", 59: "Việt Nam", 60: "Việt Nam", 61: "Malaysia",
    62: "Việt Nam", 63: "Mỹ", 64: "Mỹ"
};

const archived_ids = [58, 57, 61, 46];
const upcoming_ids = [42, 49, 51, 63, 66, 23, 25, 26, 41, 43, 50, 54];

function isNa(val) {
    return val === undefined || val === null || (typeof val === 'number' && isNaN(val)) || String(val).toLowerCase() === 'nan';
}

for (let i = 0; i < data.length; i++) {
    const row = data[i];
    const movie_id = row['id'];
    
    if (movie_enrichment[movie_id]) {
        for (const [k, v] of Object.entries(movie_enrichment[movie_id])) {
            row[k] = v;
        }
    }
    
    if ((isNa(row['country']) || String(row['country']).trim() === '') && missing_country_map[movie_id]) {
        row['country'] = missing_country_map[movie_id];
    }
    
    if (isNa(row['language']) || String(row['language']).trim() === '') {
        const c = String(row['country']);
        if (c === "Việt Nam") row['language'] = "Tiếng Việt";
        else if (c === "Mỹ" || c === "Anh") row['language'] = "Phụ đề Tiếng Việt";
        else if (c === "Nhật Bản") row['language'] = "Lồng tiếng / Phụ đề Tiếng Việt";
        else row['language'] = "Phụ đề Tiếng Việt";
    }

    const t_url = String(row['trailer_url']);
    if (isNa(row['trailer_url']) || !t_url.startsWith("http")) {
        row['trailer_url'] = `https://www.youtube.com/watch?v=devcine_${movie_id}`;
    }

    if (isNa(row['release_year']) || row['release_year'] > 2026 || row['release_year'] < 1900) {
        row['release_year'] = 2024;
    }
    if (isNa(row['duration']) || row['duration'] > 300 || row['duration'] < 40) {
        row['duration'] = 110;
    }

    if (isNa(row['poster_url']) || String(row['poster_url']).trim() === '') {
        row['poster_url'] = "https://image.tmdb.org/t/p/w500/yK9IflD2697xU6rD441S0bXk2S2.jpg";
    }
    if (isNa(row['banner_url']) || String(row['banner_url']).trim() === '') {
        row['banner_url'] = row['poster_url'];
    }

    if (isNa(row['director']) || ['trống', 'nqh', 'đá'].includes(String(row['director']).trim().toLowerCase())) {
        row['director'] = "Đang cập nhật";
    }
    if (isNa(row['actors']) || ['nqh', 'sad'].includes(String(row['actors']).trim().toLowerCase())) {
        row['actors'] = "Đang cập nhật";
    }
    if (isNa(row['genres']) || String(row['genres']).trim() === '') {
        row['genres'] = "Hành động, Tâm lý";
    }
    
    const desc = String(row['description']).toLowerCase();
    if (isNa(row['description']) || String(row['description']).trim().length < 10 || desc.includes('qwe') || desc.includes('123dssss')) {
        row['description'] = `Bộ phim ${row['name']} hứa hẹn mang đến cho khán giả những trải nghiệm điện ảnh tuyệt vời và đầy cảm xúc tại hệ thống rạp DevCine.`;
    }

    if (archived_ids.includes(movie_id)) {
        row['status'] = 'archived';
        row['release_date'] = '2026-07-01';
        row['end_date'] = '2026-07-30';
    } else if (upcoming_ids.includes(movie_id)) {
        row['status'] = 'upcoming';
        row['release_date'] = '2026-08-15';
        row['end_date'] = '2026-09-15';
    } else {
        row['status'] = 'active';
        row['release_date'] = '2026-06-01';
        row['end_date'] = '2026-08-31';
    }

    row['duration'] = parseInt(row['duration'], 10);
    row['release_year'] = parseInt(row['release_year'], 10);
}

const newWs = xlsx.utils.json_to_sheet(data);
const newWb = xlsx.utils.book_new();
xlsx.utils.book_append_sheet(newWb, newWs, 'Movies');
xlsx.writeFile(newWb, filePath);

console.log("ĐÃ CẬP NHẬT THÀNH CÔNG FILE EXCEL TẠI:", filePath);
